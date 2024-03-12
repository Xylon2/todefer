(ns todefer.hiccup
    (:require [hiccup2.core :as h]))

(defn render-base
  "the basic structure of an html document. takes a title and list of
  elements

  The pagelist is a list of maps, each containing page_link & page_title."
  [title contents & {:keys [pagelist scripts actionbar]}]
  (->> [:html {:lang "en"}
        [:head
         [:title title]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0"}]
         [:link {:rel "stylesheet" :href "/public/style/style.css"}]
         [:script {:src "https://unpkg.com/htmx.org@1.9.10"
          :integrity "sha384-D1Kt99CQMDuVetoL1lrYwg5t+9QdHe7NLX/SoJYkXDFfX37iInKRy5xLSi8nO7UC"
          :crossorigin "anonymous"}]]
        [:body
         [:div#topbar.hero-header
          [:nav#navbar.width
           (if (empty? pagelist)
             [:ul [:li [:a {:href "/login"} "Login"]]]
             (into [:ul]
                   (for [{page_name :page_name} pagelist]
                     [:li [:a {:href (str "/page/" page_name)} page_name]])))]]
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
           ;; this is a kludge to force task_id to always be a list
           [:input {:type "hidden" :name "task_id" :value "-1"}]
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
  [pagelist page-name page-id due-tasks defcats-named defcats-dated f-token]
  (let [contents (render-tasks page-id due-tasks defcats-named defcats-dated)]
    (render-base
     (str page-name " tasks")
     contents
     :scripts ["/public/cljs/todefer.js"]
     :pagelist pagelist
     :actionbar [[:form {:method "post"
                         :style "padding-left: 0;"
                         :hx-target "main"
                         :hx-on:htmx:after-request "this.reset()"}
                  [:input {:name "__anti-forgery-token"
                           :type "hidden"
                           :value f-token}]
                  [:div.t-container
                   [:input#add_new.flex-input {:type "text"
                                               :name "task_name"}]
                   [:button {:type "submit"
                             :hx-post (str "/page/" page-name "/add-task")}
                    "add task"]]
                  [:div
                   [:button {:type "submit"
                             :hx-post (str "/page/" page-name "/delete-task")
                             :hx-include "[name='task_id']"}
                    "delete"]
                   [:button {:type "submit"
                             :hx-post (str "/page/" page-name "/modify-task-view")
                             :hx-include "[name='task_id']"}
                    "modify"]]]])))

(defn render-modify-tasks
  "page to modify selected tasks"
  [tasks f-token page-name]
  (list[:h2 "Modify tasks"]
       [:form {:method "post"
               :hx-post (str "/page/" page-name "/modify-task-save")
               :hx-target "main"}
        [:input {:name "__anti-forgery-token"
                 :type "hidden"
                 :value f-token}]
        [:table
         [:colgroup
          [:col]]
         [:tbody
           ;; this is a kludge to force these inputs to always be a list
           [:input {:type "hidden" :name "task_id" :value "-1"}]
           [:input {:type "hidden" :name "task_newname" :value "-1"}]
          (for [{:keys [task_id task_name]} tasks]
            [:tr
             [:td
              [:input {:type "hidden" :name "task_id" :value task_id}]
              [:input {:type "text" :name "task_newname" :value task_name}]]])]]
        [:button {:type "submit"} "Save changes"]]))
