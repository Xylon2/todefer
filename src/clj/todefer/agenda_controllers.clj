(ns todefer.agenda-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [todefer.handlers :as hl]
            [hiccup2.core :as h]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(def one-update?   #(= 1 (num-updated %)))
(def some-updated? #(< 0 (num-updated %)))

(defn show-agenda-200
  [exec-query page-id]
  (let [{:keys [todo-today todo-tomorrow]}
        (hl/assemble-agenda-page-info exec-query page-id)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (ph/render-agenda page-id todo-today todo-tomorrow)
               h/html
               str)}))

(defn show-500
  [message]
  {:status 500
   :headers {"Content-Type" "text/html"}
   :body message})

(defn get-page-id
  "given the page name, get page id"
  [exec-query page-name]
  (:page_id (exec-query (q/get-page page-name))))

;; done-delete-handler :thing_id
;; todo-thing-handler :thing_id :action

(defn done-delete-handler
  []
  true)

(defn todo-thing-handler
  []
  true)
