(ns todefer.agenda-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [todefer.task-controllers :as tc]
            [todefer.habit-controllers :as hc]
            [clojure.string :as string]
            [hiccup2.core :as h]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(def one-update?   #(= 1 (num-updated %)))
(def some-updated? #(< 0 (num-updated %)))

(defn show-agenda-200
  [exec-query page-id]
  (let [{:keys [todo-today todo-tomorrow]}
        (hl/assemble-agenda-page-info exec-query page-id)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-agenda page-id todo-today todo-tomorrow)
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

(defn wrap-show-agenda
  "this is a middleware. wrap it around the action handlers."
  [handler]
  (fn [{exec-query :q-builder
        {{:keys [page-name]} :path} :parameters :as req}]
    (let [page-id (get-page-id exec-query page-name)]
        (if (handler page-id req)
          (show-agenda-200 exec-query page-id)
          (show-500 ":o")))))

(defn parse-id
  "habit/12 or task/34"
  [s]
  (let [[type id] (string/split s #"/")]
    {:type (keyword type)
     :id (parse-long id)}))

(defn done-delete-handler
  [page-id
   {exec-query :q-builder
    {{:keys [thing_id]} :form
     {:keys [page-name]} :path} :parameters :as req}]
  (doseq [thing thing_id]
   (let [{:keys [type id]} (parse-id thing)]
     (case type
       :task (tc/delete-task-handler -1 (assoc-in req [:parameters :form :task_id] [id]))
       :habit (hc/done-habit-handler -1 (-> req
                                         (assoc-in [:parameters :form :habit_id] [id])
                                         (assoc-in [:parameters :form :donewhen] "today"))))))
  true)

(defn todo-thing-handler
  [page-id
   req]
  true)
