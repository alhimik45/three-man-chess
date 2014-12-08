;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns three-man-chess.core
  (:require
   [ring.adapter.jetty               :refer [run-jetty]]
   [ring.middleware.resource         :refer [wrap-resource]]
   [ring.middleware.session          :refer [wrap-session]]
   [ring.middleware.session.cookie   :refer [cookie-store]]
   [ring.middleware.file             :refer [wrap-file]]
   [ring.middleware.file-info        :refer [wrap-file-info]]
   [tailrecursion.castra.handler     :refer [castra]]))

(def server (atom nil))

(defn wrap-dir-index [handler]
  (fn [req]
    (handler
     (update-in req [:uri]
                #(if (= "/" %) "/index.html" %)))))

(defn app [port]
  (->
   (castra 'three-man-chess.api)
   (wrap-session {:store (cookie-store {:key "a 16-byte secret"})})
   (wrap-file "resources/public")
   (wrap-file-info)
   (run-jetty {:join? false :port port})))

(defn start-server
  "Start castra demo server (port 33333)."
  [port]
  (swap! server #(or % (app port))))

(defn run-task
  [port]
  (start-server port)
  (fn [continue]
    (fn [event]
      (continue event))))

(defn -main
  "I don't do a whole lot."
  [& [port]]
  (run-task (Integer. (or port 8000))))
