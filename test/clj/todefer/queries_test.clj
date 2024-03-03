(ns todefer.queries-test
  (:require [clojure.test :refer :all]
            [todefer.test-utils :as tu]
            [todefer.queries :refer :all]
            [java-time :as jt]))

(use-fixtures :once (tu/system-fixture))

;; the tests can use factory function tu/q-fn to run their queries

(defn num-updated [n x]
  (= n (:next.jdbc/update-count (first x))))

(def one-update? #(num-updated 1 %))
(def two-updates? #(num-updated 2 %))

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

(deftest test-delete-page!
  (testing "delete a page"
    (is (one-update? ((tu/q-fn)
                      (create-page! "cool" "task"))))))

(deftest test-reorder-page!
  (testing "reorder a page"
    (is (one-update? ((tu/q-fn)
                      (reorder-page! 1 2))))))

(deftest test-add-task!
  (testing "add a task"
    (is (one-update? ((tu/q-fn)
                      (add-task! "name of task to be added" 1))))))

(deftest test-delete-task!
  (testing "delete a task"
    (is (one-update? ((tu/q-fn)
                      (delete-task! [1812]))))))

(deftest test-list-pages
  (testing "listing pages"
    (is (= ["Lorem ipsum" "Consectetur adipiscing" "Dolor sit amet" "Another Page"]
           (->> ((tu/q-fn)
                 (list-pages))
                (mapv :page_name))))))

(deftest test-get-page
  (testing "get page info"
    (is (= {:page_id 1, :page_name "Lorem ipsum", :order_key 0, :page_type "task"}
           ((tu/q-fn)
            (get-page "Lorem ipsum"))))))

(deftest test-get-default-page
  (testing "get default page"
    (is (= [{:page_name "Lorem ipsum"}]
           ((tu/q-fn)
            (get-default-page))))))

(deftest test-list-due-tasks
  (testing "list due tasks"
    (is (= 19 (-> ((tu/q-fn)
                      (list-due-tasks 1))
                  count)))))

(deftest test-list-defcats-named
  (testing
      "get all the defcats that need to be displayed on a page

       The optional cat_name is used when we want to check if a page contains a
       category of a certain name."
    (is (= 2 (-> ((tu/q-fn)
                     (list-defcats-named 1))
                 count)))
    (is (= 1 (-> ((tu/q-fn)
                     (list-defcats-named 1 "Minim veniam"))
                 count)))))

(deftest test-list-defcats-dated
  (testing
      "get all the defcats that need to be displayed on a page

       The optional def_date is used when we want to check if a page contains a
       deferred date category of a certain date"
    (is (= 5 (-> ((tu/q-fn)
                     (list-defcats-dated 1))
                 count)))
    (is (= 1 (-> ((tu/q-fn)
                     (list-defcats-dated 1 "2124-03-19"))
                 count)))))

(deftest test-get-task-info
  (testing "get all the info for a list of tasks"
    (is (= ["Adipiscing elit" "Exercitation ullamco" "Labore et"]
           (->> ((tu/q-fn)
                 (get-task-info [1806 1739 1810]))
                (mapv :task_name))))))

(deftest test-list-all-tasks
  (testing "list all task deets for a page"
    (is (= 28 (-> ((tu/q-fn)
                   (list-all-tasks 1))
                  count)))))

(deftest test-tasks-defcat-named
  (testing "list all the tasks in a deferred category"
    (is (= 2 (-> ((tu/q-fn)
                  (tasks-defcat-named 13))
                 count)))))

(deftest test-create-defcat-named!
  (testing "create a defCatNamed with a name"
    (is (integer? (-> ((tu/q-fn)
                       (create-defcat-named! "foobar"))
                      first
                      :cat_id)))))

(deftest test-create-defcat-dated!
  (testing "create a defCatDated"
    (is (integer? (-> ((tu/q-fn)
                       (create-defcat-dated! "2051-06-14"))
                      first
                      :cat_id)))))

(deftest test-delete-defcat-named!
  (testing "delete a defCatNamed"
    (is (one-update? ((tu/q-fn)
                      (delete-defcat-named! 5))))))

(deftest test-delete-defcat-dated!
  (testing "delete a defCatDated"
    (is (one-update? ((tu/q-fn)
                      (delete-defcat-dated! 5))))))

(deftest test-defer-task-named!
  (testing "assign tasks to a defCatNamed and clear highlight"
    (is (one-update? ((tu/q-fn)
                      (defer-task-named! 1 [1812]))))))

(deftest test-defer-task-dated!
  (testing "assign tasks to a defCatDated and clear highlight"
    (is (one-update? ((tu/q-fn)
                      (defer-task-dated! 65 [1812]))))))
 
(deftest test-undefer-task-named!
  (testing "undefer a task from a defCatNamed"
    (is (one-update? ((tu/q-fn)
                      (undefer-task-named! [1526]))))))

(deftest test-undefer-task-dated!
  (testing "undefer a task from a defCatDated"
    (is (one-update? ((tu/q-fn)
                      (undefer-task-dated! [1719]))))))

(deftest test-move-task!
  (testing "move tasks to a new page"
    (is (two-updates? ((tu/q-fn)
                      (move-task! 14 [1799 1791]))))))

(deftest test-add-habit!
  (testing "add a habit"
    (is (one-update? ((tu/q-fn)
                      (add-habit! "habit name" 10 "days" 4))))))

(deftest test-list-due-habits
  (testing "get all due habits, ordered by due date"
    (is (= ["Incididunt ut" "Labore et" "Nisi ut"]
         (->> ((tu/q-fn)
               (list-due-habits 13))
              (mapv :habit_name)
              (take 3))))))

(deftest test-list-upcoming-habits
  (testing "get all upcoming habits, ordered by due date"
    (is (= ["Aliquip ex" "Ea commodo" "Ullamco laboris"]
         (->> ((tu/q-fn)
               (list-upcoming-habits 13))
              (mapv :habit_name)
              (take 3))))))

(deftest test-get-habit-info
  (testing "get all the info for a list of habits"
    (is (= ["Lorem ipsum" "Dolor sit" "Amet consectetur"]
           (->> ((tu/q-fn)
                 (get-habit-info [2 39 45]))
                (mapv :habit_name))))))

(deftest test-list-all-habits
  (testing "list all habit deets for a page"
    (is (= 18 (-> ((tu/q-fn)
                  (list-all-habits 10))
                 count)))))

(deftest test-defer-habit!
  (testing "defer habit(s) to a specific date"
    (is (two-updates? ((tu/q-fn)
            (defer-habit! [25 36] (jt/plus (jt/local-date) (jt/days 10))))))))

(deftest test-delete-habit!
  (testing "delete one or more habits"
    (is (two-updates? ((tu/q-fn)
                       (delete-habit! [42 35]))))))

(deftest test-modify-habit!
  (testing "modifies a single habit"
    (is (one-update? ((tu/q-fn)
                      (modify-habit! 42 "Updated Habit" 7 "days" "2023-04-01"))))))

(deftest test-modify-task!
  (testing "modifies a single task"
    (is (one-update? ((tu/q-fn)
                      (modify-task! 1811 "Updated Task"))))))

(deftest test-highlight-tasks!
  (testing "set the highlight for one or more tasks"
    (is (two-updates? ((tu/q-fn)
                      (highlight-tasks! [1788 1811] "highlighted"))))))

(deftest test-highlight-habits!
  (testing "set the highlight for one or more habits"
    (is (two-updates? ((tu/q-fn)
                       (highlight-habits! [41 10] "highlighted"))))))

(deftest test-undefer-highlight!
  (testing "highlight all tasks for a given category a special color"
    (is (one-update? ((tu/q-fn)
                      (undefer-highlight! 96))))))

(deftest test-move-habit!
  (testing "move one-or-more habits to a new page"
    (is (two-updates? ((tu/q-fn)
                       (move-habit! [2 44] 13))))))
;; todo
(deftest test-distinct-task-highlights
  (testing "select distinct highlight from tasks for a page"
    (is (= '(nil "lightblue")
           (->> ((tu/q-fn)
                 (distinct-task-highlights 1))
                (mapv :highlight)
                (sort))))))

(deftest test-distinct-habit-highlights
  (testing "select distinct highlight from habits for a page"
    (is (= '(nil "lightblue" "red")
           (->> ((tu/q-fn)
                 (distinct-habit-highlights 10))
                (mapv :highlight)
                (sort))))))

(comment
  ;; .n.b "is" macro doesn't work inside a rich comment
  ;; this is a template
(deftest test-
  (testing ""
    (is (x ((tu/q-fn)
            (testme))))))
  )


;; to create test user, we must bypass our query-function that always rolls back:
;; - start repl from this buffer (so it uses "devtest" profile)
;; - (go)
;; - switch repl namespace to this buffer, and evaluate it
;; - run ((:todefer/queries (tu/system-state)) (create-user "testuser" "testpass"))

;; the lines below mean, if starting a REPL from here, it uses devtest profile

;; Local Variables:
;; cider-clojure-cli-aliases: "devtest"
;; End:
