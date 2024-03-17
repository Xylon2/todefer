(ns todefer.habit-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [hiccup2.core :as h]))

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

(defn add-habit-handler
  "add habit"
  [{exec-query :q-builder
    {{:keys [habit_name freq_unit freq_value]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
      (if (one-update? (exec-query (q/add-habit! habit_name page-id freq_unit freq_value)))
        (show-habits-200 exec-query page-id)
        (show-500 ":o"))))

