(ns todefer.todo
  (:require [todefer.shared :refer [log byid byclass
                                    setup-collapsibles
                                    expand-it
                                    toggle-element]]))

;; want to expand the today section by default
(let [elementname "today"
      todaysection (.getElementById js/document elementname)]
  (toggle-element elementname)
  (expand-it todaysection))

(setup-collapsibles)

(.onLoad js/htmx setup-collapsibles)
