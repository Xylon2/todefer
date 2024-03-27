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
            [todefer.agenda-controllers :as ac]
            [todefer.settings-controllers :as sc]
            [ring.logger :as logger]
            [clojure.string :as string]
            [clojure.pprint]
            [clojure.java.io]))

(s/def ::int int?)
(s/def ::string string?)

(s/def ::iso-date #(re-matches #"\d{4}-\d{2}-\d{2}" %))
(s/def ::freq_unit #{"days" "weeks" "months" "years"})
(s/def ::donewhen #{"today" "yesturday"})
(s/def ::pagetype #{"task" "habit" "agenda"})
(s/def ::todo-actions #{"today" "tomorrow" "not"})

(s/def ::ints-list (s/coll-of ::int      :kind vector?))
(s/def ::strs-list (s/coll-of ::string   :kind vector?))
(s/def ::date-list (s/coll-of ::iso-date :kind vector?))
(s/def ::freq_unit-list (s/coll-of ::freq_unit :kind vector?))

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

(defn wrap-filter-dummy-values
  "there is a kludge in-place to force certain form inputs to always be a list
  because otherwize coercion fails. here we remove the extra items"
  [handler]
  (fn [req]
    (let [;; nuke-values #(vec (remove #{-1 "59866220-59be-4143-90b3-63c2861eadca"} %))
          nuke-values #(vec (rest %))
          req-cleaned 
          (loop [xs req, keys [:task_id
                               :habit_id
                               :thing_id
                               :task_newname
                               :habit_name_new
                               :freq_value_new
                               :freq_unit_new
                               :due_new
                               :linkedpage]]
            (if (empty? keys)
              xs
              (let [[key & keys'] keys
                    xs' (update-in xs [:parameters :form key] nuke-values)]
                (recur xs' keys'))))]
      
      (handler req-cleaned))))

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
        {:middleware [wrap-auth
                      wrap-filter-dummy-values]
         :post {:parameters {:path {:page-name ::string}}}}

        ;; single-request actions. these run a db query and re-display the
        ;; normal page contents
        ["add-task"
         {:post {:handler tc/add-task-handler
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_name ::string}}}}]
        ["add-habit"
         {:post {:handler hc/add-habit-handler
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_name ::string
                                     :freq_value ::int
                                     :freq_unit ::freq_unit}}}}]

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

        ;; multi-request actions. these actions have a view page, where the user
        ;; is asked for more input, and a save page, which will run a query and
        ;; re-display the normal page contents
        ["modify-task-view"
         {:post {:handler tc/modify-task-view
                 :parameters {:form {:task_id ::ints-list}}}}]
        ["modify-task-save"
         {:post {:handler tc/modify-task-save
                 :middleware [tc/wrap-show-tasks]
                 :parameters {:form {:task_id ::ints-list
                                     :task_newname ::strs-list}}}}]

        ["modify-habit-view"
         {:post {:handler hc/modify-habit-view
                 :parameters {:form {:habit_id ::ints-list}}}}]
        ["modify-habit-save"
         {:post {:handler hc/modify-habit-save
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :habit_name_new ::strs-list
                                     :freq_value_new ::ints-list
                                     :freq_unit_new ::freq_unit-list
                                     :due_new ::date-list
                                     }}}}]

        ;; the defer task page has three possible actions
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
                                     :new-catname ::string}}}}]

        ;; defer habit only has one submit option
        ["defer-habit-view"
         {:post {:handler hc/defer-habit-view
                 :parameters {:form {:habit_id ::ints-list}}}}]
        ["defer-habit-date-save"
         {:post {:handler hc/defer-habit-date-save
                 :middleware [hc/wrap-show-habits]
                 :parameters {:form {:habit_id ::ints-list
                                     :date ::iso-date}}}}]

        ;; actions for the agenda page
        ["done-delete"
         {:post {:handler ac/done-delete-handler
                 :middleware [ac/wrap-show-agenda]
                 :parameters {:form {:thing_id ::thing-list}}}}]

        ["todo-thing"
         {:post {:handler ac/todo-thing-handler
                 :parameters {:form {:thing_id ::thing-list
                                     :action ::todo-actions}}}}]

        ]

       ["/settings"
        {:middleware [wrap-auth]
         :get {:handler sc/settings-handler}}]

       ["/settings/"
        {:middleware [wrap-auth]}
        ["add-page"
         {:post {:handler sc/add-page-handler
                 :middleware [sc/wrap-settings-page]
                 :parameters {:form {:new_pagename ::string
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

        ["update_agenda_pages"
         {:post {:handler sc/update-agenda-handler
                 :middleware [wrap-filter-dummy-values
                              sc/wrap-settings-page]
                 :parameters {:form {:page_id ::int
                                     :linkedpage ::ints-list}}}}]]

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
              :middleware [
                           parameters/parameters-middleware
                           rrc/coerce-request-middleware
                           muuntaja/format-response-middleware
                           rrc/coerce-response-middleware
                           logger/wrap-with-logger
                           wrap-query-builder
                           [wrap-session {:store session-store}]
                           wrap-anti-forgery
                           ;; [wrap-debug-reqmap "complete"]
                           ]}})

     ;; default handler
     (wrap-query-builder
      (wrap-session
       hl/not-found-handler {:store session-store}))

     ;; options

     )))
