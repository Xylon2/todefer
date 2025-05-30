(ns todefer.queries
  ;; exclude these core clojure functions
  (:refer-clojure :exclude [distinct filter for group-by into partition-by set update])
  
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer :all]  ;; shadows core functions
            [clojure.core :as c]  ;; so we can still access core functions
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [buddy.hashers :as hashers]))

;; the core namespace will closure over this with the connection and defaults
(defn execute-query
  "Essentially this function just executes a honeysql query, and runs a
   post-processor on it.

   We take 4 args:

   The first two args are required:
   - the db connection - to be closured in when the system starts up
   - a vector containing the query and post-processor function

   The last two args are optional:
   - debug (truthy or falsy)
   - t-opts - this is a map of arguments for jdbc/with-transaction
   "

  ([conn [query processor]]
   (execute-query conn [query processor] false {:rollback-only false}))

  ([conn [query processor] debug]
   (execute-query conn [query processor] debug {:rollback-only false}))

  ;; debug is truthy or falsy
  ;; t-opts is option-map for jdbc/with-transaction
  ([conn [query processor] debug t-opts]
   (when debug (println (str "raw-query: " query)))
   (let [formatted-query (sql/format query)]
     (when debug (println (str "formatted-query: " formatted-query)))
     (jdbc/with-transaction [t-conn conn t-opts]
       (processor (jdbc/execute! t-conn formatted-query {:builder-fn result-set/as-unqualified-maps}))
       ))))

(defn create-user
  "hashes the password"
  [login password]
  [(-> (insert-into :users)
       (values [{:login login
                 :password (hashers/derive password)}]))

   identity])

(defn authenticate-user
  "we return true or false indicating authentication success"
  [login password]
  [(-> (select :*)
       (from :users)
       (where [:= :login login]))

   (fn [[{hashed :password}]]
     (hashers/check password hashed))])

(defn create-page!
  "we create a page with a name and a type"
  [page_name page_type]
  [(-> (insert-into :appPage)
       (values [{:page_name page_name
                 :page_type [:cast page_type :pageType]}]))
   
   identity])

(defn delete-page!
  "we delete the page"
  [page_id]
  [(-> (delete-from :appPage)
       (where [:= :page_id page_id]))

   identity])

(defn reorder-page!
  "updates the order_key of one page"
  [page_id order_key]
  [(-> (update :appPage)
       (set {:order_key order_key})
       (where [:= :page_id page_id]))

   identity])

(defn rename-page!
  "updates the name of one page"
  [page_id name]
  [(-> (update :appPage)
       (set {:page_name name})
       (where [:= :page_id page_id]))

   identity])

(defn add-task!
  "add a task"
  ([task_name page_ref]
   (add-task! task_name page_ref nil))
  ([task_name page_ref highlight]
   [(-> (insert-into :task)
        (values [{:task_name task_name
                  :page_ref [:cast page_ref :integer]
                  :highlight highlight}])
        (returning :task_id))

    (comp :task_id first)]))

(defn delete-task!
  "delete one or more tasks"
  [task_ids]
  [(-> (delete-from :task)
       (where [:= :task_id [:any [:array task_ids :integer]]]))

   identity])

(defn list-pages
  "lists pages in order"
  []
  [(-> (select :*)
       (from :appPage)
       (order-by :order_key))

   identity])

(defn list-linked-pages
  "to be used on Todo pages only"
  [todo_id]
  [(-> (select :appPage.page_id :appPage.page_name :appPage.page_type)
       (from :appPage)
       (join :pagetodo [:= :appPage.page_id :pagetodo.page_id])
       (where [:= :todo_id todo_id]))

   identity])

(defn nuke-linked-pages!
  "nuke all linked pages for a given todo page"
  [page_id]
  [(-> (delete-from :pagetodo)
       (where [:= :todo_id page_id]))

   identity])

(defn update-linked-pages!
  "update the linked pages of an Todo page"
  [page_id linkedpage]
  [(-> (insert-into :pagetodo)
       (values [{:todo_id page_id :page_id linkedpage}])
       (on-conflict (do-nothing)))

   identity])

(defn get-page
  "gets the info for a single page"
  [page_name]
  [(-> (select :*)
       (from :appPage)
       (where [:= :page_name page_name]))

   first])

(defn get-default-page
  "gets the name of the page with the lowest index"
  []
  [(-> (select [:page_name])
       (from :appPage)
       (order-by :order_key)
       (limit 1))

   identity])

(defn list-due-tasks
  "lists the tasks for a page"
  [page_ref]
  [(-> (select :*)
       (from :task)
       (where [:and [:= :page_ref page_ref]
                    [:is :defcat_named nil]
                    [:is :defcat_dated nil]])
       (order-by [:order_key_task :asc] [:task_id :asc]))

   identity])

(defn list-page-task-order
  "id and order_key for tasks on a given page"
  [page_ref]
  [(-> (select :task_id)
       (from :task)
       (where [:= :page_ref page_ref])
       (order-by [:order_key_task :asc] [:task_id :asc]))

   #(map :task_id %)])

(defn list-page-habit-order
  "id and order_key for habits on a given page"
  [page_ref]
  [(-> (select :habit_id)
       (from :habit)
       (where [:= :page_ref page_ref])
       (order-by [:order_key_habit :asc] [:habit_id :asc]))

   #(map :habit_id %)])

(defn update-task-order
  "given an id and order key, update"
  [column task_id order_key]
  [(-> (update :task)
       (set {column order_key})
       (where [:= :task_id task_id]))

   identity])

(def update-task-order-local #(update-task-order :order_key_task %1 %2))
(def update-task-order-todo #(update-task-order :order_key_todo %1 %2))

(defn update-habit-order
  "given an id and order key, update"
  [column habit_id order_key]
  [(-> (update :habit)
       (set {column order_key})
       (where [:= :habit_id habit_id]))

   identity])

(def update-habit-order-local #(update-habit-order :order_key_habit %1 %2))
(def update-habit-order-todo #(update-habit-order :order_key_todo %1 %2))

(defn list-todo-things
  "lists the todo tasks for a list of pages"
  [table ;; table name
   fname ;; id field
   op    ;; date comparrison operator
   page_refs]
  [(-> (select :*)
       (from table)
       (where [:and
               [:= :page_ref [:any [:array page_refs :integer]]]
               [op :todo :CURRENT_DATE]])
       (order-by [fname :asc]))

   identity])

(def list-todo-tasks-today #(list-todo-things :task :task_id :<= %))
(def list-todo-habits-today #(list-todo-things :habit :habit_id :<= %))
(def list-todo-tasks-tomorrow #(list-todo-things :task :task_id :> %))
(def list-todo-habits-tomorrow #(list-todo-things :habit :habit_id :> %))

(defn list-defcats-named
  "get all the defcats that need to be displayed on a page

   The optional cat_name is used when we want to check if a page contains a
   category of a certain name."
  [page_ref & cat_name]
  [(let [specialfn #(if-not (empty? cat_name)
                      (where % [:= :cat_name (first cat_name)])
                      %)]
     (-> (select-distinct :cat_id :order_key :cat_name)
         (from :defCatNamed)
         (join :task [:= :cat_id :defcat_named])
         (where [:= :page_ref page_ref])
         (specialfn)
         (order-by :order_key)))

   identity])

(defn list-defcats-dated
  "get all the defcats that need to be displayed on a page

   The optional def_date is used when we want to check if a page contains a
   deferred date category of a certain date"
  [page_ref & def_date]
  [(-> (select-distinct :cat_id :def_date :order_key)
       (from :defCatDated)
       (join :task [:= :cat_id :defcat_dated])
       (where (if-not (empty? def_date)
                [:and
                 [:= :page_ref page_ref]
                 [:= :def_date [:cast def_date :date]]]
                [:= :page_ref page_ref]))
       (order-by :def_date))

   identity])

(defn get-task-info
  "get all the info for a list of tasks"
  [task_ids]
  [(-> (select :*)
       (from :task)
       (where [:= :task_id [:any [:array task_ids :integer]]])
       (order-by :task_id))
   
   identity])

(defn list-all-tasks
  "list all task deets for a page"
  [page_ref]
  [(-> (select :*)
       (from :task)
       (where [:= :page_ref page_ref])
       (order-by :task_id))
   
   identity])

(defn tasks-defcat-named
  "list all the tasks in a deferred category"
  [defcat_ref]
  [(-> (select :*)
       (from :task)
       (where [:= :defcat_named defcat_ref]))
   
   identity])

(defn tasks-defcat-dated
  "list all the tasks in a deferred category"
  [defcat_ref]
  [(-> (select :*)
       (from :task)
       (where [:= :defcat_dated defcat_ref]))
   
   identity])

(defn create-defcat-named!
  "create a defCatNamed with a name"
  [cat_name]
  [(-> (insert-into :defCatNamed)
       (values [{:cat_name cat_name}])
       (returning :cat_id))
   
   (comp :cat_id first)])

(defn create-defcat-dated!
  "create a defCatDated"
  [def_date]
  [(-> (insert-into :defCatDated)
       (values [{:def_date [:cast def_date :date]}])
       (returning :cat_id))
   
   (comp :cat_id first)])

(defn delete-defcat-named!
  "delete a defCatNamed"
  [cat_id]
  [(-> (delete-from :defCatNamed)
       (where [:= :cat_id cat_id]))

   identity])

(defn delete-defcat-dated!
  "delete a defCatDated"
  [cat_id]
  [(-> (delete-from :defCatDated)
       (where [:= :cat_id cat_id]))

   identity])

(defn defer-task-named!
  "assign tasks to a defCatNamed and clear highlight"
  [defcat_ref task_ids]
  [(-> (update :task)
       (set {:defcat_named [:cast defcat_ref :integer]
             :highlight nil})
       (where [:= :task_id [:any [:array task_ids :integer]]]))

   identity])

(defn defer-task-dated!
  "assign tasks to a defCatDated and clear highlight"
  [defcat_ref task_ids]
  [(-> (update :task)
       (set {:defcat_dated [:cast defcat_ref :integer]
             :highlight nil})
       (where [:= :task_id [:any [:array task_ids :integer]]]))

   identity])
 
(defn undefer-task-named!
  "undefer a task from a defCatNamed"
  [task_ids]
  [(-> (update :task)
       (set {:defcat_named nil})
       (where [:= :task_id [:any [:array task_ids :integer]]]))

   identity])

(defn undefer-task-dated!
  "undefer a task from a defCatDated"
  [task_ids]
  [(-> (update :task)
       (set {:defcat_dated nil})
       (where [:= :task_id [:any [:array task_ids :integer]]]))

   identity])

(defn move-task!
  "moves one-or-more tasks to a new page"
  [task_ids newpage]
  [(-> (update :task)
       (set {:page_ref [:cast newpage :integer]})
       (where [:= :task_id [:any [:array task_ids :integer]]]))

   identity])

(defn add-habit!
  "add a habit"
  [habit_name page_ref freq_unit freq_value]
  [(-> (insert-into :habit)
       (values [{:habit_name habit_name
                 :page_ref [:cast page_ref :integer]
                 :freq_unit [:cast freq_unit :timeUnit]
                 :freq_value [:cast freq_value :integer]}])
        (returning :habit_id))

   (comp :habit_id first)])

 (defn list-due-habits
   "get all due habits, ordered by due date"
   [page_ref]
   [(-> (select :*)
        (from :habit)
        (where [:and [:= :page_ref page_ref]
                [:<= :date_scheduled :CURRENT_DATE]])
        (order-by [:order_key_habit :asc] [:date_scheduled :asc] [:habit_id :asc]))

    identity])

(defn list-upcoming-habits
  "get all upcoming habits, ordered by due date"
  [page_ref]
  [(-> (select :*)
       (from :habit)
       (where [:and [:= :page_ref page_ref]
               [:> :date_scheduled :CURRENT_DATE]])
       (order-by :date_scheduled))

   identity])

(defn get-habit-info
  "get all the info for habit(s)"
  [habit_ids]
  [(-> (select :*)
       (from :habit)
       (where [:= :habit_id [:any [:array habit_ids :integer]]])
       (order-by :date_scheduled))
   
   identity])

(defn list-all-habits
  "list all habit deets for a page"
  [page_ref]
  [(-> (select :*)
       (from :habit)
       (where [:= :page_ref page_ref])
       (order-by :date_scheduled))
   
   identity])

(defn defer-habit!
  "defer habit(s) to a specific date"
  [habit_ids defer_date & [done]]
  (let [updates {:date_scheduled [:cast defer_date :date]
                 :highlight nil}]

    [(-> (update :habit)
         (set (if done
                (assoc updates :last_done :CURRENT_DATE)
                updates))
         (where [:= :habit_id [:any [:array habit_ids :integer]]]))

     identity]))

(defn delete-habit!
  "delete one or more habits"
  [habit_ids]
  [(-> (delete-from :habit)
       (where [:= :habit_id [:any [:array habit_ids :integer]]]))

   identity])

(defn modify-habit!
  "modifies a single habit"
  [habit_id habit_name freq_value freq_unit date_scheduled]
  [(-> (update :habit)
       (set {:habit_name habit_name
             :freq_value [:cast freq_value :integer]
             :freq_unit [:cast freq_unit :timeUnit]
             :date_scheduled [:cast date_scheduled :date]})
       (where [:= :habit_id habit_id]))

   identity])

(defn modify-task!
  "modifies a single task"
  [task_id task_name]
  [(-> (update :task)
       (set {:task_name task_name})
       (where [:= :task_id task_id]))

   identity])

(defn highlight-tasks!
  "set the highlight for one or more tasks"
  [task_ids highlight]
  [(-> (update :task)
       (set {:highlight highlight})
       (where [:= :task_id [:any [:array task_ids :integer]]]))

   identity])

(defn highlight-habits!
  "set the highlight for one or more habits"
  [habit_ids highlight]
  [(-> (update :habit)
       (set {:highlight highlight})
       (where [:= :habit_id [:any [:array habit_ids :integer]]]))

   identity])

(defn undefer-highlight!
  "highlights all tasks for a given category a special color"
  [cat_id]
  [(-> (update :task)
       (set {:highlight "khaki"})
       (where [:= :defcat_dated cat_id]))

   identity])

(defn move-habit!
  "moves one-or-more habits to a new page"
  [habit_ids newpage]
  [(-> (update :habit)
       (set {:page_ref [:cast newpage :integer]})
       (where [:= :habit_id [:any [:array habit_ids :integer]]]))

   identity])

(defn distinct-task-highlights
  "select distinct highlight from tasks for a page"
  [page_ref]
  [(-> (select-distinct :highlight)
       (from :task)
       (where [:= :page_ref page_ref]))

   identity])

(defn distinct-habit-highlights
  "select distinct highlight from habits for a page"
  [page_ref]
  [(-> (select-distinct :highlight)
       (from :habit)
       (where [:= :page_ref page_ref]))

   identity])

(defn thing-today!
  "update the todo field of thing(s) to today's date"
  [table fname thing_ids]
  [(-> (update table)
       (set {:todo :CURRENT_DATE})
       (where [:= fname [:any [:array thing_ids :integer]]]))

   identity])

(def task-today! #(thing-today! :task :task_id %))
(def habit-today! #(thing-today! :habit :habit_id %))

(defn thing-tomorrow!
  "update the todo field of thing(s) to tomorrow's date"
  [table fname thing_ids]
  [(-> (update table)
       (set {:todo [:+ :CURRENT_DATE [:interval "1 day"]]})
       (where [:= fname [:any [:array thing_ids :integer]]]))

   identity])

(def task-tomorrow! #(thing-tomorrow! :task :task_id %))
(def habit-tomorrow! #(thing-tomorrow! :habit :habit_id %))

(defn thing-untodo!
  "set the todo field of thing(s) to nil"
  [table fname thing_ids]
  [(-> (update table)
       (set {:todo nil})
       (where [:= fname [:any [:array thing_ids :integer]]]))

   identity])

(def task-untodo! #(thing-untodo! :task :task_id %))
(def habit-untodo! #(thing-untodo! :habit :habit_id %))

(comment
(defn query-name
  ""
  [arg]
  [(-> ())
   
   identity])

  (sql/format [:array (range 5)])
  (sql/format [:cast [:array (range 5)] :integer])
  )
