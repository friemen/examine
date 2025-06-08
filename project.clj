(defproject examine "1.3.4-SNAPSHOT"
  :description
  "Validating Clojure data"

  :url
  "https://github.com/friemen/examine"

  :license
  {:name "Eclipse Public License"
   :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojure "1.12.1"]
   [org.clojure/clojurescript "1.12.42" :scope "provided"]]

  :plugins
  [[lein-codox "0.10.8"]
   [lein-cljsbuild "1.1.8"]]

  :codox
  {:language     :clojure
   :source-paths ["src"]
   :namespaces   [#"^examine"]
   :source-uri   "https://github.com/friemen/examine/blob/master/{filepath}#L{line}"}

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
    [[org.clojure/clojurescript "1.12.42"]
     [cider/piggieback "0.6.0"]]
    :repl-options
    {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}

  :aliases
  {"deploy" ["do" "clean," "deploy" "clojars"]})
