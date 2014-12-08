(ns three-man-chess.page)

(def params
  (let [location-hash  (-> js/window .-location .-hash)]
    (-> location-hash (.split "/") next vec)))

(defn redirect! [page & params]
  (.replace js/location (str (.-protocol js/location) "//"
                             (.-host js/location) "/"
                             page "#/"
                             (clojure.string/join "/" params))))
