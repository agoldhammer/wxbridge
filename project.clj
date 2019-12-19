(defproject wxbridge "0.1.0"
  :description "weather server"
  :url "github.com/agoldhammer"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.5.527"]
                 #_[org.clojure/tools.logging "0.5.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [compojure "1.6.1"]
                 [http-kit "2.4.0-alpha1"]
                 [commons-daemon/commons-daemon "1.2.2"]
                 #_[clj-http "3.10.0"]
                 #_[metosin/reitit "0.3.10"] ]
  :plugins [[lein-ring "0.12.5"]]
  #_#_:ring {:handler wxbridge.core/rts}
  :main wxbridge.core
  :aot :all
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
