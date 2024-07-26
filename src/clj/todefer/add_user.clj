(ns todefer.add-user
  (:require [todefer.queries :as q]))

(defn prompt-for-input
  [prompt-message]
  (loop []
    (println prompt-message)
    (let [input (read-line)]
      (if (>= (count input) 3)
        input
        (do
          (println "Input must be at least 3 characters long. Please try again.")
          (recur))))))

(defn add-user
  [exec-query]
  (let [user (prompt-for-input "Enter username:")
        pass (prompt-for-input "Enter password:")]
    (exec-query (q/create-user user pass))))
