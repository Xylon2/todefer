(ns todefer.hiccup-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [todefer.hiccup :refer :all]
            [expectations.clojure.test
             :refer [defexpect expect expecting
                     approximately between between' functionally
                     side-effects from-each]]
            [todefer.test-utils :as tu]
            [clojure.string :as string]))

(deftest test-annotate-positions
  (expect '(["a" {:first true, :last false}]
            ["a" {:first false, :last false}]
            ["c" {:first false, :last true}])
          (annotate-positions ["a" "a" "c"]))

  (expect '(["a" {:first true, :last true}])
          (annotate-positions ["a"])))
