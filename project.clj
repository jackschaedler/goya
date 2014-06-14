(defproject goya "0.1.0-SNAPSHOT"
  :description "Pixel Art Studio"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [om "0.6.2"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :cljsbuild {
    :builds [{
      :source-paths ["src/cljs"]
        :compiler {
          :output-to "resources/main.js"
          :optimizations :advanced
          :pretty-print false
          :preamble ["react/react_with_addons.min.js"
                     "gifjs/dist/gif.js"
                     "lz-string/lz-string-1.3.3-min.js"]
          :externs ["react/react_with_addons.js"
                    "gifjs/dist/gif.js"
                    "lz-string/lz-string-1.3.3-min.js"]
          :closure-warnings {:externs-validation :off
                             :non-standard-jsdoc :off}}}]})
