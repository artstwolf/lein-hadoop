(ns leiningen.hadoop
  "Create a jar for submission as a hadoop job."
  (:require [leiningen.compile :as compile]
            [leiningen.core.classpath :as classpath]
            [cemerick.pomegranate.aether :as aether])
  (:use [leiningen.pom :only [make-pom make-pom-properties]]
        [leiningen.jar :only [write-jar]]
        [clojure.java.io :only [file]]))

;; (defn eval-in-project
;;   "Support eval-in-project in both Leiningen 1.x and 2.x."
;;   [project form init]
;;     (let [[eip two?] (or (try (require 'leiningen.core.eval)
;;                               [(resolve 'leiningen.core.eval/eval-in-project)
;;                                true]
;;                               (catch java.io.FileNotFoundException _))
;;                          (try (require 'leiningen.compile)
;;                               [(resolve 'leiningen.compile/eval-in-project)]
;;                               (catch java.io.FileNotFoundException _)))]
;;       (if two?
;;         (eip project form init)
;;         (eip project form nil nil init))))

(defn- generate-hadoop-exclusions [project]
  (let [existing-excludes (:jar-exclusions project)
        all-deps (aether/resolve-dependencies
                  :local-repo (:local-repo project)
                  :offline? (:offline project)
;;                  :repositories (add-auth (:repositories project))
                  :repositories (:repositories project)
                  :coordinates (:dependencies project)
      :proxy (classpath/get-proxy-settings )
                  )
        hadoop-coordinate (filter #(= "org.apache.hadoop/hadoop-core" (str (first %)))
                                  (keys all-deps))
        hadoop-deps (when hadoop-coordinate
                      (aether/resolve-dependencies
                       :local-repo (:local-repo project)
                       :offline? (:offline project)
;;                       :repositories (add-auth (:repositories project))
                       :repositories (:repositories project)
           :proxy (classpath/get-proxy-settings )
                       :coordinates hadoop-coordinate))
        _ (println "hadoop-deps: " hadoop-deps)
        hadoop-regexes (map (fn [e]
                              (let [pkg (first e)]
                                (re-pattern (str (name (first pkg))
                                                 "-" (second pkg)))))
                            hadoop-deps)
        _ (println "hadoop-regexes: " hadoop-regexes)
        ]
    (concat existing-excludes hadoop-regexes)))

(defn- jar
  "Create a $PROJECT-hadoop.jar file containing the compiled .class files
as well as the source .clj files. If project.clj contains a :main symbol, it
will be used as the main-class for an executable jar."
  ([project jar-name]
     (compile/compile project)
     (let [jar-file (str (:root project) "/" jar-name)
           exclude-hadoop? (and (not (nil? (:exclude-hadoop project)))
                                (true? (:exclude-hadoop project)))
           filespecs [{:type :bytes
                       :path (format "meta-inf/maven/%s/%s/pom.xml"
                                     (:group project)
                                     (:name project))
                       :bytes (make-pom project)}
                      {:type :bytes
                       :path (format "meta-inf/maven/%s/%s/pom.properties"
                                     (:group project)
                                     (:name project))
                       :bytes (make-pom-properties project)}
                      (when (:resource-paths project)
                        {:type :paths :paths (:resource-paths project)})
                      {:type :path :path (:compile-path project)}
                      {:type :paths :paths (:source-paths project)}
                      {:type :path :path (str (:root project) "/project.clj")}]
           exclusions (generate-hadoop-exclusions project)
           nproject (if (and exclude-hadoop? (not (nil? exclusions)))
                      (assoc project :jar-exclusions exclusions)
                      project)]
       ;; TODO: support slim, etc
       (write-jar nproject jar-file filespecs)
       (println "Created" jar-file ", hadoop excluded: " exclude-hadoop?)
       jar-file))
  ([project] (jar project (str (:name project) ".jar"))))

(defn hadoop
  "Create a jar for submission to hadoop."
  ([project]
     (jar project (str (:name project) "-hadoop.jar"))))
