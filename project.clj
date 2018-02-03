(defproject cljs-webgl-test-1 "0.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.4.474"]]

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.14"]]

  :source-paths ["src"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel {:websocket-host :js-client-host}
                        :compiler {:main webgl-test.core
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/cljs-webgl-test-1.js"
                                   :output-dir "resources/public/js/out"
                                   :source-map-timestamp true}}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/cljs-webgl-test-1.js"
                                   :main webgl-test.core
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["resources/lib/gl-matrix.ext.js"]}}]}

  :figwheel {:css-dirs ["resources/public/css"]
             :open-file-command "open-in-intellij"
             :repl false}

  :profiles {:dev {:clean-targets ^{:protect false} ["resources/public/js" :target-path]}}

  :aliases {"dist" ["do"
                    ["clean"]
                    ["cljsbuild" "once" "min"]]})
