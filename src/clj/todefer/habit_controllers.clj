(ns todefer.habit-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [hiccup2.core :as h]
            [java-time :as jt]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(defn one-update?
  "takes query results as args. checks they returned 1 update"
  [& queries]
  (every? #(= 1 (num-updated %)) queries))

(defn some-updated?
  "takes query results as args. checks they returned at-least 1 update"
  [& queries]
  (every? #(< 0 (num-updated %)) queries))

(defn get-page-id
  "given the page name, get page id"
  [exec-query page-name]
  (:page_id (exec-query (q/get-page page-name))))

(defn show-habits-200
  [exec-query page-id]
  (let [{:keys [due-habits upcoming-habits]}
        (hl/assemble-habit-page-info exec-query page-id)]
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

(defn wrap-show-habits
  "this is a middleware. wrap it around the action handlers."
  [handler]
  (fn [{exec-query :q-builder
        {{:keys [page-name]} :path} :parameters :as req}]
    (let [page-id (get-page-id exec-query page-name)]
        (if (handler page-id req)
          (show-habits-200 exec-query page-id)
          (show-500 ":o")))))

(def timeunit-multipliers
  {"days" 1
   "weeks" 7
   "months" 28
   "years" 365})

(defn done-habit [exec-query habit_id offset]
  (let [{:keys [freq_unit freq_value]} (first (exec-query (q/get-habit-info [habit_id])))
        dayshence (jt/days (* (get timeunit-multipliers freq_unit) freq_value))
        now (jt/minus (jt/local-date) (jt/days offset))]
    (some-updated? (exec-query (q/defer-habit! [habit_id] (jt/plus now dayshence) true))
                   (exec-query (q/habit-untodo! [habit_id])))))

(defn done-habit-handler
  "mark a habit as done, today or yesturday"
  [_
   {exec-query :q-builder
    {{:keys [habit_id donewhen]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [done-habit' (fn [id] (done-habit exec-query id (case donewhen
                                                         "today" 0
                                                         "yesturday" 1)))]
    (run! done-habit' habit_id)
    true))

(defn delete-habit-handler
  "delete one or more habits"
  [page-id
   {exec-query :q-builder
    {{:keys [habit_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (q/delete-habit! habit_id))))

(defn modify-habit-view
  "always accessed with POST requests.

  We receive a list of habit_ids but no values. Then we render page ready to be
  modified

  this one does not need to be wrapped with wrap-show-habits"
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
  (doseq [[hid hname hvalue hunit hdue] (map vector habit_id habit_name_new freq_value_new freq_unit_new due_new)] ;; pair them up
    (exec-query (q/modify-habit! hid hname hvalue hunit hdue)))
  {:status 303
   :headers {"Location" (str "/page/" page-name)}
   :body ""})

(defn move-habit-handler
  "move one or more habits to a different page"
  [page-id
   {exec-query :q-builder
    {{:keys [habit_id newpage]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (q/move-habit! habit_id newpage))))

(defn todo-habit-handler
  "update one or more habits todo field"
  [page-id
   {exec-query :q-builder
    {{:keys [habit_id action]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (case action
                               "today" (q/habit-today! habit_id)
                               "tomorrow" (q/habit-tomorrow! habit_id)
                               "not" (q/habit-untodo! habit_id)))))

(defn order-habit-handler
  "order habits to top or bottom as appropriate"
  [page-id
   {exec-query :q-builder
    {{:keys [habit_id order]} :form
     {:keys [page-name]} :path} :parameters}]

  (let [;; get all habits for this page
        page-habits (vec (exec-query (q/list-page-habit-order page-id)))
        ;; convert id list to set
        id-set (set habit_id)
        ;; remove ours
        not-ours (vec (remove #(id-set %) page-habits))
        ;; new vector, in-order
        new-vec (case order
                  "top" (into habit_id not-ours)
                  "bottom" (into not-ours habit_id))]
    (doseq [[order_key_habit habit_id] (map-indexed vector new-vec)]
      (exec-query (q/update-habit-order-local habit_id order_key_habit)))
    true))

(defn defer-habit-view
  "always accessed with POST requests.

  We receive a list of habit_ids. We render a date selector for them to choose
  when to defer to.

  this one does not need to be wrapped with wrap-show-habits"
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
  [page-id
   {exec-query :q-builder
    {{:keys [habit_id date]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (q/defer-habit! habit_id date))
                 (exec-query (q/habit-untodo! [habit_id]))))

(defn add-habit-handler
  "this one is not to be wrapped with wrap-show-habits. sometimes it needs to
  return the defer page"
  [{exec-query :q-builder
    {{:keys [habit_name freq_unit freq_value xaction]} :form
     {:keys [page-name]} :path} :parameters :as req}]
  (let [page-id (get-page-id exec-query page-name)
        habit_id (exec-query (q/add-habit! habit_name page-id freq_unit freq_value))]
    (case xaction
      "defer" (defer-habit-view (assoc-in req [:parameters :form :habit_id] [habit_id]))
      "today" (do
                (exec-query (q/habit-today! [habit_id]))
                (show-habits-200 exec-query page-id))
      "tomorrow" (do
                   (exec-query (q/habit-tomorrow! [habit_id]))
                   (show-habits-200 exec-query page-id))
      (show-habits-200 exec-query page-id))))
