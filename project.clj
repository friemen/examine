(defproject examine "1.3.1"
  :description "Validating Clojure data"
  :url "https://github.com/friemen/examine"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48" :scope "provided"]]
  :plugins [[codox "0.8.10"]
            [lein-cljsbuild "1.1.2"]]
  :codox {:defaults {}
          :sources ["src"]
          :exclude []
          :src-dir-uri "https://github.com/friemen/examine/blob/master/"
          :src-linenum-anchor-prefix "L"}
  :scm {:name "git"
        :url "https://github.com/friemen/examine"}
  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :creds :gpg}]]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.6"]]
                   :injections [(require 'cemerick.austin.repls)
                                (defn browser-repl []
                                  (cemerick.austin.repls/cljs-repl (reset! cemerick.austin.repls/browser-repl-env
                                                                           (cemerick.austin/repl-env))))]}

             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}

  :cljsbuild {:test-commands {"unit-tests" ["phantomjs" "phantom/unit-test.js" "phantom/unit-test.html"]}
              :builds [{:source-paths ["src" "test"]
                        :compiler {:output-to "target/testable.js"
                                   :output-dir "target/js"
                                   :source-map "target/testable.js.map"
                                   :optimizations :whitespace
                                   :static-fns true}}]}

  :aliases {"all" ["with-profile" "+dev:+1.7"]
            "deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do" "clean," "test," "cljsbuild" "test"]})
