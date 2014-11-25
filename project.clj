(defproject examine "1.2.0"
  :description "Validating Clojure data"
  :url "https://github.com/friemen/examine"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371" :scope "provided"]]
  :jar-exclusions [#"\.cljx"]
  :plugins [[codox "0.8.10"]
            [org.clojars.frozenlock/cljx "0.4.6"]
            [lein-cljsbuild "1.0.4-SNAPSHOT"]
            [com.cemerick/clojurescript.test "0.3.1"]]
  :codox {:defaults {}
          :sources ["src/clj" "target/classes"]
          :exclude []
          :src-uri-mapping {#"target/classes" #(str "src/cljx/" % "x")}
          :src-dir-uri "https://github.com/friemen/examine/blob/master/"
          :src-linenum-anchor-prefix "L"}
  :scm {:name "git"
        :url "https://github.com/friemen/examine"}
  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :creds :gpg}]]
  :hooks [cljx.hooks]
  :source-paths ["src/clj"]
  :test-paths ["target/test-classes"]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.5"]]
                   :injections [(require 'cemerick.austin.repls)
                                (defn browser-repl []
                                  (cemerick.austin.repls/cljs-repl (reset! cemerick.austin.repls/browser-repl-env
                                                                           (cemerick.austin/repl-env))))]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha4"]]}}

  :cljsbuild {:test-commands {"unit-tests" ["phantomjs" :runner "target/testable.js"]}
              :builds [{:source-paths ["src/clj" "target/classes" "target/test-classes"]
                        :compiler {:output-to "target/testable.js"
                                   :output-dir "target/js"
                                   :source-map "target/testable.js.map"
                                   :optimizations :advanced
                                   :static-fns true}}]}
  
  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}
                  {:source-paths ["test"]
                   :output-path "target/test-classes"
                   :rules :clj}
                  {:source-paths ["test"]
                   :output-path "target/test-classes"
                   :rules :cljs}]}
  
  :aliases {"all" ["with-profile" "+dev:+1.5:+1.7"]
            "deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do" "clean," "test," "cljsbuild" "test"]})
