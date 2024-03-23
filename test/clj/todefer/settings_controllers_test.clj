(ns todefer.settings-controllers-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [todefer.settings-controllers :refer :all]
            [expectations.clojure.test
             :refer [defexpect expect expecting
                     approximately between between' functionally
                     side-effects from-each]]
            [todefer.test-utils :as tu]
            [clojure.string :as string]))

(deftest test-reorderfunc
  (expect [10 1 13 14]
          (:newvec
           (reduce reorderfunc {:swap-id 1 :state "returning" :newvec []}
                   [1 10 13 14]))
          )
  )
