(defproject disclojure "0.1.0-SNAPSHOT"
  :description "A comprehensive library to interact with the Discord API"
  :url "https://github.com/joeltio/disclojure"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot disclojure.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
