(ns todefer.hiccup
    (:require [hiccup2.core :as h]))

(defn render-base
  "the basic structure of an html document. takes a page-list title and list of
  elements

  The pagelist is a list of maps, each containing page_link & page_title.

  If a fourth arg is provided it will go into the actionbar"
  [title contents & {:keys [pagelist scripts actionbar]}]
  (->> [:html {:lang "en"}
        [:head
         [:title title]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0"}]
         [:link {:rel "stylesheet" :href "/public/style/style.css"}]]
        [:body
         [:div#topbar.hero-header
          [:nav#navbar.width
           (if (empty? pagelist)
             [:ul [:li [:a {:href "/login"} "Login"]]]
             (into [:ul]
                   (for [{page_title :page_name
                          page_link :page_id} pagelist]
                     [:li [:a {:href page_link} page_title]])))]]
         (when-not (empty? actionbar)
           [:div#actionbar.hero-header
            (into [:div.width] actionbar)])
         
         (into [:main.width]
               contents)
         (when-not (empty? scripts)
           (for [script scripts]
             [:script {:type "text/javascript" :src script}]))]]
       h/html
       (str "<!DOCTYPE html>")))

(defn render-message
  "render a message on an html page"
  [msg & [pagelist]]
  (println (str "pagelist: " pagelist))
  (let [contents [:p msg]]
    (apply render-base "Message" [contents] (when pagelist
                                              [:pagelist pagelist]))))

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
  (let [duehiccup
        [[:h2 "Due"]
         [:table
          [:colgroup
           [:col {:style "width: 2em;"}]
           [:col {:style "width: 100%;"}]]
          [:tbody
           (for [{:keys [highlight task_id task_name]} due-tasks]
             [:tr (when highlight {:style (str "background-color: " highlight)})
              [:td [:input {:type "checkbox" :name "task_id" :value task_id}]]
              [:td task_name]]
             )]]
         [:br]]
        dnamedhiccup
        (apply concat
               (for [{:keys [cat_name tasks]} defcats-named]
                 [[:button.collapsible {:type "button"} cat_name]
                  [:div.collapsiblecontent
                   [:table
                    [:colgroup
                     [:col {:style "width: 2em;"}]
                     [:col {:style "width: 100%;"}]]
                    [:tbody
                     (for [{:keys [highlight task_id task_name]} tasks]
                       [:tr (when highlight {:style (str "background-color: " highlight)})
                        [:td [:input {:type "checkbox" :name "task_id" :value task_id}]]
                        [:td task_name]]
                       )]]]
                  [:div [:br]]]))
        ddatedhiccup
        [[:button.collapsible {:type "button"} "Upcoming"]
         [:div.collapsiblecontent
          [:table
           [:colgroup
            [:col {:style "width: 2em;"}]
            [:col {:style "width: 100%;"}]
            [:col]]
           [:tbody
            (for [{:keys [tasks prettydue]} defcats-dated]
              (for [{:keys [highlight task_id task_name]} tasks]
                [:tr (when highlight {:style (str "background-color: " highlight)})
                 [:td [:input {:type "checkbox" :name "task_id" :value task_id}]]
                 [:td task_name]
                 [:td prettydue]]
                ))]]]
         [:div [:br]]]
        ]
    (concat duehiccup dnamedhiccup ddatedhiccup)))

(defn tasks-page
  "renders a full page of tasks"
  [pagelist page-name page-id due-tasks defcats-named defcats-dated]
  (let [contents (render-tasks page-id due-tasks defcats-named defcats-dated)]
    (render-base (str page-name " tasks") contents :scripts ["/public/cljs/todefer.js"] :pagelist pagelist)))
