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

(defn clearform
  "just clear the new habit form"
  []
  (.reset (.getElementById js/document "pageform")))

(defn setup-form-handlers
  []
  ;; html produces this event https://htmx.org/headers/hx-trigger/
  (.addEventListener js/document.body "clearform" clearform))

;; tooltip
(defn adjust-tooltip-position [event]
  (js/console.log "adjusting tooltip")

  (let [hover-icon (.-currentTarget event)
        tooltip (.querySelector hover-icon ".tooltip")
        tooltip-rect (.getBoundingClientRect tooltip)
        hover-icon-rect (.getBoundingClientRect hover-icon)
        viewport-width js/window.innerWidth]

    ;; Adjust if tooltip goes off the left edge
    (when (< (.-left tooltip-rect) 0)
      (set! (.-left (.-style tooltip)) "0")
      (set! (.-transform (.-style tooltip)) "translateX(0)"))

    ;; Adjust if tooltip goes off the right edge
    (when (> (.-right tooltip-rect) viewport-width)
      (set! (.-left (.-style tooltip)) "auto")
      (set! (.-right (.-style tooltip)) "0")
      (set! (.-transform (.-style tooltip)) "translateX(0)"))))

(defn setup-tooltips []
  (let [hover-icons (.querySelectorAll js/document ".hover-icon")]
    (doseq [hover-icon hover-icons]
      (.addEventListener hover-icon "mouseover" adjust-tooltip-position)
      (.addEventListener hover-icon "focus" adjust-tooltip-position)
      (.addEventListener hover-icon "click" adjust-tooltip-position)
      (.addEventListener hover-icon "touchstart" adjust-tooltip-position)
      (.addEventListener hover-icon "touchend" adjust-tooltip-position))))
