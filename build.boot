#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.5.1"

(set-env!
  :project      'three-man-chess
  :version      "0.1.0-SNAPSHOT"
  :dependencies '[[tailrecursion/boot.task   "2.2.4"]
                  [tailrecursion/hoplon      "5.10.24"]
                  [org.clojure/clojure       "1.6.0"]
                  [tailrecursion/javelin     "3.7.1"]
                  [org.clojure/core.match "0.2.2"]
                  [rm-hull/monet             "0.2.1"]]
  :out-path     "resources/public"
  :main-class 'three-man-chess.core
  :src-paths    #{"src/hl" "src/cljs" "src/clj"})

;; Static resources (css, images, etc.):
(add-sync! (get-env :out-path) #{"assets"})

(require '[tailrecursion.hoplon.boot :refer :all]
         '[tailrecursion.castra.task :as c])

(deftask heroku
  "Prepare project.clj and Procfile for Heroku deployment."
  [& [main-class]]
  (let [jar-name   "3chess-standalone.jar"
        jar-path   (format "target/%s" jar-name)
        main-class (or main-class (get-env :main-class))]
    (assert main-class "missing :main-class entry in env")
    (set-env!
      :src-paths #{"resources"}
      :lein      {:min-lein-version "2.0.0" :uberjar-name jar-name})
    (comp
      (lein-generate)
      (with-pre-wrap
        (println "Writing project.clj...")
        (-> "project.clj" slurp
          (.replaceAll "(:min-lein-version)\\s+(\"[0-9.]+\")" "$1 $2")
          ((partial spit "project.clj")))
        (println "Writing Procfile...")
        (-> "web: java $JVM_OPTS -cp %s clojure.main -m %s $PORT"
          (format jar-path main-class)
          ((partial spit "Procfile")))))))

(deftask development
  "Build three-man-chess for development."
  []
  (comp (watch) (hoplon {:prerender false}) (c/castra-dev-server 'three-man-chess.api)))

(deftask dev-debug
  "Build three-man-chess for development with source maps."
  []
  (comp (watch) (hoplon {:pretty-print true
                         :prerender false
                         :source-map true}) (c/castra-dev-server 'three-man-chess.api)))

(deftask production
  "Build three-man-chess for production."
  []
  (hoplon {:optimizations :advanced}))
