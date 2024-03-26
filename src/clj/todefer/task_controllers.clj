(ns todefer.task-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [hiccup2.core :as h]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(def one-update?   #(= 1 (num-updated %)))
(def some-updated? #(< 0 (num-updated %)))

(defn show-tasks-200
  [exec-query page-id]
  (let [{:keys [due-tasks defcats-named defcats-dated]}
        (hl/assemble-task-page-info exec-query page-id)]
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
        (show-tasks-200 exec-query page-id)
        (show-500 ":o"))))

(defn delete-task-handler
  "delete one or more tasks"
  [{exec-query :q-builder
    {{:keys [task_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (q/delete-task! task_id)))
      (show-tasks-200 exec-query page-id)
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
   (show-tasks-200 exec-query page-id)))

(defn move-task-handler
  "move one or more tasks to a different page"
  [{exec-query :q-builder
    {{:keys [task_id newpage]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (q/move-task! task_id newpage)))
      (show-tasks-200 exec-query page-id)
      (show-500 ":o"))))

(defn todo-task-handler
  "update one or more tasks todo field"
  [{exec-query :q-builder
    {{:keys [task_id action]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (case action
                                     "today" (q/task-today! task_id)
                                     "tomorrow" (q/task-tomorrow! task_id)
                                     "not" (q/task-untodo! task_id))))

      (show-tasks-200 exec-query page-id)
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
  "defer task(s) to a date"
  [{exec-query :q-builder
    {{:keys [task_id date]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)
        categories (exec-query (q/list-defcats-dated page-id date))
        cat-id (if (= 1 (count categories))
                 (:cat_id (first categories))
                 (exec-query (q/create-defcat-dated! date)))]
    (if (some-updated? (exec-query (q/defer-task-dated! cat-id task_id)))
      (show-tasks-200 exec-query page-id)
      (show-500 ":o"))))

(defn defer-task-category-save
  "defer task(s) to a named category"
  [{exec-query :q-builder
    {{:keys [task_id cat_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)]
    (if (some-updated? (exec-query (q/defer-task-named! cat_id task_id)))
      (show-tasks-200 exec-query page-id)
      (show-500 ":o"))))

(defn defer-task-newcategory-save
  "defer task(s) to a new category"
  [{exec-query :q-builder
    {{:keys [task_id new-catname]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [page-id (get-page-id exec-query page-name)
        categories (exec-query (q/list-defcats-named page-id new-catname))
        cat-id (if (< 0 (count categories))
                 (:cat_id (first categories))
                 (exec-query (q/create-defcat-named! new-catname)))]
    (prn categories)
    (if (some-updated? (exec-query (q/defer-task-named! cat-id task_id)))
      (show-tasks-200 exec-query page-id)
      (show-500 ":o"))))

