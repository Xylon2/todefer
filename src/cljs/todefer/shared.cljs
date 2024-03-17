(ns todefer.shared)

(def expanded-collapsibles (atom #{}))

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

(defn toggle-element
  "toggle the presence of something in a set. return true or false to indicate"
  [elem]
  (if (contains? @expanded-collapsibles elem)
    (do
      (swap! expanded-collapsibles disj elem)
      false)
    (do
      (swap! expanded-collapsibles conj elem)
      true)))

(defn expand-it [elem]
  (let [contentstyle (.-style (.-nextElementSibling elem))]
    (.add (.-classList elem) "active")
    (set! (.-display contentstyle) "block")))

(defn collapse-it [elem]
  (let [contentstyle (.-style (.-nextElementSibling elem))]
    (.remove (.-classList elem) "active")
    (set! (.-display contentstyle) "none")))

(defn collapser
  "toggles the collapsed state of a collapsible"
  []
  (this-as this
    (if (toggle-element (.-id this))
      (expand-it this)
      (collapse-it this))))

(defn setup-collapsibles
  "add the event listener, and restore the state of the collapsibles"
  []
  (let [elements (byclass "collapsible")]

    ;; collapsibles
    (doseq [elem elements]
      (.addEventListener elem "click" collapser)
      (when (contains? @expanded-collapsibles (.-id elem))
        (expand-it elem)))))
