(ns todefer.routes-test
  (:require [clojure.test :refer :all]
            [todefer.test-utils :as tu]
            [clojure.string :as string]))

(use-fixtures :once (tu/system-fixture))

(defn handler [] (:todefer/handler (tu/system-state)))

(deftest test-login-page
  (testing "testing login page"
    (let [testmap ((handler) {:uri "/login"
                              :query-string nil
                              :request-method :get
                              :headers {}
                              })]

      (is (= 200 (:status testmap)))
      (is (string/includes? (:body testmap) "Login")))))
