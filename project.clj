(defproject morphics "0.1.0-SNAPSHOT"
  :description "malleable, evolvable software"
  :url "https://github.com/deansher/morphics"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.rpl/specter "1.1.0"]
                 [metosin/spec-tools "0.6.2-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/test.check "0.10.0-alpha2"]
                                  [orchestra "2017.11.12-1"]
                                  [expound "0.6.0"]]}})
                                  ; [spectrum "0.1.5-SNAPSHOT"] one day this ship will come in

