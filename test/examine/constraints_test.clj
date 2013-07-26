(ns examine.constraints-test
  (:use [examine.constraints]
        [clojure.test]))

(deftest from-pred-test
  (let [lte (from-pred <= "Min value must be lower or equal than max value")]
    (are [msg min max] (= msg (lte min max))
         "Min value must be lower or equal than max value" 4 3
         nil 3 4)))


(deftest not-blank-test
  (are [r       x] (= r (not-blank? x))
       true     "Foo"
       true     (java.util.Date.)
       false    ""
       false    nil))


(deftest from-exception-test
  (let [f (fn [t] (when t (throw (IllegalArgumentException. "BOOM!"))))
        cex (no-exception f)]
    (are [msg                         throw-ex] (= msg (cex throw-ex))
         nil                          false
         ["exception-thrown" "BOOM!"] true)))


(deftest is-boolean-test
  (are [msg               x] (= msg (is-boolean x))
       "boolean-required" 0
       "boolean-required" nil
       nil                true
       nil                false))

(deftest is-date-test
  (are [msg               x] (= msg (is-date x))
       "date-required"    nil
       "date-required"    "10.07.2013"
       nil                (java.util.Date.)))


(deftest max-length-test
  (are [msg                  x] (= msg ((max-length 3) x))
       nil                   "123"
       nil                   nil
       ["length-exceeded" 3] "1234"
       nil                   []
       ["length-exceeded" 3] [:foo :bar :baz :bam]))


(deftest min-length-test
  (are [msg                   x] (= msg ((min-length 3) x))
       nil                    "123"
       nil                    "1234"
       ["below-min-length" 3] "12"
       ["below-min-length" 3] nil))


(deftest in-range-test
  (are [msg                 x] (= msg ((in-range 2 4) x))
       nil                  2
       nil                  3
       nil                  4
       ["not-in-range" 2 4] 1
       ["not-in-range" 2 4] 5))


(deftest min-le-max-test
  (are [msg              x y] (= msg (min-le-max x y))
       nil               1 2
       nil              -1 2
       nil               1 1
       "min-greater-max" 2 1))


(deftest one-of-test
  (are [msg    expected-values      x] (= msg ((apply one-of expected-values) x))
       nil                          [:foo :bar] :bar
       nil                          [1 2 3]     1
       ["unexpected-value" [1 2 3]] [1 2 3]     4
       ["unexpected-value" [:bar]]  [:bar]      :foo))


(deftest matches-re-test
  (are [msg                    re-pattern x] (= msg ((matches-re re-pattern) x))
       nil                     #"foo.*"   "foobar"
       ["no-re-match" "foo.*"] #"foo.*"   "bar"))

