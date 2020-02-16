(defproject examine "1.3.2-SNAPSHOT"
  :description
  "Validating Clojure data"
  :url
  "https://github.com/friemen/examine"
  :license
  {:name "Eclipse Public License"
   :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.9.946" :scope "provided"]]
  :plugins
  [[codox "0.10.7"]
   [lein-cljsbuild "1.1.7"]]

  :codox
  {:defaults {}
   :sources ["src"]
   :exclude []
   :src-dir-uri "https://github.com/friemen/examine/blob/master/"
   :src-linenum-anchor-prefix "L"}

  :scm
  {:name "git"
   :url "https://github.com/friemen/examine"}

  :repositories
  [["clojars" {:url "https://clojars.org/repo"
               :creds :gpg}]]

  :profiles
  {:profiles
   {:dev
    {:source-paths
     ["dev"]
     :dependencies
     [[org.clojure/clojurescript "1.9.946"]
      [cider/piggieback "0.4.2"]]
     :repl-options
     {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}}

  :cljsbuild
  {:test-commands
   {"unit-tests" ["jjs" "target/testable.js" #_"phantom/unit-test.js" #_"phantom/unit-test.html"]}
   :builds
   [{:source-paths ["src" "test"]
     :compiler {:output-to "target/testable.js"
                :output-dir "target/js"
                :source-map "target/testable.js.map"
                :optimizations :whitespace
                :static-fns true}}]}

  :aliases
  {"deploy" ["do" "clean," "deploy" "clojars"]
   "test" ["do" "clean," "test," "cljsbuild" "test"]})
