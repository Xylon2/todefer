(ns todefer.test-utils
  "we want to make it so it works in both repl, where we already have the system
  running, or when run on command-line"
  (:require [todefer.core :as core]
            [integrant.repl.state :as state]))

(def test-system (atom nil))

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

(defn system-fixture
  []
  (fn [f]
    (reset! test-system (or (system-state) (core/start-system :test)))
    (f)
    (core/halt-system @test-system)))

;; the lines below mean, if starting a REPL from here, it uses devtest profile

;; Local Variables:
;; cider-clojure-cli-aliases: "devtest"
;; End:
