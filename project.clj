(defproject org.clojars.baader/eszterlanc "0.3.0"
  :description "Clojurized access to magyarlanc. A toolkit for linguistic processing of Hungarian."
  :url "https://github.com/damesek/eszterlanc"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :java-source-paths ["src/java"]
  :resource-paths ["lib/magyarlanc-3.0.jar"]
  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "1.87.1366"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]}
  :repl-options {:init-ns eszterlanc.core})
