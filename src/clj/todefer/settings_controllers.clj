(ns todefer.settings-controllers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [hiccup2.core :as h]))

(defn num-updated [x]
  (:next.jdbc/update-count (first x)))

(def one-update?   #(= 1 (num-updated %)))
(def some-updated? #(< 0 (num-updated %)))

(defn add-linked-pages
  "for todo pages, we add a list of the linked pages"
  [page-list exec-query]
  (map (fn [page]
         (if (= (:page_type page) "todo")
           (let [linked-page-ids (map :page_id (exec-query (q/list-linked-pages (:page_id page))))]
             (assoc page :linked-pages linked-page-ids))
           page)) page-list))

(defn settings-200 [exec-query f-token]
  (let [page-list (exec-query (q/list-pages))
        page-list' (add-linked-pages page-list exec-query)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (ph/settings-page page-list' f-token)}))

(defn show-500
  [message]
  {:status 500
   :headers {"Content-Type" "text/html"}
   :body message})

(defn settings-handler
  "show the settings page"
  [{exec-query :q-builder
    f-token :anti-forgery-token}]
  (settings-200 exec-query f-token))

(defn wrap-settings-page
  "this is a middleware. wrap it around the action handlers."
  [handler]
  (fn [{exec-query :q-builder
        f-token :anti-forgery-token :as req}]
    (if (handler req)
      (settings-200 exec-query f-token)
      (show-500 ":o"))))

(defn add-page-handler
  "add page"
  [{exec-query :q-builder
    {{:keys [new_pagename new_pagetype]} :form} :parameters}]
  (one-update? (exec-query (q/create-page! new_pagename new_pagetype))))

(defn delete-page-handler
  "delete page"
  [{exec-query :q-builder
    {{:keys [page_id]} :form} :parameters}]
  (one-update? (exec-query (q/delete-page! page_id))))

(defn reorderfunc
  "reducing function. when we find the page we want, we swap it with the next one

  first param is a map of:
   - swap-id - this is to remember the id of the page we want to swap
   - state   - essentially have we found the one to swap yet
   - newvec  - the vector we're building"
  [{:keys [swap-id state newvec]} page-id]
  (case state
    "returning" (if (not= page-id swap-id)
                  {:swap-id swap-id :state "returning" :newvec (conj newvec page-id)}
                  {:swap-id swap-id :state "swapping"  :newvec newvec})
    "swapping"    {:swap-id swap-id :state "returning" :newvec (conj newvec page-id swap-id)}))

(defn apply-order
  "given the list of page ids in a vector, generate them new order ids and apply"
  [exec-query idlist]
  (doseq [[order-key page_id] (map-indexed vector idlist)]
    (exec-query (q/reorder-page! page_id order-key)))
  true)

(defn page-down-handler
  "page up"
  [{exec-query :q-builder
    {{:keys [page_id]} :form} :parameters}]
  (let [page-id-list (mapv :page_id (exec-query (q/list-pages)))
        {reordered_list :newvec} (reduce reorderfunc {:swap-id page_id :state "returning" :newvec []} page-id-list)]

    (apply-order exec-query reordered_list)))

(defn page-up-handler
  "page up"
  [{exec-query :q-builder
    {{:keys [page_id]} :form} :parameters}]
  (let [page-id-list (rseq (mapv :page_id (exec-query (q/list-pages))))
        {reordered_list :newvec} (reduce reorderfunc {:swap-id page_id :state "returning" :newvec []} page-id-list)]

    (apply-order exec-query (rseq reordered_list))))

(defn update-todo-handler
  [{exec-query :q-builder
    {{:keys [page_id linkedpage]} :form} :parameters}]

  ;; our list contains only positives so we have to nuke the old values before
  ;; adding them all back again
  (exec-query (q/nuke-linked-pages! page_id))
  (doseq [lpg linkedpage]
    (exec-query (q/update-linked-pages! page_id lpg)))
  true)
