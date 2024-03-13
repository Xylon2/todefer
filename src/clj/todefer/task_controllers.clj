(ns todefer.task-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [hiccup2.core :as h]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(defn check-num-updated [n x]
  (= n (num-updated x)))

(def one-update? #(check-num-updated 1 %))

(defn show-tasks-200
  [exec-query page-name page-id]
  (let [due-tasks (exec-query (q/list-due-tasks page-id))
        defcats-named (map #(hl/add-tasks-named exec-query %)
                           (exec-query (q/list-defcats-named page-id)))
        defcats-dated (map #(hl/add-tasks-dated exec-query %)
                           (map #(hl/prettify-due % :def_date)
                                (hl/list-defcats-dated-undefer exec-query page-id)))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-tasks page-id due-tasks defcats-named defcats-dated)
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

(defn add-task-handler
  "add task"
  [{exec-query :q-builder
    {{:keys [task_name]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
      (if (one-update? (exec-query (q/add-task! task_name page-id)))
        (show-tasks-200 exec-query page-name page-id)
        (show-500 ":o"))))

(defn delete-task-handler
  "delete one or more tasks"
  [{exec-query :q-builder
    {{:keys [task_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (< 0 (num-updated (exec-query (q/delete-task! task_id))))
      (show-tasks-200 exec-query page-name page-id)
      (show-500 ":o"))))

(defn modify-task-view
  "always accessed with POST requests.

  We receive a list of task_ids but no values. Then we render page ready to be
  modified"
  [{exec-query :q-builder
    {{:keys [task_id]} :form
     {page-name :page-name} :path} :parameters :as request
    f-token :anti-forgery-token}]
  (let [tasks (exec-query (q/get-task-info task_id))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-modify-tasks tasks f-token page-name)
               h/html
               str)}))

(defn modify-task-save
  "always accessed with POST requests. 

  We receive a task-list and values for each. We save the changes and redirect
  to the original page."
  [{exec-query :q-builder
    {{:keys [task_id task_newname]} :form
     {page-name :page-name} :path} :parameters :as request
    f-token :anti-forgery-token}]
  (let [page-id (get-page-id exec-query page-name)]
    (doseq [[tid tname] (map vector task_id task_newname)]  ;; pair them up
      (exec-query (q/modify-task! tid tname)))
   (show-tasks-200 exec-query page-name page-id)))

(defn move-task-handler
  "move one or more tasks to a different page"
  [{exec-query :q-builder
    {{:keys [task_id newpage]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (< 0 (num-updated (exec-query (q/move-task! newpage task_id))))
      (show-tasks-200 exec-query page-name page-id)
      (show-500 ":o"))))

(defn defer-task-view
  "always accessed with POST requests.

  We receive a list of task_ids. We render a date selector for them to choose
  when to defer to. "
  [{exec-query :q-builder
    {{:keys [task_id]} :form
     {page-name :page-name} :path} :parameters :as request
    f-token :anti-forgery-token}]
  (let [page-id (get-page-id exec-query page-name)
        categories (exec-query (q/list-defcats-named page-id))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-defer-tasks task_id categories f-token page-name)
               h/html
               str)}))

(defn defer-task-date-save
  ""
  []
  true)

(defn defer-task-category-save
  ""
  []
  true)

(defn defer-task-newcategory-save
  ""
  []
  true)

