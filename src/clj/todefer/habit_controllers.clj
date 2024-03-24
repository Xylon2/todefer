(ns todefer.habit-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [hiccup2.core :as h]
            [java-time :as jt]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(def one-update?   #(= 1 (num-updated %)))
(def some-updated? #(< 0 (num-updated %)))

(defn show-habits-200
  [exec-query page-id]
  (let [due-habits (-> (exec-query (q/list-due-habits page-id))
                       (hl/prettify-due :date_scheduled))
        upcoming-habits (-> (exec-query (q/list-upcoming-habits page-id))
                            (hl/prettify-due :date_scheduled))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-habits page-id due-habits upcoming-habits)
               h/html
               str)}))

(defn show-500
  [message]
  {:status 500
   :headers {"Content-Type" "text/html"}
   :body message})

(defn get-page-id
  "given the page name, get page id"
  [exec-query page-name]
  (:page_id (exec-query (q/get-page page-name))))

(def timeunit-multipliers
  {"days" 1
   "weeks" 7
   "months" 28
   "years" 365})

(defn done-habit [exec-query habit_id offset]
  (let [{:keys [freq_unit freq_value]} (first (exec-query (q/get-habit-info [habit_id])))
        dayshence (jt/days (* (get timeunit-multipliers freq_unit) freq_value))
        now (jt/minus (jt/local-date) (jt/days offset))]
    (and
     (some-updated? (exec-query (q/defer-habit! [habit_id] (jt/plus now dayshence) true)))
     (some-updated? (exec-query (q/habit-untodo! [habit_id]))))))

(defn add-habit-handler
  "add habit"
  [{exec-query :q-builder
    {{:keys [habit_name freq_unit freq_value]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
      (if (one-update? (exec-query (q/add-habit! habit_name page-id freq_unit freq_value)))
        (show-habits-200 exec-query page-id)
        (show-500 ":o"))))

(defn done-habit-handler
  "mark a habit as done, today or yesturday"
  [{exec-query :q-builder
    {{:keys [habit_id donewhen]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)
        done-habit' (fn [id] (done-habit exec-query id (case donewhen
                                                         "today" 0
                                                         "yesturday" 1)))]
    (run! done-habit' habit_id)
    (show-habits-200 exec-query page-id)))

(defn delete-habit-handler
  "delete one or more habits"
  [{exec-query :q-builder
    {{:keys [habit_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (q/delete-habit! habit_id)))
      (show-habits-200 exec-query page-id)
      (show-500 ":o"))))

(defn modify-habit-view
  "always accessed with POST requests.

  We receive a list of habit_ids but no values. Then we render page ready to be
  modified"
  [{exec-query :q-builder
    {{:keys [habit_id]} :form
     {page-name :page-name} :path} :parameters :as request
    f-token :anti-forgery-token}]
  (let [habits (exec-query (q/get-habit-info habit_id))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-modify-habits habits f-token page-name)
               h/html
               str)}))

(defn modify-habit-save
  "always accessed with POST requests. 

  We receive a habit-list and values for each. We save the changes and redirect
  to the original page."
  [{exec-query :q-builder
    {{:keys [habit_id habit_name_new freq_value_new freq_unit_new due_new]} :form
     {page-name :page-name} :path} :parameters :as request
    f-token :anti-forgery-token}]
  (let [page-id (get-page-id exec-query page-name)]
    (doseq [[hid hname hvalue hunit hdue] (map vector habit_id habit_name_new freq_value_new freq_unit_new due_new)] ;; pair them up
      (exec-query (q/modify-habit! hid hname hvalue hunit hdue)))
    (show-habits-200 exec-query page-id)))

(defn move-habit-handler
  "move one or more habits to a different page"
  [{exec-query :q-builder
    {{:keys [habit_id newpage]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (q/move-habit! habit_id newpage)))
      (show-habits-200 exec-query page-id)
      (show-500 ":o"))))

(defn todo-habit-handler
  "update one or more habits todo field"
  [{exec-query :q-builder
    {{:keys [habit_id action]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (case action
                                     "today" (q/habit-today! habit_id)
                                     "tomorrow" (q/habit-tomorrow! habit_id)
                                     "not" (q/habit-untodo! habit_id))))
      (show-habits-200 exec-query page-id)
      (show-500 ":o"))))

(defn defer-habit-view
  "always accessed with POST requests.

  We receive a list of habit_ids. We render a date selector for them to choose
  when to defer to. "
  [{exec-query :q-builder
    {{:keys [habit_id]} :form
     {page-name :page-name} :path} :parameters :as request
    f-token :anti-forgery-token}]
  (let [page-id (get-page-id exec-query page-name)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-defer-habits habit_id f-token page-name)
               h/html
               str)}))

(defn defer-habit-date-save
  "defer habit(s) to a date"
  [{exec-query :q-builder
    {{:keys [habit_id date]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (q/defer-habit! habit_id date)))
      (show-habits-200 exec-query page-id)
      (show-500 ":o"))))
