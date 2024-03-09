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
            [ring.middleware.session.memory :as memory]
            [clojure.spec.alpha :as s]
            [todefer.handlers :as hl]
            [todefer.task-controllers :as tc]
            [ring.logger :as logger]
            [clojure.string :as string]
            [clojure.pprint]
            [clojure.java.io]))

(s/def ::id int?)
(s/def ::string string?)

;; the reason we put the session store in this atom is so we can log ourselfes
;; in when testing
(def session-atom (atom {}))

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

(defn app
  "reitit with format negotiation and input & output coercion"
  [q-builder]
  ;; we define a middleware that includes our query builder
  (let [wrap-query-builder (fn [handler]
                             (fn [request]
                               (handler (assoc request :q-builder q-builder))))
        session-store (memory/memory-store session-atom)]

    (ring/ring-handler
     (ring/router
      [["/" {:middleware [wrap-auth]
             :handler hl/home-handler}]
       ["/page/:page-name" {:middleware [wrap-auth]
                            :get {:handler hl/display-page
                                  :parameters {:path {:page-name ::string}}}}]
       ["/page/:page-name/add-task" {:middleware [wrap-auth]
                                     :post {:handler tc/add-task-handler
                                            :parameters {:path {:page-name ::string}
                                                         :form {:task_name ::string}}}}]
       ["/login" {:get {:handler hl/login-handler}
                  :post {:handler hl/login-post-handler
                         :parameters {:form {:username ::string
                                             :password ::string}}}}]
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
