(ns todefer.hiccup
    (:require [hiccup2.core :as h]))

(defn render-base
  "the basic structure of an html document. takes a title and list of
  elements

  The pagelist is a list of maps, each containing page_link & page_title."
  [title contents & {:keys [pagelist scripts actionbar settings?]}]
  (->> [:html {:lang "en"}
        [:head
         [:title title]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0"}]
         [:meta {:charset "UTF-8"}]
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
                   (for [{:keys [page_name selected]} pagelist]
                     [:li (when selected {:id "selected-page"})
                      [:a {:href (str "/page/" page_name)} page_name]])))
           [:div (when settings? {:id "selected-page"})
            [:a {:href "/settings/"} "⚙"]]]]
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
               (for [{:keys [cat_name tasks cat_id]} defcats-named]
                 [[:button.collapsible {:type "button" :id cat_id} cat_name]
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
        [[:button.collapsible {:type "button" :id "dated"} "Upcoming"]
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

(defn render-habits
  "the meat of a habits page. used both in initial page-load and by AJAX"
  [page-id due-habits upcoming-habits]
  (let [duehiccup
        [[:h2 "Due"]
         [:table
          [:colgroup
           [:col {:style "width: 2em;"}]
           [:col {:style "width: 100%;"}]]
          [:tbody
           ;; this is a kludge to force habit_id to always be a list
           [:input {:type "hidden" :name "habit_id" :value "-1"}]
           (for [{:keys [highlight habit_id habit_name freq_value freq_unit prettydue]} due-habits]
             [:tr (when highlight {:style (str "background-color: " highlight)})
              [:td [:input {:type "checkbox" :name "habit_id" :value habit_id}]]
              [:td habit_name
               [:span.habit-info (str "every " freq_value " " freq_unit ", due " prettydue)]]]
             )]]
         [:br]]
        upcominghiccup
        [[:button.collapsible {:type "button" :id "upcoming"} "Upcoming"]
         [:div.collapsiblecontent
          [:table
           [:colgroup
            [:col {:style "width: 2em;"}]
            [:col {:style "width: 100%;"}]]
           [:tbody
            (for [{:keys [highlight habit_id habit_name freq_value freq_unit prettydue]} upcoming-habits]
              [:tr (when highlight {:style (str "background-color: " highlight)})
               [:td [:input {:type "checkbox" :name "habit_id" :value habit_id}]]
               [:td habit_name
               [:span.habit-info (str "every " freq_value " " freq_unit ", due " prettydue)]]])]]]
         [:div [:br]]]
        ]
    
    (concat duehiccup upcominghiccup)))

(defn tasks-page
  "renders a full page of tasks"
  [pagelist page-name page-id due-tasks defcats-named defcats-dated f-token]
  (let [contents (render-tasks page-id due-tasks defcats-named defcats-dated)
        other-task-pages (filter #(and (= "task" (:page_type %))
                                       (not= page-name (:page_name %)))
                                 pagelist)]
    (render-base
     (str page-name " tasks")
     contents
     :scripts ["/public/cljs/shared.js" "/public/cljs/tasks.js"]
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
                   [:button {:type "button"
                             :hx-post (str "/page/" page-name "/delete-task")
                             :hx-include "[name='task_id']"}
                    "delete"]
                   [:button {:type "button"
                             :hx-post (str "/page/" page-name "/modify-task-view")
                             :hx-include "[name='task_id']"}
                    "modify"]
                   (when (< 0 (count other-task-pages))
                     (into [:select {:name "newpage"
                                     :hx-post (str "/page/" page-name "/move-task")
                                     :hx-include "[name='task_id']"}
                            [:option {:value ""} "move"]]
                           (for [{:keys [page_name page_id]} other-task-pages]
                             [:option {:value page_id} page_name])))
                   [:button {:type "button"
                             :hx-post (str "/page/" page-name "/defer-task-view")
                             :hx-include "[name='task_id']"}
                    "defer"]
                   ]]])))

(defn habits-page
  "renders a full page of habits"
  [page-list page-name page-id due-habits upcoming-habits f-token]
  (let [contents (render-habits page-id due-habits upcoming-habits)
        other-habit-pages (filter #(and (= "habit" (:page_type %))
                                        (not= page-name (:page_name %)))
                                  page-list)]
    (render-base
     (str page-name " habits")
     contents
     :scripts ["/public/cljs/shared.js" "/public/cljs/habits.js"]
     :pagelist page-list
     :actionbar [[:form {:method "post"
                         :style "padding-left: 0;"
                         :hx-target "main"
                         :hx-on:htmx:after-request "this.reset()"}
                  [:input {:name "__anti-forgery-token"
                           :type "hidden"
                           :value f-token}]
                  [:div.t-container
                   [:input#add_new.flex-input {:type "text"
                                               :name "habit_name"}]
                   [:input {:type "number" :name "freq_value" :style "width: 3em"}]
                   [:select {:name "freq_unit"}
                    [:option {:value "days"} "days"]
                    [:option {:value "weeks"} "weeks"]
                    [:option {:value "months"} "months"]
                    [:option {:value "years"} "years"]]
                   [:button {:type "submit"
                             :hx-post (str "/page/" page-name "/add-habit")}
                    "add habit"]]
                  [:div
                   [:select {:name "donewhen"
                             :hx-post (str "/page/" page-name "/done-habit")
                             :hx-include "[name='habit_id']"}
                    [:option {:value ""} "done"]
                    [:option {:value "today"} "today"]
                    [:option {:value "yesturday"} "yesturday"]]
                   [:select {:name "really"
                             :hx-post (str "/page/" page-name "/delete-habit")
                             :hx-include "[name='habit_id']"}
                    [:option {:value ""} "delete"]
                    [:option {:value "really"} "really?"]]
                   [:button {:type "button"
                             :hx-post (str "/page/" page-name "/modify-habit-view")
                             :hx-include "[name='habit_id']"}
                    "modify"]
                   (when (< 0 (count other-habit-pages))
                     (into [:select {:name "newpage"
                                     :hx-post (str "/page/" page-name "/move-habit")
                                     :hx-include "[name='habit_id']"}
                            [:option {:value ""} "move"]]
                           (for [{:keys [page_name page_id]} other-habit-pages]
                             [:option {:value page_id} page_name])))
                   [:button {:type "button"
                             :hx-post (str "/page/" page-name "/defer-habit-view")
                             :hx-include "[name='habit_id']"}
                    "defer"]
                   ]]])))

(defn render-modify-tasks
  "page to modify selected tasks"
  [tasks f-token page-name]
  (list
   [:h2 "Modify tasks"]
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
      [:input {:type "hidden" :name "task_newname" :value "59866220-59be-4143-90b3-63c2861eadca"}]
      (for [{:keys [task_id task_name]} tasks]
        [:tr
         [:td
          [:input {:type "hidden" :name "task_id" :value task_id}]
          [:input {:type "text" :name "task_newname" :value task_name}]]])]]
    [:button {:type "submit"} "Save changes"]]))

(defn render-modify-habits
  "page to modify selected habits"
  [habits f-token page-name]
  (list
   [:h2 "Modify habits"]
   [:form {:method "post"
           :hx-post (str "/page/" page-name "/modify-habit-save")
           :hx-target "main"}
    [:input {:name "__anti-forgery-token"
             :type "hidden"
             :value f-token}]
    [:table
     [:colgroup
      [:col {:style "width: 100%;"}]
      [:col]]
     [:tbody
      ;; this is a kludge to force these inputs to always be a list
      [:input {:type "hidden" :name "habit_id" :value "-1"}]
      [:input {:type "hidden" :name "habit_name_new" :value "59866220-59be-4143-90b3-63c2861eadca"}]
      [:input {:type "hidden" :name "freq_value_new" :value "-1"}]
      [:input {:type "hidden" :name "freq_unit_new" :value "days"}]
      [:input {:type "hidden" :name "due_new" :value "1970-01-01"}]
      (for [{:keys [habit_id habit_name freq_value freq_unit date_scheduled]} habits]
        [:tr
         [:td
          [:input {:type "hidden" :name "habit_id" :value habit_id}]
          [:input.wide-input {:type "text" :name "habit_name_new" :value habit_name}]]
         [:td
          [:input {:type "number" :name "freq_value_new"
                   :value freq_value :style "width: 3em"}]]
         [:td
          (into [:select {:name "freq_unit_new"}]
                (for [unit ["days" "weeks" "months" "years"]]
                  [:option {:value unit :selected (if (= unit freq_unit)
                                                    "selected"
                                                    nil)} unit]))]
         [:td
          [:input {:type "date" :name "due_new" :value date_scheduled}]]])]]
    [:button {:type "submit"} "Save changes"]]))

(defn render-defer-tasks
  "page to defer tasks to either date or category"
  [task_id categories f-token page-name]
  (list
   [:h2 "Defer to..."]
   [:form {:method "post"
           :hx-target "main"}
    [:input {:name "__anti-forgery-token"
             :type "hidden"
             :value f-token}]
    [:input {:type "hidden" :name "task_id" :value "-1"}]
    (for [tid task_id]
      [:input {:type "hidden" :name "task_id" :value tid}])
    [:h3 "date"]
    [:input {:type "date"
             :name "date"
             :hx-post (str "/page/" page-name "/defer-task-date-save")}]
    [:h3 "existing category"]
    (when (< 0 (count categories))
      (into [:select {:name "cat_id"
                      :hx-post (str "/page/" page-name "/defer-task-category-save")}
             [:option "select category"]]
            (for [{:keys [cat_id cat_name]} categories]
              [:option {:value cat_id} cat_name]
              )))
    [:h3 "new category"]
    [:input {:type "text" :name "new-catname"}]
    [:button {:type "submit"
              :hx-post (str "/page/" page-name "/defer-task-newcategory-save")}
     "submit"]]))

(defn render-defer-habits
  "page to defer habits to a date"
  [habit_id f-token page-name]
  (list
   [:h2 "Defer until..."]
   [:form {:method "post"
           :hx-target "main"}
    [:input {:name "__anti-forgery-token"
             :type "hidden"
             :value f-token}]
    [:input {:type "hidden" :name "habit_id" :value "-1"}]
    (for [tid habit_id]
      [:input {:type "hidden" :name "habit_id" :value tid}])
    [:input {:type "date"
             :name "date"
             :hx-post (str "/page/" page-name "/defer-habit-date-save")}]]))

(defn annotate-positions
  "we need a way of knowing if the element is first or last."
  [coll]
  (let [coll-count (count coll)]
    (map-indexed (fn [idx elem]
                   [elem (cond
                           (= idx 0) :first
                           (= idx (dec coll-count)) :last
                           :else :middle)])
                 coll)))

(defn settings-page
  "renders a the settings"
  [page-list f-token]
  (let []
    (render-base
     (str "Settings")
     [[:h2 "Pages"]
      [:form {:method "post"}
       [:input {:name "__anti-forgery-token"
                :type "hidden"
                :value f-token}]
       [:table
        [:colgroup
         [:col {:style "width: 100%;"}]
         [:col]
         [:col]
         [:col]
         [:col]]
        [:tbody
         (for [[{:keys [page_id page_name page_type]} pos] (annotate-positions page-list)]
           [:tr
            [:td page_name]
            [:td page_type]
            [:td [:button {:type "submit" :name "page_id" :value page_id} "delete"]]
            [:td (when (not= pos :first)
                   [:button {:type "submit" :name "page_id" :value page_id} "⇧"])]
            [:td (when (not= pos :last)
                   [:button {:type "submit" :name "page_id" :value page_id} "⇩"])]])]]]]
     :pagelist page-list
     :settings? true)))
