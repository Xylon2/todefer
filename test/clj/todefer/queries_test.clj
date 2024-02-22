(ns todefer.queries-test
  (:require [clojure.test :refer :all]
            [todefer.test-utils :as tu]
            [todefer.queries :refer :all]))

(use-fixtures :once (tu/system-fixture))

;; the tests can use factory function tu/q-fn to run their queries

(defn one-update? [x]
  (= 1 (:next.jdbc/update-count (first x))))

(deftest test-create-user
  (testing "creating a user"
   (is (one-update? ((tu/q-fn)
                    (create-user "username" "password"))))))

(deftest test-authenticate-user
  (testing "authenticate a user"
    (is (= true
           ((tu/q-fn) (authenticate-user "testuser" "testpass"))))
    (is (= false
           ((tu/q-fn) (authenticate-user "testuser" "wrongpass"))))))

(deftest test-create-page!
  (testing "creating a page"
   (is (one-update? ((tu/q-fn)
                    (create-page! "cool" "task"))))
   (is (one-update? ((tu/q-fn)
                    (create-page! "cool" "habit"))))))

;; .n.b "is" macro doesn't work inside a rich comment

;; to create test user, we must bypass our query-function that always rolls back:
;; - start repl from this buffer (so it uses "devtest" profile)
;; - (go)
;; - switch repl namespace to this buffer, and evaluate it
;; - run ((:todefer/queries (tu/system-state)) (create-user "testuser" "testpass"))

;; the lines below mean, if starting a REPL from here, it uses devtest profile

;; Local Variables:
;; cider-clojure-cli-aliases: "devtest"
;; End:
