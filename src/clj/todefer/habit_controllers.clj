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
  (let [due-habits (map #(hl/prettify-due % :date_scheduled)
                       (exec-query (q/list-due-habits page-id)))
        upcoming-habits (map #(hl/prettify-due % :date_scheduled)
                            (exec-query (q/list-upcoming-habits page-id)))]
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
    (some-updated? (exec-query (q/defer-habit! [habit_id] (jt/plus now dayshence) true)))))

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
