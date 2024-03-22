(ns todefer.settings-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [hiccup2.core :as h]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(def one-update?   #(= 1 (num-updated %)))
(def some-updated? #(< 0 (num-updated %)))

(defn settings-200 [page-list f-token]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (ph/settings-page page-list f-token)})

(defn show-500
  [message]
  {:status 500
   :headers {"Content-Type" "text/html"}
   :body message})

(defn settings-handler
  "show the settings page"
  [{exec-query :q-builder
    f-token :anti-forgery-token}]
  (let [page-list (exec-query (q/list-pages))]
    (settings-200 page-list f-token)))

(defn add-page-handler
  "add task"
  [{exec-query :q-builder
    f-token :anti-forgery-token
    {{:keys [new_pagename new_pagetype]} :form} :parameters}]
  (let [qresult (one-update? (exec-query (q/create-page! new_pagename new_pagetype)))
        page-list (exec-query (q/list-pages))]
    (if qresult
      (settings-200 page-list f-token)
      (show-500 ":o"))))
