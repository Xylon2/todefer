(ns todefer.queries-test
  (:require [clojure.test :refer :all]
            [todefer.test-utils :as tu]
            [todefer.queries :refer :all]))

(use-fixtures :once (tu/system-fixture))

;; the tests can use factory function tu/q-fn to run their queries

(deftest test-create-user
  (is (= 1 (-> ((tu/q-fn) (create-user "username" "password"))
               first
               :next.jdbc/update-count))))

;; .n.b "is" macro doesn't work inside a rich comment
