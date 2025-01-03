(defproject promises "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.12.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.11.132"]
                                  [org.clojure/core.async "1.7.701"]
                                  [org.clojure/core.match "1.1.0"]
                                  [com.stuartsierra/component "1.1.0"]
                                  [net.cassiel/lifecycle "0.1.0-SNAPSHOT"]
                                  [com.bhauman/figwheel-main "0.2.18"]
                                  ;; optional but recommended
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]
                                  ;; Other useful stuff:
                                  [binaryage/oops "0.7.2"]
                                  [nrepl/nrepl "1.3.1"]]
                   :resource-paths ["target"]
                   :clean-targets ^{:protect false} ["target"]}}
  :plugins [[com.github.liquidz/antq "RELEASE"]]
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]})
