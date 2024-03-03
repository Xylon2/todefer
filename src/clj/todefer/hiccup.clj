(ns todefer.hiccup
    (:require [hiccup2.core :as h]))

(defn render-base
  "the basic structure of an html document. takes a page-list title and list of
  elements

  The pagelist is a list of maps, each containing page_link & page_title"
  [pagelist title contents]
  (->> [:html {:lang "en"}
        [:head
         [:title title]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0"}]
         [:link {:rel "stylesheet" :href "/public/style/style.css"}]]
        [:body
         [:div.hero-header
          [:nav#navbar.width
           (into [:ul]
                 (for [{page_title :page_name
                        page_link :page_id} pagelist]
                   [:li [:a {:href page_link} page_title]]))]]
         [:header.width [:h1 title]]
         (into [:main.width]
               contents)]]
       h/html
       (str "<!DOCTYPE html>")))

(defn render-message
  "render a message on an html page"
  [pagelist msg]
  (let [contents [:p msg]]
    (render-base pagelist "Message" [contents])))

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

(defn render-tasks
  "the meat of a tasks page. used both in initial page-load and by AJAX"
  [page-id due-tasks defcats-named defcats-dated]
  [:p "hi"]
  )

(defn tasks-page
  "renders a full page of tasks"
  [pagelist page-name page-id due-tasks defcats-named defcats-dated]
  (let [contents (render-tasks page-id due-tasks defcats-named defcats-dated)]
    (render-base pagelist (str page-name " tasks") [contents])))
