(ns todefer.routes-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [todefer.routes :refer :all]
            [expectations.clojure.test
             :refer [defexpect expect expecting
                     approximately between between' functionally
                     side-effects from-each]]
            [todefer.test-utils :as tu]
            [clojure.string :as string]))

(use-fixtures :once tu/system-fixture)
(use-fixtures :once tu/logged-in-fixture)

(defn handler [] (:todefer/handler (tu/system-state)))

(deftest test-login-page
  (let [response-map ((handler) {:uri "/login"
                            :query-string nil
                            :request-method :get
                            :headers {}
                            })]

    (expect 200 (:status response-map))
    (expect #(string/includes? (:body response-map) %) "Please login")))

;; test the three different code paths which can result in a 404.
;; - the router can trigger a 404
;; - the static files serving middleware (ring's wrap-resource) can trigger a 404
;; - the page names come from database so the page handler generates it's own 404s

(deftest test-not-found-unauthenticated
  (expect #(#{404 303} %)
          (from-each [url ["/bliblablib"
                           "/public/bliblablib"
                           "/page/bliblablib"]]
            (:status
             ((handler) {:uri url
                         :query-string nil
                         :request-method :get
                         :headers {}}))))

  ;; not exposing page names in navbar
  (expect (complement #(string/includes? % "<a href=\"/page/"))
          (from-each [url ["/bliblablib"
                           "/public/bliblablib"
                           "/page/bliblablib"]]
            (:body
             ((handler) {:uri url
                         :query-string nil
                         :request-method :get
                         :headers {}})))))

(deftest test-not-found-authenticated
  (expect 404
          (from-each [url ["/bliblablib"
                           "/public/bliblablib"
                           "/page/bliblablib"]]
            (:status
             ((handler) {:uri url
                         :query-string nil
                         :request-method :get
                         :headers {}
                         :cookies {"ring-session"
                                   {:value @tu/login-session}}}))))

  ;; page names are included in navbar
  (expect #(string/includes? % "<a href=\"/page/")
          (from-each [url ["/bliblablib"
                           "/public/bliblablib"
                           "/page/bliblablib"]]
            (:body
             ((handler) {:uri url
                         :query-string nil
                         :request-method :get
                         :headers {}
                         :cookies {"ring-session"
                                   {:value @tu/login-session}}})))))

;; the lines below mean, if starting a REPL from here, it uses devtest profile

;; Local Variables:
;; cider-clojure-cli-aliases: "devtest"
;; End:
