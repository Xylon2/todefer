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

(defn add-linked-pages
  "for agenda pages, we add a list of the linked pages"
  [page-list exec-query]
  (map (fn [page]
         (if (= (:page_type page) "agenda")
           (assoc page :linked-pages (exec-query (q/list-linked-pages (:page_id page))))
           page)) page-list))

(defn settings-handler
  "show the settings page"
  [{exec-query :q-builder
    f-token :anti-forgery-token}]
  (let [page-list (exec-query (q/list-pages))
        page-list' (add-linked-pages page-list exec-query)]
    (settings-200 page-list' f-token)))

(defn add-page-handler
  "add page"
  [{exec-query :q-builder
    f-token :anti-forgery-token
    {{:keys [new_pagename new_pagetype]} :form} :parameters}]
  (let [qresult (one-update? (exec-query (q/create-page! new_pagename new_pagetype)))
        page-list (exec-query (q/list-pages))]
    (if qresult
      (settings-200 page-list f-token)
      (show-500 ":o"))))

(defn delete-page-handler
  "delete page"
  [{exec-query :q-builder
    f-token :anti-forgery-token
    {{:keys [page_id]} :form} :parameters}]
  (let [qresult (one-update? (exec-query (q/delete-page! page_id)))
        page-list (exec-query (q/list-pages))]
    (if qresult
      (settings-200 page-list f-token)
      (show-500 ":o"))))

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
    (exec-query (q/reorder-page! page_id order-key))))

(defn page-down-handler
  "page up"
  [{exec-query :q-builder
    f-token :anti-forgery-token
    {{:keys [page_id]} :form} :parameters}]
  (let [page-id-list (mapv :page_id (exec-query (q/list-pages)))
        {reordered_list :newvec} (reduce reorderfunc {:swap-id page_id :state "returning" :newvec []} page-id-list)]

    (apply-order exec-query reordered_list)
    (let [page-list (exec-query (q/list-pages))]
      (settings-200 page-list f-token))))

(defn page-up-handler
  "page up"
  [{exec-query :q-builder
    f-token :anti-forgery-token
    {{:keys [page_id]} :form} :parameters}]
  (let [page-id-list (rseq (mapv :page_id (exec-query (q/list-pages))))
        {reordered_list :newvec} (reduce reorderfunc {:swap-id page_id :state "returning" :newvec []} page-id-list)]

    (apply-order exec-query (rseq reordered_list))
    (let [page-list (exec-query (q/list-pages))]
      (settings-200 page-list f-token))))
