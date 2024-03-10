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

(defn add-task-handler
  "add task"
  [{exec-query :q-builder
    {{:keys [task_name]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [{page-type :page_type
         page-id :page_id} (exec-query (q/get-page page-name))]
      (if (one-update? (exec-query (q/add-task! task_name page-id)))
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
                     str)})
     
        {:status 500})))

(defn delete-task-handler
  "delete one or more tasks"
  [{exec-query :q-builder
    {{:keys [task_id]} :form
     {:keys [page-name]} :path} :parameters}]
  (let [{page-type :page_type
         page-id :page_id} (exec-query (q/get-page page-name))]
    (if (< 0 (num-updated (exec-query (q/delete-task! task_id))))
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
                   str)})
      
      {:status 500
       :headers {"Content-Type" "text/html"}
       :body ":o"})))