(ns todefer.queries
  ;; exclude these core clojure functions
  (:refer-clojure :exclude [distinct filter for group-by into partition-by set update])
  
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer :all]  ;; shadows core functions
            [clojure.core :as c]  ;; so we can still access core functions
            [next.jdbc :as jdbc]
            [buddy.hashers :as hashers]))

;; the core namespace will closure over this with the connection
(defn execute-query
  "takes a connection, a honeysql query, and a post-processor function"
  [conn [query processor] & [debug]]
  (let [formatted-query (sql/format query)]
    (when debug (println (str "formatted-query: " formatted-query)))
    (-> (jdbc/execute! conn formatted-query)
        processor)))

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
  [(-> (select [:*])
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

   identity]
  )
