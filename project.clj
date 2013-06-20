(defproject examine "1.0.0"
  :description "Validating Clojure data"
  :url "https://github.com/friemen/examine"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  
  :scm {:name "git"
        :url "https://github.com/friemen/examine"}
  :repositories [["clojars" {:url "https://clojars.com/repo"
                             :creds :gpg}]]
  :codox {:src-dir-uri "https://github.com/friemen/examine/blob/master"
          :src-linenum-anchor-prefix "L"})
