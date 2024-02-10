(ns todefer.hiccup
    (:require [hiccup2.core :as h]))

(defn render-base
  "the basic structure of an html document. takes a title and list of elements"
  [title contents]
  (->> [:html {:lang "en"}
        [:head
         [:title title]
         [:link {:rel "stylesheet" :href "/public/style/style.css"}]]
        [:body
         [:header [:h1 title]]
         (into [:main]
               contents)]]
       h/html
       (str "<!DOCTYPE html>")))

(defn render-message
  "render a message on an html page"
  [msg]
  (let [contents [:p msg]]
    (render-base "Message" [contents])))

(defn render-login
  "prompt for login deets"
  [redirect-to f-token & [errormsg]]
  (let [post-to (str "/login?redirect=" redirect-to)
        errorprint (if errormsg
                     [:p [:em errormsg]]
                     "")
        login-form [:form {:method "POST" :action post-to}
                    [:input {:name "__anti-forgery-token"
                             :type "hidden"
                             :value f-token}]
                    [:fieldset
                     [:legend "Please login"]
                     [:label {:for "username"} "Username"]
                     [:input#username {:type "text" :name "username"}]
                     [:br]
                     [:label {:for "password"} "Password"]
                     [:input#password {:type "password" :name "password"}]
                     [:br]]
                    
                    [:input {:type "submit" :value "Login"}]]]
    (render-base "Login" [errorprint login-form])))
