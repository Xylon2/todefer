(ns todefer.todo-controllers
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

(defn show-todo-200
  [exec-query page-id]
  (let [{:keys [todo-today todo-tomorrow]}
        (hl/assemble-todo-page-info exec-query page-id)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-todo page-id todo-today todo-tomorrow)
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

(defn wrap-show-todo
  "this is a middleware. wrap it around the action handlers."
  [handler]
  (fn [{exec-query :q-builder
        {{:keys [page-name]} :path} :parameters :as req}]
    (let [page-id (get-page-id exec-query page-name)]
        (if (handler page-id req)
          (show-todo-200 exec-query page-id)
          (show-500 ":o")))))

(defn dispatch-case
  "essentially a custom case function which passes the id into the provided fn"
  [thing & {:as cases}]
  (let [[type id] (string/split thing #"/")
        type (keyword type)
        id (parse-long id)]
    (if-let [handler (get cases type)]
      (handler id)
      (throw (ex-info "Invalid type" {:type type})))))

(defn done-delete-handler
  [page-id
   {exec-query :q-builder
    {{:keys [thing_id]} :form
     {:keys [page-name]} :path} :parameters :as req}]
  (doseq [thing thing_id]
   (dispatch-case thing
      :task (fn [id]
              (tc/delete-task-handler -1 (assoc-in req [:parameters :form :task_id] [id])))
      :habit (fn [id]
               (hc/done-habit-handler -1 (-> req
                                             (assoc-in [:parameters :form :habit_id] [id])
                                             (assoc-in [:parameters :form :donewhen] "today"))))))
  true)

(defn todo-thing-handler
  [page-id
   {exec-query :q-builder
    {{:keys [thing_id]} :form
     {:keys [page-name]} :path} :parameters :as req}]
  (doseq [thing thing_id]
    (dispatch-case thing
                   :task (fn [id]
                           (tc/todo-task-handler -1 (assoc-in req [:parameters :form :task_id] [id])))
                   :habit (fn [id]
                            (hc/todo-habit-handler -1 (assoc-in req [:parameters :form :habit_id] [id])))))
  true)

(defn add-task-handler
  [page-id
   {exec-query :q-builder
    {{:keys [task_name tpage aaction]} :form
     {:keys [page-name]} :path} :parameters :as req}]
  (let [task_id (exec-query (q/add-task! task_name tpage))]
    (case aaction
      "today" (exec-query (q/task-today! [task_id]))
      "tomorrow" (exec-query (q/task-tomorrow! [task_id]))
      true)))

(defn order-thing-handler
  "order tasks to top or bottom as appropriate"
  [page-id
   {exec-query :q-builder
    {{:keys [thing_id order]} :form
     {:keys [page-name]} :path} :parameters}]

  (let [;; get all things for this page and sort them
        {:keys [todo-today todo-tomorrow]} (hl/assemble-todo-page-info exec-query page-id)
        all-things (sort-by :order_key_todo hl/my-compare (into todo-today todo-tomorrow))
        ;; need just vector of thing ids
        all-things' (mapv (fn [{ttype :ttype :as todo-item}]
                            (case ttype
                              "task"
                              (str "task/" (:task_id todo-item))
                              "habit"
                              (str "habit/" (:habit_id todo-item)))) all-things)
        ;; convert id list to set
        id-set (set thing_id)
        ;; remove ours
        not-ours (vec (remove #(id-set %) all-things'))
        ;; new vector, in-order
        new-vec (case order
                  "top" (into thing_id not-ours)
                  "bottom" (into not-ours thing_id))]

    (doseq [[order_key_todo thing_id] (map-indexed vector new-vec)]
      (dispatch-case thing_id
                     :task  #(exec-query (q/update-task-order-todo  % order_key_todo))
                     :habit #(exec-query (q/update-habit-order-todo % order_key_todo))))
    true))

(defn modify-thing-view
  "dispatch to either modify-task-view or modify-habit-view"
  [{exec-query :q-builder
    {{:keys [thing_id]} :form
     {:keys [page-name]} :path} :parameters :as req}]
  (dispatch-case (first thing_id)
                 :task (fn [id]
                         (tc/modify-task-view (assoc-in req [:parameters :form :task_id] [id])))
                 :habit (fn [id]
                          (hc/modify-habit-view (assoc-in req [:parameters :form :habit_id] [id])))))
