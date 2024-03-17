(ns todefer.tasks
  (:require [todefer.shared :refer [log byid byclass setup-collapsibles]]))

(set! (.-onkeyup js/document)
      (fn [e]
        (when (.-ctrlKey e)
          (case (.-key e)
            "y" (.focus (byid "add_new"))))))

(defn deferhandler
  "depending whether they are deferring to a new or existing category,
   either show the popup or action the defer"
  []
  (this-as this
    (let [value (.-value this)]
      (case value
        "new_5AXDrIpRBb69"
        ;; they want to defer to a new named category
        (do
          (.toggle (.-classList (byid "popup_defer_name")) "show")
          (.focus (byid "new_defcat_name")))
        ;; they want to defer to an existing named category
        (do
          (.setAttribute (byid "action_defer_existing") "name" "action")
          (.submit (byid "due_tasks_form")))))))

(setup-collapsibles)

(.onLoad js/htmx setup-collapsibles)
