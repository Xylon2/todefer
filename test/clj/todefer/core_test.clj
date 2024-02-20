(ns todefer.core-test
  (:require [clojure.test :refer :all]
            [todefer.core :refer :all]))

(deftest test-read-edn-file
  (is (= "John Doe" (:name (read-edn-file "test_assets/example.edn")))))

;; .n.b "is" macro doesn't work inside a rich comment
