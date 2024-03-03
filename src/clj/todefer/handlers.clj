(ns todefer.handlers
  (:require [todefer.queries :as q]
            [todefer.hiccup :as ph]
            [hiccup2.core :as h]
            [java-time :as jt]))

(def days {"Mon" "Monday"
           "Tue" "Tuesday"
           "Wed" "Wednesday"
           "Thu" "Thursday"
           "Fri" "Friday"
           "Sat" "Saturday"
           "Sun" "Sunday"})

(defn prettify-due
  "we take a map, and we add a :prettydue based off of the key in datekey"
  [taskdata datekey]
  (let [{date_scheduled datekey} taskdata
        now (jt/local-date)]
    (conj taskdata {:prettydue (cond
                                  (= date_scheduled now)
                                    "today"
                                  (= date_scheduled (jt/plus now (jt/days 1)))
                                    "tomorrow"
                                  (= date_scheduled (jt/minus now (jt/days 1)))
                                    "yesturday"
                                  (jt/before? (jt/minus now (jt/days 6)) date_scheduled (jt/plus now (jt/days 6)))
                                    (days (jt/format "E" date_scheduled))
                                  :else
                                    date_scheduled)})))

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
  "given a map of cat info, add a new key called :tasks, with all the task info"
  [exec-query defcat]
  (conj defcat {:tasks (exec-query (q/tasks-defcat-named {:defcat_ref (defcat :cat_id)}))}))

(defn add-tasks-dated
  "given a map of cat info, add a new key called :tasks, with all the task info"
  [exec-query defcat]
  (conj defcat {:tasks (exec-query (q/tasks-defcat-dated {:defcat_ref (defcat :cat_id)}))}))

(defn not-found-handler
  "display not found page"
  [& _]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body (ph/render-message "Page Not Found")})

(defn home-handler
  "display the list of questions"
  [{exec-query :q-builder}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (ph/render-message (exec-query (q/list-pages)) "Hello World!")})

(defn display-page
  "displays a task, habit or agenda page"
  [{exec-query :q-builder
    {{page-name :page-name} :path} :parameters}]

  ;; check the page_name is legit and find out what type of page it is
  (let [page-list (exec-query (q/list-pages))
        {page-type :page_type
         page-id :page_id} (exec-query (q/get-page page-name))]

    (case page-type
      "task"
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (let [due-tasks (exec-query (q/list-due-tasks page-id))
                   defcatsnamed (map #(add-tasks-named exec-query %)
                                     (exec-query (q/list-defcats-named page-id)))
                   defcatsdated (map #(add-tasks-dated exec-query %)
                                     (map #(prettify-due % :def_date)
                                          (list-defcats-dated-undefer exec-query page-id)))]
               (ph/tasks-page
                page-list
                page-name
                page-id
                due-tasks
                defcatsnamed
                defcatsdated))}
      ;; "habit"
      ;; {:status 200
      ;;  :headers {"Content-Type" "text/html"}
      ;;  :body (ph/habits-page page-list page-name page-id)}
      ;; "agenda"
      ;; {:status 200
      ;;  :headers {"Content-Type" "text/html"}
      ;;  :body (ph/agenda-page page-list page-name page-id)}
      ;; nil
      ;; {:status 404
      ;;  :headers {"Content-Type" "text/html"}
      ;;  :body (ph/render-message "Page Not Found")}
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
