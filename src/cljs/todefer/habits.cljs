(ns todefer.habits
  (:require [todefer.shared :refer [log byid byclass
                                    setup-collapsibles
                                    expand-it
                                    toggle-element]]))

;; (set! (.-onkeyup js/document)
;;       (fn [e]
;;         (when (.-ctrlKey e)
;;           (case (.-key e)
;;             "y" (.focus (byid "add_new"))))))

;; (defn deferhandler
;;   "depending whether they are deferring to a new or existing category,
;;    either show the popup or action the defer"
;;   []
;;   (this-as this
;;     (let [value (.-value this)]
;;       (case value
;;         "new_5AXDrIpRBb69"
;;         ;; they want to defer to a new named category
;;         (do
;;           (.toggle (.-classList (byid "popup_defer_name")) "show")
;;           (.focus (byid "new_defcat_name")))
;;         ;; they want to defer to an existing named category
;;         (do
;;           (.setAttribute (byid "action_defer_existing") "name" "action")
;;           (.submit (byid "due_tasks_form")))))))

;; want to expand the due section by default
(let [elementname "due"
      duesection (.getElementById js/document elementname)]
  (toggle-element elementname)
  (expand-it duesection))

(setup-collapsibles)

(.onLoad js/htmx setup-collapsibles)

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

(let [hover-icons (.querySelectorAll js/document ".hover-icon")]
    (doseq [hover-icon hover-icons]
      (.addEventListener hover-icon "mouseover" adjust-tooltip-position)
      (.addEventListener hover-icon "focus" adjust-tooltip-position)
      (.addEventListener hover-icon "click" adjust-tooltip-position)
      (.addEventListener hover-icon "touchstart" adjust-tooltip-position)
      (.addEventListener hover-icon "touchend" adjust-tooltip-position)))
