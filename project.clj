(defproject examine "1.3.2-SNAPSHOT"
  :description
  "Validating Clojure data"
  :url
  "https://github.com/friemen/examine"
  :license
  {:name "Eclipse Public License"
   :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [org.clojure/clojurescript "1.10.914" :scope "provided"]]
  :plugins
  [[codox "0.10.7"]
   [lein-cljsbuild "1.1.8"]]

  :codox
  {:defaults                  {}
   :sources                   ["src"]
   :exclude                   []
   :src-dir-uri               "https://github.com/friemen/examine/blob/master/"
   :src-linenum-anchor-prefix "L"}

  :scm
  {:name "git"
   :url  "https://github.com/friemen/examine"}

  :repositories
  [["clojars" {:url   "https://clojars.org/repo"
               :creds :gpg}]]

  :profiles
  {:dev
   {:source-paths
    ["dev"]
    :dependencies
    [[org.clojure/clojurescript "1.10.914"]
     [cider/piggieback "0.5.3"]]
    :repl-options
    {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}

  :aliases
  {"deploy" ["do" "clean," "deploy" "clojars"]})
