(ns todefer.core
  (:require [ring.adapter.jetty :as jetty]
            [next.jdbc :as jdbc]
            [clojure.edn :as edn]
            [integrant.core :as ig]
            [migratus.core :as migratus]
            [todefer.queries :as queries]
            [todefer.routes :as routes])
  (:gen-class))

(defn read-edn-file [file-path]
  (edn/read-string
   (slurp file-path)))

(defn read-classpath-resource [filename]
  (let [resource-stream (.getResourceAsStream (clojure.lang.RT/baseLoader) filename)]
    (if resource-stream
      (slurp (java.io.InputStreamReader. resource-stream "UTF-8"))
      (throw (Exception. (str "Resource not found: " filename))))))

;; config
(defmethod ig/init-key :credentials/db [_ {:keys [file-path]}]
  (read-edn-file file-path))

;; db connection
(defn connect-db
  "reads the credentials and returns a connection object"
  [config]
  (let [my-datasource (jdbc/get-datasource (conj {:dbtype "postgresql"} config))
        connection (jdbc/get-connection my-datasource)]
    connection))

(defmethod ig/init-key :db.sql/connection [_ {:keys [db-credentials]}]
  (connect-db db-credentials))

(defmethod ig/halt-key! :db.sql/connection [_ conn]
  (.close conn))

;; handler
(defmethod ig/init-key :todefer/handler [_ {:keys [q-builder]}]
  (routes/app q-builder))

;; query builder. for prod use, debug and rollback may simply be unset
(defmethod ig/init-key :todefer/queries [_ {:keys [database debug t-opts]
                                            :or
                                            {debug false
                                             t-opts {:rollback-only false}}}]
  (let [defaults {:debug debug
                  :t-opts t-opts}]
    (fn [& args]
      (apply queries/execute-query database defaults args))))

;; jetty
(defmethod ig/init-key :ring.adaptor/jetty [_ {:keys [handler opts]}]
  (jetty/run-jetty handler opts))

(defmethod ig/halt-key! :ring.adaptor/jetty [_ server]
  (.stop server))

;; migratus
(defmethod ig/init-key :migratus/config [_ {:keys [database]}]
  {:store :database
   :migration-dir "migrations/"
   :db {:connection database
        :managed-connection? true}})

;; integrant
(defn system-config
  "app or db"
  [system-type]
  (ig/read-string (read-classpath-resource (case system-type
                           :app "system.edn"
                           :db  "db-system.edn"))))

(defn halt-system [system]
  (ig/halt! system))

(defn print-usage []
  (let [infos ["Usage:"
               ""
               "Please provide a single argument:"
               "- run-app: this will run the application"
               "- migrate: this will setup or update the database tables"
               "- rollback: revert the latest db migration"]]
    (println (reduce #(str %1 "\n" %2) infos))))

(defn start-system
  "app or db"
  [system-type]
  (let [system (ig/init (system-config system-type))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(halt-system system)))
    ;; return the system to the caller
    system))

(defn -main [& args]
  (case (first args)
    "run-app"  (let [system (start-system :app)])
    "migrate"  (let [system (start-system :db)]
                (migratus/migrate (:migratus/config system)))
    "rollback" (let [system (start-system :db)]
                 (migratus/rollback (:migratus/config system)))
    (print-usage)))

(comment

  )
