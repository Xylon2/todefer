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
   :body (ph/render-message "Hello World!")})

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
