(ns todefer.handlers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [hiccup2.core :as h]
            [java-time :as jt]
            [clojure.pprint :refer [pprint]]))

(def days {"Mon" "Monday"
           "Tue" "Tuesday"
           "Wed" "Wednesday"
           "Thu" "Thursday"
           "Fri" "Friday"
           "Sat" "Saturday"
           "Sun" "Sunday"})

(defn prettify-due
  "we take a vector of maps, and we add a :prettydue to each map based off of the key in datekey"
  [taskdata datekey]
  (mapv
   (fn [task]
     (let [{date_scheduled datekey} task
           date_scheduled' (.toLocalDate date_scheduled)
           now (jt/local-date)]
       (assoc task :prettydue
              (cond
                (= date_scheduled' now)
                "today"
                (= date_scheduled' (jt/plus now (jt/days 1)))
                "tomorrow"
                (= date_scheduled' (jt/minus now (jt/days 1)))
                "yesterday"
                (jt/before? (jt/minus now (jt/days 6)) date_scheduled' (jt/plus now (jt/days 6)))
                (days (jt/format "E" date_scheduled'))
                :else
                date_scheduled'))))
   taskdata))

(defn undefer-due
  "to be used by reduce to either return a category back or undefer it"
  [exec-query buildme defcat]
  (let [{cat_id :cat_id
         catdate :def_date} defcat
        catdate' (.toLocalDate catdate) ;; it was in java.sql.Date format
        datenow (jt/local-date)]
    (if (jt/not-after? catdate' datenow)
      (do (exec-query (q/undefer-highlight! cat_id))
          (exec-query (q/delete-defcat-dated! cat_id))
          buildme)
      (conj buildme defcat))))

(defn list-defcats-dated-undefer
  "basically a wrapper for q/list-defcats-dated that undefers categories as appropriate"
  [exec-query page_id]
  (let [defcats_dated (exec-query (q/list-defcats-dated page_id))]
    (reduce #(undefer-due exec-query %1 %2) [] defcats_dated)))

(defn add-tasks-named
  "given a vector of cat info maps, add a new key called :tasks to each map, with all the task info"
  [defcats exec-query]
  (mapv (fn [defcat]
          (conj defcat {:tasks (exec-query (q/tasks-defcat-named (defcat :cat_id)))}))
        defcats))

(defn add-tasks-dated
  "given a vector of cat info maps, add a new key called :tasks to each map, with all the task info"
  [defcats exec-query]
  (mapv (fn [defcat]
          (conj defcat {:tasks (exec-query (q/tasks-defcat-dated (defcat :cat_id)))}))
        defcats))

(defn not-found-handler
  "display not found page"
  [{exec-query :q-builder
    session :session}]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (ph/render-message "Page Not Found" (when (contains? session :user)
                                               (exec-query (q/list-pages))))})

(defn home-handler
  "display the list of questions"
  [{exec-query :q-builder}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (ph/render-message "Select a page" (exec-query (q/list-pages)))})

(defn mapdebugger
  [x annotation]
  (println annotation)
  (pprint x)
  x)

(defn display-page
  "displays a task, habit or agenda page"
  [{exec-query :q-builder
    {{page-name :page-name} :path} :parameters :as request
    f-token :anti-forgery-token}]

  ;; check the page_name is legit and find out what type of page it is
  (let [page-list (exec-query (q/list-pages))
        page-list' (map #(if (= (:page_name %) page-name)
                           (assoc % :selected true)
                           %) page-list)
        {page-type :page_type
         page-id :page_id} (exec-query (q/get-page page-name))]

    (case page-type
      "task"
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (let [due-tasks (exec-query (q/list-due-tasks page-id))
                   defcatsnamed (-> (exec-query (q/list-defcats-named page-id))
                                    (add-tasks-named exec-query))
                   defcatsdated (-> (list-defcats-dated-undefer exec-query page-id)
                                    (prettify-due :def_date)
                                    (add-tasks-dated exec-query))]
               (ph/tasks-page
                page-list'
                page-name
                page-id
                due-tasks
                defcatsnamed
                defcatsdated
                f-token))}
      "habit"
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (let [duehabits (-> (exec-query (q/list-due-habits page-id))
                                 (prettify-due :date_scheduled))
                   upcominghabits (-> (exec-query (q/list-upcoming-habits page-id))
                                      (prettify-due :date_scheduled))]
               (ph/habits-page
                page-list'
                page-name
                page-id
                duehabits
                upcominghabits
                f-token))}
      "agenda"
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (let []
               (ph/agenda-page
                page-list'
                page-name
                page-id))}
      (not-found-handler request)
      )))

(defn login-handler
  "show the login prompt. the parameter variable holds the url the user was trying
  to access before being sent here"
  [{{redirect "redirect" :or {redirect "/"}} :query-params
    f-token :anti-forgery-token}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (ph/render-login redirect f-token)})

(defn login-post-handler
  "check the credentials. if success, redirect to the redirect url. else, display
  the login page again"
  [{exec-query :q-builder
    {redirect "redirect" :or {redirect "/"}} :query-params
    session :session
    {{:keys [username password]} :form} :parameters
    f-token :anti-forgery-token}]
  (if (exec-query (q/authenticate-user username password))
    ;; login success
    {:status 303
     :headers {"Location" redirect}
     :body ""
     :session (assoc session :user username)}

    ;; login failure
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (ph/render-login redirect f-token "Login failed")}))

(defn logout-handler
  "log out and redirect to /"
  [{session :session}]

  {:status 303
     :headers {"Location" "/"}
     :body ""
     :session (dissoc session :user)})
