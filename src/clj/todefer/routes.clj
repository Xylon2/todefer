(ns todefer.routes
  (:require [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.session :refer [wrap-session]]
            ;; [ring.middleware.session.memory :as memory]
            ;; [ring.redis.session :refer [redis-store read-redis-session write-redis-session]]
            [clojure.spec.alpha :as s]
            [todefer.handlers :as hl]
            [todefer.task-controllers :as tc]
            [todefer.habit-controllers :as hc]
            [todefer.todo-controllers :as ac]
            [todefer.settings-controllers :as sc]
            [ring.logger :as logger]
            [clojure.string :as string]
            [clojure.pprint]
            [clojure.java.io]))

(s/def ::int int?)
(s/def ::string string?)
(s/def ::non-empty-string (s/and string? #(> (count %) 0)))
(s/def ::iso-date #(re-matches #"\d{4}-\d{2}-\d{2}" %))
(s/def ::freq_unit #{"days" "weeks" "months" "years"})
(s/def ::int-colon-int
  (s/and string? #(re-matches #"\d+:\d+" %)))

(s/def ::ints-list (s/coll-of ::int                    :kind vector?))
(s/def ::strs-list (s/coll-of ::string                 :kind vector?))
(s/def ::date-list (s/coll-of ::iso-date               :kind vector?))
(s/def ::freq_unit-list (s/coll-of ::freq_unit         :kind vector?))
(s/def ::int-colon-int-list (s/coll-of ::int-colon-int :kind vector?))

(s/def ::donewhen #{"today" "yesturday"})
(s/def ::pagetype #{"task" "habit" "todo"})
(s/def ::todo-actions #{"today" "tomorrow" "not"})
(s/def ::add-thing-actions #{"due" "defer" "today" "tomorrow"})
(s/def ::order-values #{"top" "bottom"})

;; examples
;; - habit/34
;; - task/23
(s/def ::thing-id
  (s/and string?
         #(re-matches #"^(habit|task|kludge)/\d+$" %)))
(s/def ::thing-list
  (s/coll-of ::thing-id :kind vector?))

(defn wrap-debug-reqmap
  "debug middleware to save the requestmap to a file so we can analyze"
  [handler comment]
  (fn [req]
    (when-not (string/includes? (:uri req) "favicon.ico")
      (let [timestamp (.toString (java.time.LocalDateTime/now))
            filename (str "reqlog/request-" comment "-" timestamp ".edn")]
        (clojure.pprint/pprint req (clojure.java.io/writer filename))))

    ; Call the handler and return its response
    (handler req)))

(defn wrap-auth [handler]
  (fn [{:keys [uri query-string session] :as request}]
    (if (contains? session :user)
      ;; user is logged in. proceed
      (handler request)

      ;; user not logged in. redirect
      (let [redirect-url (java.net.URLEncoder/encode
                          (str uri "?" query-string)
                          "UTF-8")]
        {:status 303
         :headers {"Location" (str "/login?redirect=" redirect-url)}
         :body ""}))))

(defn conform-to-vector [x]
  (if (vector? x)
    x
    [x]))

(defn keys-with-list-values
  "this function helps parsing the ring request middleware"
  [kv-map]
  (for [[k v] kv-map
        :when (clojure.string/includes? (name v) "-list")]
    (name k)))

(defn wrap-vector
  "for some reason the coercion does not update these to vectors. so we do it
  ourselfes"
  [handler mapkey]
  (fn [{{{{{[coercion-info] :form} :parameters} :post} :data} :reitit.core/match
        :as request}]
    (let [form-params (mapkey request)
          transform-keys (keys-with-list-values coercion-info)
          transformed-params (reduce (fn [params key]
                                       (if (contains? params key)
                                         (update params key conform-to-vector)
                                         params))
                                     form-params
                                     transform-keys)
          updated-request (assoc request mapkey transformed-params)]
      (handler updated-request))))

(defn app
  "reitit with format negotiation and input & output coercion"
  [q-builder session-store]
  ;; we define a middleware that includes our query builder
  (let [wrap-query-builder (fn [handler]
                             (fn [request]
                               (handler (assoc request :q-builder q-builder))))]

    (ring/ring-handler
     (ring/router
      [["/" {:middleware [wrap-auth]
             :handler hl/home-handler}]

       ["/page/:page-name"
        {:middleware [wrap-auth]
         :get {:handler hl/display-page
               :parameters {:path {:page-name ::string}}}}]

       ["/page/:page-name/"
        {:middleware [wrap-auth]
         :post {:parameters {:path {:page-name ::string}}}}

        ;; single-request actions. these run a db query and re-display the
        ;; normal page contents
        ["done-habit"
         {:post {:handler hc/done-habit-handler
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :donewhen ::donewhen}}}}]

        ["delete-task"
         {:post {:handler tc/delete-task-handler
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list}}}}]
        ["delete-habit"
         {:post {:handler hc/delete-habit-handler
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list}}}}]

        ["move-task"
         {:post {:handler tc/move-task-handler
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :newpage ::string}}}}]
        ["move-habit"
         {:post {:handler hc/move-habit-handler
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :newpage ::string}}}}]

        ["todo-task"
         {:post {:handler tc/todo-task-handler
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :action ::todo-actions}}}}]

        ["todo-habit"
         {:post {:handler hc/todo-habit-handler
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :action ::todo-actions}}}}]

        ["order-task"
         {:post {:handler tc/order-task-handler
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :order ::order-values}}}}]

        ["order-habit"
         {:post {:handler hc/order-habit-handler
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :order ::order-values}}}}]

        ;; multi-request actions. these actions have a view page, where the user
        ;; is asked for more input, and a save page, which will run a query and
        ;; re-display the normal page contents
        ["modify-task-view"
         {:post {:handler tc/modify-task-view
                 :parameters {:form {:task_id ::ints-list}}}}]
        ["modify-task-save"
         {:post {:handler tc/modify-task-save
                 ;; :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :task_newname ::strs-list}}}}]

        ["modify-habit-view"
         {:post {:handler hc/modify-habit-view
                 :parameters {:form {:habit_id ::ints-list}}}}]
        ["modify-habit-save"
         {:post {:handler hc/modify-habit-save
                 ;; :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :habit_name_new ::strs-list
                                     :freq_value_new ::ints-list
                                     :freq_unit_new ::freq_unit-list
                                     :due_new ::date-list
                                     }}}}]

        ["modify-thing-view"
         {:post {:handler ac/modify-thing-view
                 :parameters {:form {:thing_id ::thing-list}}}}]

        ;; the defer task page has four possible actions
        ["defer-task-view"
         {:post {:handler tc/defer-task-view
                 :parameters {:form {:task_id ::ints-list}}}}]
        ["defer-task-date-save"
         {:post {:handler tc/defer-task-date-save
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :date ::iso-date}}}}]
        ["defer-task-category-save"
         {:post {:handler tc/defer-task-category-save
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :cat_id ::int}}}}]
        ["defer-task-newcategory-save"
         {:post {:handler tc/defer-task-newcategory-save
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :new-catname ::non-empty-string}}}}]
        ["defer-task-not"
         {:post {:handler tc/defer-task-not
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list}}}}]

        ;; defer habit only has one submit option
        ["defer-habit-view"
         {:post {:handler hc/defer-habit-view
                 :parameters {:form {:habit_id ::ints-list}}}}]
        ["defer-habit-date-save"
         {:post {:handler hc/defer-habit-date-save
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :date ::iso-date}}}}]

        ;; special cases where it may be single or multi-request
        ["add-task"
         {:post {:handler tc/add-task-handler
                 :parameters {:form {:task_name ::non-empty-string
                                     :xaction ::add-thing-actions}}}}]
        ["add-habit"
         {:post {:handler hc/add-habit-handler
                 :parameters {:form {:habit_name ::non-empty-string
                                     :freq_value ::int
                                     :freq_unit ::freq_unit
                                     :xaction ::add-thing-actions}}}}]

        ;; actions for the todo page
        ["done-delete"
         {:post {:handler ac/done-delete-handler
                 :middleware [ac/wrap-show-todo]
                 :parameters {:form {:thing_id ::thing-list}}}}]

        ["todo-thing"
         {:post {:handler ac/todo-thing-handler
                 :middleware [ac/wrap-show-todo]
                 :parameters {:form {:thing_id ::thing-list
                                     :action ::todo-actions}}}}]

        ["todo-add-task"
         {:post {:handler ac/add-task-handler
                 :middleware [ac/wrap-show-todo]
                 :parameters {:form {:task_name ::non-empty-string
                                     :tpage ::int
                                     :aaction ::add-thing-actions}}}}]

        ["order-todo"
         {:post {:handler ac/order-thing-handler
                 :middleware [ac/wrap-show-todo]
                 :parameters {:form {:thing_id ::thing-list
                                     :order ::order-values}}}}]
        ]

       ["/settings"
        {:middleware [wrap-auth]
         :get {:handler sc/settings-handler}}]

       ["/settings/"
        {:middleware [wrap-auth]}
        ["add-page"
         {:post {:handler sc/add-page-handler
                 :middleware [sc/wrap-settings-page]
                 :parameters {:form {:new_pagename ::non-empty-string
                                     :new_pagetype ::pagetype}}}}]
        ["delete"
         {:post {:handler sc/delete-page-handler
                 :middleware [sc/wrap-settings-page]
                 :parameters {:form {:page_id ::int}}}}]

        ["page_down"
         {:post {:handler sc/page-down-handler
                 :middleware [sc/wrap-settings-page]
                 :parameters {:form {:page_id ::int}}}}]

        ["page_up"
         {:post {:handler sc/page-up-handler
                 :middleware [sc/wrap-settings-page]
                 :parameters {:form {:page_id ::int}}}}]

        ["update_pages"
         {:post {:handler sc/update-handler
                 :middleware [sc/wrap-settings-page]
                 :parameters {:form {:rpid ::ints-list
                                     :new_page_name ::strs-list
                                     :linkedpage ::int-colon-int-list}}}}]]

       ["/login" {:get {:handler hl/login-handler}
                  :post {:handler hl/login-post-handler
                         :parameters {:form {:username ::string
                                             :password ::string}}}}]
       ["/logout" {:get {:handler hl/logout-handler}}]

       ["/public/*path" {:get {:middleware [wrap-content-type
                                            [wrap-resource ""]]
                               :handler hl/not-found-handler}}]]

      ;; router data affecting all routes
      {:data {:coercion   reitit.coercion.spec/coercion
              :muuntaja   m/instance
              :middleware [parameters/parameters-middleware
                           [wrap-vector :params]
                           [wrap-vector :form-params]
                           rrc/coerce-request-middleware
                           muuntaja/format-response-middleware
                           rrc/coerce-response-middleware
                           logger/wrap-with-logger
                           wrap-query-builder
                           [wrap-session {:store session-store
                                          :cookie-attrs {:http-only true
                                                         :max-age 604800}}]
                           wrap-anti-forgery
                           ;; [wrap-debug-reqmap "complete"]
                           ]}})

     ;; default handler
     (wrap-query-builder
      (wrap-session
       hl/not-found-handler {:store session-store
                             :cookie-attrs {:http-only true
                                            :max-age 604800}}))

     ;; options

     )))
