(defproject pva-radio-be "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-cors "0.1.13"]
                 [ring "1.9.5"]
                 [org.clojure/data.json "0.2.7"]
                 [clj-http "3.12.3"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.36.0.3"]]
  :main pva-radio-be.handler
  :uberjar-name "pva-radio.jar"
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler pva-radio-be.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
