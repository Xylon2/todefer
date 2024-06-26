(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'todefer)
(def version (format "1.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
;; (def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))
(def uber-file (format "target/%s.jar" (name lib)))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources" "env/prod/resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :ns-compile '[todefer.core]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'todefer.core}))
