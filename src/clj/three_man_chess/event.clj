(ns three-man-chess.event
  (:require [tailrecursion.javelin :refer [defc cell=]]))

(defmacro declare-event-cell [cell-name default-value]
  (let [listeners (symbol (str cell-name '-listeners))
        add-listeners (symbol (str 'add-to- listeners "!"))]
  `(do
     (defc ~cell-name ~default-value)
     (def ~listeners (atom []))
     (cell=
       (doseq [callback# (deref ~listeners)]
         (callback# ~cell-name)))

     (defn ~add-listeners [callback#]
       (swap! ~listeners conj callback#)))))
