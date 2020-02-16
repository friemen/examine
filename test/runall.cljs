(ns runall
  (:require
   [cljs.test :as t :refer-macros [run-all-tests]]
   [examine.core-test]
   [examine.constraints-test]))


(set! *print-fn* js/print)

(defmethod cljs.test/report [:cljs.test/default :end-run-tests]
  [args]
  (when-not (t/successful? args)
    (js/exit 1)))

(t/run-tests 'examine.core-test)
(t/run-tests 'examine.constraints-test)
