(defproject lein-hadoop "1.1.0-msilva"
  :description "A leiningen plugin to build jars for hadoop."
  :dependencies [[com.cemerick/pomegranate "0.0.9" :exclusions [org.slf4j/slf4j-api]]
                 ]
  :dev-dependencies [[org.clojure/clojure "1.3.0"]
                     [lein-clojars "0.6.0"]]
  :eval-in-leiningen true)
