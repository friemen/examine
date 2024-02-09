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
  (is (= {}
         (examine-contact (Person. nil "Bar"))))

  (is (every? #{"A value is required" "Must be a string"}
              (-> (Person. nil nil) (examine-contact) :lastname)))

  (is (= {:firstname '("Must be a string")
          :lastname '("Must be a string")}
         (examine-contact (Person. 42 13)))))


;; The second sample shows how navigation within a data structure is supported

(deftest navigation
  (let [data {:name "Donald Duck"
              :address {:zipcode "1234"
                        :city "Duckberg"}}
        rules (e/rule-set
               [[:address :zipcode]] (c/matches-re #"\d{5}"))]
    (is (= {[:address :zipcode] '("Must match pattern \\d{5}")}
           (->> data (e/validate rules) e/messages))))

  (let [data {:validity {:min 3
                         :max 2}}
        rules (e/rule-set
               [[:validity :min] [:validity :max]] c/min-le-max)]
    (is (= {[:validity :min] '("Minmax violation")
            [:validity :max] '("Minmax violation")}
           (->> data (e/validate rules) e/messages)))))


;; The third sample demonstrates how rule-sets can be applied in a nested fashion

(deftest nesting
  (let [data {:validity {:min 3
                         :max 2}}
        detail-rules (e/rule-set
                      [:min :max] c/min-le-max)
        rules (e/rule-set
               :validity (partial e/validate detail-rules))]
    (is (= {[:validity :min] '("Minmax violation")
            [:validity :max] '("Minmax violation")}
           (->> data (e/validate rules) e/messages)))))


;; This sample shows that constraints can just return maps instead of strings or vectors

(deftest map-messages
  (let [age-constraint (fn [age]
                       (if-not (< 0 age 120)
                         {:type :warning :text "age is not within usual values"}))
        rules (e/rule-set
               :age age-constraint)]
    (is (= {:age '({:type :warning, :text "age is not within usual values"})}
           (->> {:age 121} (e/validate rules) (e/messages))))))
