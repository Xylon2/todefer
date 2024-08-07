(ns todefer.task-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [hiccup2.core :as h]))

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

(defn show-tasks-200
  [exec-query page-id]
  (let [{:keys [due-tasks defcats-named defcats-dated]}
        (hl/assemble-task-page-info exec-query page-id)]
    {:status 200
     :headers {"Content-Type" "text/html"
               "HX-Trigger" "clearform"}
     :body (-> (ph/render-tasks page-id due-tasks defcats-named defcats-dated)
               h/html
               str)}))

(defn show-500
  [message]
  {:status 500
   :headers {"Content-Type" "text/html"}
   :body message})

(defn wrap-show-tasks
  "this is a middleware. wrap it around the action handlers."
  [handler]
  (fn [{exec-query :q-builder
        {{:keys [page-name]} :path} :parameters :as req}]
    (let [page-id (get-page-id exec-query page-name)]
        (if (handler page-id req)
          (show-tasks-200 exec-query page-id)
          (show-500 ":o")))))

(defn delete-task-handler
  "delete one or more tasks"
  [_
   {exec-query :q-builder
    {{:keys [task_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (q/delete-task! task_id))))

(defn modify-task-view
  "always accessed with POST requests.

  We receive a list of task_ids but no values. Then we render page ready to be
  modified

  this one does not need to be wrapped with wrap-show-tasks"
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
  (doseq [[tid tname] (map vector task_id task_newname)]  ;; pair them up
    (exec-query (q/modify-task! tid tname)))
  {:status 303
   :headers {"Location" (str "/page/" page-name)}
   :body ""})

(defn move-task-handler
  "move one or more tasks to a different page"
  [page-id
   {exec-query :q-builder
    {{:keys [task_id newpage]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (q/move-task! task_id newpage))))

(defn todo-task-handler
  "update one or more tasks todo field"
  [_
   {exec-query :q-builder
    {{:keys [task_id action]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (case action
                               "today" (q/task-today! task_id)
                               "tomorrow" (q/task-tomorrow! task_id)
                               "not" (q/task-untodo! task_id)))))

(defn order-task-handler
  "order tasks to top or bottom as appropriate"
  [page-id
   {exec-query :q-builder
    {{:keys [task_id order]} :form
     {:keys [page-name]} :path} :parameters}]

  (let [;; get all tasks for this page
        page-tasks (vec (exec-query (q/list-page-task-order page-id)))
        ;; convert id list to set
        id-set (set task_id)
        ;; remove ours
        not-ours (vec (remove #(id-set %) page-tasks))
        ;; new vector, in-order
        new-vec (case order
                  "top" (into task_id not-ours)
                  "bottom" (into not-ours task_id))]
    (doseq [[order_key_task task_id] (map-indexed vector new-vec)]
      (exec-query (q/update-task-order-local task_id order_key_task)))
    true))

(defn defer-task-view
  "always accessed with POST requests.

  We receive a list of task_ids. We render a date selector for them to choose
  when to defer to.

  this one does not need to be wrapped with wrap-show-tasks"
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
  [page-id
   {exec-query :q-builder
    {{:keys [task_id date]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [categories (exec-query (q/list-defcats-dated page-id date))
        cat-id (if (= 1 (count categories))
                 (:cat_id (first categories))
                 (exec-query (q/create-defcat-dated! date)))]
    (some-updated? (exec-query (q/defer-task-dated! cat-id task_id))
                   (exec-query (q/task-untodo! task_id)))))

(defn defer-task-category-save
  "defer task(s) to a named category"
  [page-id
   {exec-query :q-builder
    {{:keys [task_id cat_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (some-updated? (exec-query (q/defer-task-named! cat_id task_id))
                 (exec-query (q/task-untodo! task_id))))

(defn defer-task-newcategory-save
  "defer task(s) to a new category"
  [page-id
   {exec-query :q-builder
    {{:keys [task_id new-catname]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [categories (exec-query (q/list-defcats-named page-id new-catname))
        cat-id (if (< 0 (count categories))
                 (:cat_id (first categories))
                 (exec-query (q/create-defcat-named! new-catname)))]
    (some-updated? (exec-query (q/defer-task-named! cat-id task_id))
                   (exec-query (q/task-untodo! task_id)))))

(defn defer-task-not
  "undefer a task from any type of defer"
  [_
   {exec-query :q-builder
    {{:keys [task_id]} :form} :parameters}]
  (some-updated? (exec-query (q/undefer-task-named! task_id))
                 (exec-query (q/undefer-task-dated! task_id))))

(defn add-task-handler
  "this one is not to be wrapped with wrap-show-tasks. sometimes it needs to
  return the defer page"
  [{exec-query :q-builder
    {{:keys [task_name xaction]} :form
     {:keys [page-name]} :path} :parameters :as req}]
  (let [page-id (get-page-id exec-query page-name)
        task_id (exec-query (q/add-task! task_name page-id))]
    (case xaction
      "defer" (defer-task-view (assoc-in req [:parameters :form :task_id] [task_id]))
      "today" (do
                (exec-query (q/task-today! [task_id]))
                (show-tasks-200 exec-query page-id))
      "tomorrow" (do
                   (exec-query (q/task-tomorrow! [task_id]))
                   (show-tasks-200 exec-query page-id))
      (show-tasks-200 exec-query page-id))))
