(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [todefer.core :as pc]
            [clojure.repl :refer [doc]]
            [migratus.core :as migratus]
            [todefer.queries :as q]
            [todefer.test-utils :as tu]))

(integrant.repl/set-prep! #(ig/prep (pc/system-config :test)))

;; use like this: (query (q/myquery "foo" "bar"))
(defn query [& args]
  (apply tu/q-fn args))
