(ns todefer.test-utils
  "we want to make it so it works in both repl, where we already have the system
  running, or when run on command-line"
  (:require [todefer.core :as core]
            [integrant.repl.state :as state]
            [ring.redis.session :refer [read-redis-session write-redis-session]]))

(def test-system (atom nil))
(def login-session (atom nil))

(defn system-state 
  []
  (or @test-system state/system))

(defn q-fn
  "we need a query function that always rolls-back"
  []
  (let [{query-fn :todefer/queries} (system-state)]
    #(query-fn % false {:rollback-only true})))

(defn debug-q-fn
  "use this one if want debug"
  []
  (let [{query-fn :todefer/queries} (system-state)]
    #(query-fn % true {:rollback-only true})))

(defn system-fixture [f]
  (reset! test-system (or (system-state) (core/start-system :test)))
  (f))

(defn logged-in-fixture [f]
  (let [{session-store :todefer/session-store} (system-state)]
    (reset! login-session
            (write-redis-session session-store nil {:user "testuser"})))
  (f))

;; the lines below mean, if starting a REPL from here, it uses devtest profile

;; Local Variables:
;; cider-clojure-cli-aliases: "devtest"
;; End:
