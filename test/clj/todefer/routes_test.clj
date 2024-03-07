(ns todefer.routes-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [expectations.clojure.test
             :refer [defexpect expect expecting
                     approximately between between' functionally
                     side-effects]]
            [todefer.test-utils :as tu]
            [todefer.routes :refer [session-atom]]
            [clojure.string :as string]))

(use-fixtures :once (tu/system-fixture))

(defn handler [] (:todefer/handler (tu/system-state)))

(deftest test-login-page
  (let [response-map ((handler) {:uri "/login"
                            :query-string nil
                            :request-method :get
                            :headers {}
                            })]

    (expect 200 (:status response-map))
    (expect (string/includes? (:body response-map) "Please login"))))

(deftest test-not-found-global
  (expecting "unauthenticated"
    (let [response-map ((handler) {:uri "/bliblablib"
                                   :query-string nil
                                   :request-method :get
                                   :headers {}
                                   })]
      (expect 404 (:status response-map))
      ;; not exposing page names in navbar
      (expect (not (string/includes? (:body response-map) "<a href=\"/page/")))
      ))
  
  (expecting "authenticated"
    (swap! session-atom assoc "testsession3" {:user "foo"})
    (let [response-map ((handler) {:uri "/bliblablib"
                                   :query-string nil
                                   :request-method :get
                                   :headers {}
                                   :cookies {"ring-session"
                                             {:value "testsession3"}}
                                   })]
      ;; log ourselfes in

      (expect 404 (:status response-map))
      ;; page names are included in navbar
      (expect (string/includes? (:body response-map) "<a href=\"/page/"))
      ))
  )

(comment
 (deftest test-not-found-static
   (expecting "unauthenticated"
     (let [response-map ((handler) {:uri "/"
                                    :query-string nil
                                    :request-method :get
                                    :headers {}
                                    })]))
   )

 (deftest test-not-found-page
   (expecting "unauthenticated"
     (let [response-map ((handler) {:uri "/"
                                    :query-string nil
                                    :request-method :get
                                    :headers {}
                                    })]))
   )
 )
