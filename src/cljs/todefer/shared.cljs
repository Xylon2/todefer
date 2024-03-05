(ns todefer.shared)

(defn log
  "concatenate and print to console"
  [& strings]
  ((.-log js/console) (reduce str strings)))

(defn byid
  "shortcut for getting element by id"
  [id]
  (.getElementById js/document id))

(defn byclass
  "shortcut for getting element by class"
  [name]
  (.getElementsByClassName js/document name))

(defn collapser
  "toggles the collapsed state of a collapsible"
  []
  (this-as this
    (do
      ;; set color and symbol on the collapsible button
      (.toggle (.-classList this) "active")

      ;; (un)collapse the contents of the collapsible
      (let [contentstyle (.-style (.-nextElementSibling this))]
        (case (.-display contentstyle)
          "block" (set! (.-display contentstyle) "none")
          (set! (.-display contentstyle) "block"))))))
