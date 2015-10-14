(ns runall
  (:require  [cljs.test :as t :refer-macros [run-all-tests]]))

(defn runall []
  (enable-console-print!)
  (run-all-tests #"examine.*"))
