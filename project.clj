(defproject agree "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [org.xerial/sqlite-jdbc "3.23.1"]
                 [com.layerware/hugsql "0.4.9"]
                 [http-kit "2.2.0"]
                 [compojure "1.5.1"]
                 [hiccup "1.0.5"]
                 [ring/ring-defaults "0.2.1"]
                 [garden "1.3.5"]]
  :target-path "target/%s"
  :main agree.core
  :profiles {:uberjar {:aot :all}})
