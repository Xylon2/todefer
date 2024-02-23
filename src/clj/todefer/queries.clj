(ns todefer.queries
  ;; exclude these core clojure functions
  (:refer-clojure :exclude [distinct filter for group-by into partition-by set update])
  
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer :all]  ;; shadows core functions
            [clojure.core :as c]  ;; so we can still access core functions
            [next.jdbc :as jdbc]
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
   (let [formatted-query (sql/format query)]
     (when debug (println (str "formatted-query: " formatted-query)))
     (jdbc/with-transaction [t-conn conn t-opts]
       (processor (jdbc/execute! t-conn formatted-query))
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

   (fn [[{hashed :users/password}]]
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

(defn add-task!
  "add a task"
  ([task_name page_ref]
   (add-task! task_name page_ref nil))
  ([task_name page_ref highlight]
   [(-> (insert-into :task)
        (values [{:task_name task_name
                  :page_ref [:cast page_ref :integer]
                  :highlight highlight}]))

    identity]))

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

(defn get-page
  "gets the info for a single page"
  [page_name]
  [(-> (select :*)
       (from :appPage)
       (where [:= :page_name page_name]))

   identity])

(defn get-default-page
  "gets the name of the page with the lowest index"
  []
  [(-> (select [:page_name])
       (from :appPage)
       (order-by [:order_key])
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
       (order-by [:sort_id :asc] [:task_id :asc]))

   identity])

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
         (order-by [:order_key])))

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
       (order-by [:def_date]))

   identity])

(defn get-task-info
  "get all the info for a list of tasks"
  [task_ids]
  [(-> (select :*)
       (from :task)
       (where [:= :task_id [:any [:array task_ids :integer]]])
       (order-by [:task_id]))
   
   identity])

(comment
(defn query-name
  ""
  [arg]
  [(-> ())
   
   identity])

  (sql/format [:array (range 5)])
  (sql/format [:cast [:array (range 5)] :integer])
  )
