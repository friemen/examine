(ns examine.samples
  (:require [examine.core :as e]
            #? (:clj [examine.macros :refer [defvalidator]])
            [examine.constraints :as c]
            #? (:clj [clojure.test :refer :all]
                :cljs [cljs.test :as t]))
  #? (:cljs (:require-macros [examine.macros :refer [defvalidator]]
                             [cljs.test :refer [is are deftest testing]])))


;; The first sample shows how to use the defvalidator macro.

(defrecord Person [firstname lastname])

(defvalidator examine-contact

  ; firstname is not required, but - if set - must be a string
  :firstname c/not-nil? c/is-string

  ; lastname is required, so nil is not permitted
  :lastname  c/required c/is-string)


(deftest simple
  (is (= (examine-contact (Person. nil "Bar"))
         {}))

  (is (every? #{"A value is required" "Must be a string"}
              (-> (Person. nil nil) (examine-contact) :lastname)))

  (is (= (examine-contact (Person. 42 13))
         {:firstname '("Must be a string")
          :lastname '("Must be a string")})))


;; The second sample shows how navigation within a data structure is supported

(deftest navigation
  (let [data {:name "Donald Duck"
              :address {:zipcode "1234"
                        :city "Duckberg"}}
        rules (e/rule-set
               [[:address :zipcode]] (c/matches-re #"\d{5}"))]
    (is (= (->> data (e/validate rules) e/messages)
           {[:address :zipcode] '("Must match pattern \\d{5}")})))

  (let [data {:validity {:min 3
                         :max 2}}
        rules (e/rule-set
               [[:validity :min] [:validity :max]] c/min-le-max)]
    (is (= (->> data (e/validate rules) e/messages)
           {[:validity :min] '("Minmax violation")
            [:validity :max] '("Minmax violation")}))))


;; The third sample demonstrates how rule-sets can be applied in a nested fashion

(deftest nesting
  (let [data {:validity {:min 3
                         :max 2}}
        detail-rules (e/rule-set
                      [:min :max] c/min-le-max)
        rules (e/rule-set
               :validity (partial e/validate detail-rules))]
    (is (= (->> data (e/validate rules) e/messages)
           {[:validity :min] '("Minmax violation")
            [:validity :max] '("Minmax violation")}))))
