(ns examine.core-test
  (:use [clojure.test]
        [examine core constraints]))


(deftest rule-set-test
  (are [rs specs] (= rs (apply rule-set specs))
       {[:foo] [is-string]} [:foo is-string]
       {[:foo :bar] [is-string is-string]} [[:foo :bar] is-string is-string]
       {[:foo] [is-string] [:bar] [is-number is-string]} [:foo is-string :bar is-number is-string]))


(deftest sub-set-test
  (let [rules (rule-set :foo is-string is-number
                        [:foo :bar] min-le-max
                        :baz is-string)]
    (are [sub-rs specs] (= sub-rs (apply (partial sub-set rules) specs))
         {} []
         {[:foo] [is-string is-number] [:foo :bar] [min-le-max]} [:foo]
         {[:foo :bar] [min-le-max]} [:bar]
         {[:baz] [is-string]} [:baz]
         rules [:foo :bar :baz])))


(deftest map-data-provider-test
  (let [m {:foo {:bar "BAR"} :baz 42 :bam [47 13]}]
    (are [r     path] (= r (map-data-provider m path))
         "BAR"  [:foo :bar]
         42     :baz
         13     [:bam 1])))


(deftest map-while-test
  (are [r xs] (= (#'examine.core/map-while #(/ % 2) integer? xs))
       '() []
       '() [1]
       '(1 2 3) [2 4 6]
       '(1 2 3) [2 4 6 5 2 4]))


(deftest paths-msgs-pairs-test
  (are [pairs prefix paths] (= pairs (#'examine.core/paths-msgs-pairs prefix paths '("!")))
       '([:foo ("!")]) nil [:foo]
       '([:foo ("!")] [:bar ("!")]) nil [:foo :bar]
       '([[:baz :foo] ("!")]) [:baz] [:foo]
       '([:baz ("!")]) [:baz] [[]]
       '([[:baz :bar] ("!")]) [[:baz :bar]] [[]]
       '([[:baz :bar :foo] ("!")]) [[:baz :bar]] [:foo]
       '([[:foo :baz] ("!")] [:bar ("!")]) [:foo :bar] [:baz []]))


(deftest unpack-test
  (are [r   vrs] (= r (#'examine.core/unpack vrs))
       '() {}
       '([:foo ["C1"]]) {[:foo] {:c1 "C1"}}
       '([:foo ["C1" "C2"]] [:bar ["C1" "C2"]]) {[:foo :bar] {:c1 "C1" :c2 "C2"}}
       '([[:foo :baz] ["C2"]]) {[:foo] {:c1 {[:baz] {:c2 "C2"}}}}))


(deftest simple-validation-test
  (let [rules (rule-set :s is-string
                        [:min :max] min-le-max)]
    (are [vr  data] (= vr (messages (validate rules data)))
         {}   {:s "foo" :min 3 :max 4}
         {:s '("Must be a string")} {:s 42 :min 3 :max 4}
         {:min '("Minmax violation")
          :max '("Minmax violation")} {:s "foo" :min 4 :max 2})))


(deftest validation-with-data-navigation-test
  (let [rules (rule-set [[:foo :min] [:bar :max]] min-le-max)]
    (are [vr data] (= vr (messages (validate rules data)))
         {[:foo :min] '("Minmax violation")
          [:bar :max] '("Minmax violation")} {:foo {:min 3} :bar {:max 2}}
          {}                                 {:foo {:min 2} :bar {:max 2}})))


(deftest nested-validation-test
  (let [address-rules (rule-set :street-nr is-string
                                :zipcode (max-length 5)
                                [:from :to] min-le-max)
        person-rules  (rule-set :address (partial validate address-rules))]
    (are [vr data] (= vr (messages (validate person-rules data)))
         {[:address :zipcode] '("Max 5 characters allowed")}
         {:address {:street-nr "" :zipcode "123456" :from 2 :to 3}}
         {[:address :from] '("Minmax violation") [:address :to] '("Minmax violation")}
         {:address {:street-nr "" :zipcode "" :from 5 :to 3}})))


(deftest conditional-validation-test
  (let [rules (rule-set :age is-number number? (in-range 0 100))]
    (are [vr data] (= vr (messages (validate rules data)))
         {} {:age 42}
         {:age '("Must be between 0 and 100")} {:age -1}
         {:age '("Must be a number")} {:age "foo"})))


(deftest collection-validation-test
  (let [age (from-pred #(> %1 (:age %2)) "age-too-high")
        rules (rule-set [:max-age :contacts] (for-each age))]
    (are [vr                               data] (= vr (messages (validate rules data)))
         {:max-age '("age-too-high")
          [:contacts 1] '("age-too-high")}  {:max-age 100
                                             :contacts [{:name "Donald" :age 40}
                                                        {:name "Mickey" :age 101}]})))


(deftest render-test
  (are [text        msg] (= text (#'examine.core/render identity msg))
       "foo"        "foo"
       "foo"        ["foo"]
       "foo 42 13"  ["foo {1} {0}" 13 42]))


(deftest messages-for-test
  (let [rules (rule-set :foo is-string :bar is-number)]
    (are [msgs data path] (= msgs (->> data
                                       (validate map-data-provider rules)
                                       (messages-for path)))
         nil {:foo "foo" :bar 42} :foo
         '("Must be a string") {:foo nil :bar 42} :foo)))


(deftest update-test
  (are [rm om nm] (= rm (update om nm))
       {}                        {}                        nil
       {}                        {}                        {}
       {:foo 42}                 {}                        {:foo 42}
       {:foo 42}                 {:foo 42}                 nil
       {:foo 43}                 {:foo 42}                 {:foo 43}
       {:foo {:bar 43 :baz 10}}  {:foo {:bar 42 :baz 10}}  {:foo {:bar 43}})
  (let [rules (rule-set :foo is-number :bar is-string)
        vr1 (validate map-data-provider rules {:foo "" :bar ""})
        vr2 (validate map-data-provider rules {:foo 42 :bar ""})]
    (is (= {} (messages (update vr1 vr2))))
    (is (= {:foo '("Must be a number")} (messages (update vr2 vr1))))))


(defvalidator v :foo is-string)

(deftest defvalidator-test
  (is (= {:foo '("Must be a string")} (v {:foo 42}))))
