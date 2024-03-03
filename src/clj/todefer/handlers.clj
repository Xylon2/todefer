(ns todefer.handlers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [hiccup2.core :as h]))

(defn not-found-handler
  "display not found page"
  [& x]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (ph/render-message "Page Not Found")})

(defn home-handler
  "display the list of questions"
  [{exec-query :q-builder}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (ph/render-message (exec-query (q/list-pages)) "Hello World!")})

(defn display-page
  "displays a task, habit or agenda page"
  [{exec-query :q-builder
    {{page-name :page-name} :path} :parameters}]

  ;; check the page_name is legit and find out what type of page it is
  (let [page-list (exec-query (q/list-pages))
        {page-type :page_type
         page-id :page_id} (exec-query (q/get-page page-name))]

    (case page-type
      "task"
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (ph/tasks-page page-list page-name page-id)}
      ;; "habit"
      ;; {:status 200
      ;;  :headers {"Content-Type" "text/html"}
      ;;  :body (ph/habits-page page-list page-name page-id)}
      ;; "agenda"
      ;; {:status 200
      ;;  :headers {"Content-Type" "text/html"}
      ;;  :body (ph/agenda-page page-list page-name page-id)}
      ;; nil
      ;; {:status 404
      ;;  :headers {"Content-Type" "text/html"}
      ;;  :body (ph/render-message "Page Not Found")}
      )))

(defn login-handler
  "show the login prompt. the parameter variable holds the url the user was trying
  to access before being sent here"
  [{{redirect "redirect" :or {redirect "/"}} :query-params
    f-token :anti-forgery-token}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (ph/render-login redirect f-token)})

(defn login-post-handler
  "check the credentials. if success, redirect to the redirect url. else, display
  the login page again"
  [{exec-query :q-builder
    {redirect "redirect" :or {redirect "/"}} :query-params
    session :session
    {{:keys [username password]} :form} :parameters
   f-token :anti-forgery-token}]
  (if (exec-query (q/authenticate-user username password))
    ;; login success
    {:status 303
     :headers {"Location" redirect}
     :body ""
     :session (assoc session :user username)}

    ;; login failure
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (ph/render-login redirect f-token "Login failed")}))
