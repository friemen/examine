(ns examine.core
  "Function for creation of rule-sets, validating, and using validation results"
  (:refer-clojure :exclude [update])
  (:require [clojure.set :as cs]
            [examine.internationalization :as i18n])
  (:import #? (:clj  [java.text MessageFormat]
               :cljs [goog.i18n MessageFormat])))


(defn- as-vector
  "Returns a vector from xs.
  If xs is not sequential a wrapping vector is created."
  [xs]
  (cond
   (vector? xs) xs
   (sequential? xs) (vec xs)
   :else (vector xs)))


(defn- without-nil
  "Removes duplicates and the value nil from the sequence xs."
  [xs]
  (seq (disj (set xs) nil)))


(defn- path-msgs-pair
  [msgs prefix path]
  (vector (let [p (if (= [] path)
                    prefix
                    (if prefix
                      (conj (as-vector prefix) path)
                      path))]
            (if (and (coll? p) (= 1 (count p)))
              (first p)
              p))
          msgs))


(defn- paths-msgs-pairs
  "Creates a pair for every path in paths and the msgs sequence."
  [prefix paths msgs]
  (if (empty? msgs)
    (list)
    (if (= (count prefix) (count paths))
      (map (partial path-msgs-pair msgs) prefix paths)
      (map (partial path-msgs-pair msgs prefix) paths))))

;; prefix                          paths
;; [:foo :bar]  {2-arg-constraint {[:baz] {1-arg-constraint "msg"}}} <-- nonsense
;;
;; [:foo]       {1-arg-constraint {[:bar :baz] {2-arg-constraint "msg"}} <-- nested validation
;; => [[:foo :bar] ("msg")] [[:foo :baz] ("msg")]
;;
;; [:max-age :contacts] {2-arg-constraint {[[] 0] {2-arg-constraint}}} <-- collection
;; => [:max-age ("msg")] [[:contacts 0] ("msg")]


(defn- unpack
  "Takes possibly nested validation results and creates a seq of pairs where
  the first item is a path and the second a seq of messages. The paths of
  nested validation results will be prefixed with the path to the
  data that corresponds to the validation results."
  ([validation-results]
   (unpack nil validation-results))
  ([prefix validation-results]
   (mapcat (fn [[paths msgmap]]
             (let [{nested-vrs  true
                    string-msgs false} (->> msgmap vals without-nil (group-by map?))]
               (apply concat
                      (paths-msgs-pairs prefix paths string-msgs)
                      (map (partial unpack (concat prefix paths)) nested-vrs))))
           validation-results)))


(defn- render
  "Creates a localized human readable text from a message. The localizer-fn
  is a one-arg function that maps the text-key of the message to a human
  readable text.
  Message can either be a string or a vector [text args] to fill the
  placeholders."
  [localizer-fn msg]
  (if msg
    (let [[text args] (if (vector? msg) [(first msg) (rest msg)] [msg (list)])
          localized-text (localizer-fn text)]
      (if (and localized-text args)
        #? (:clj (MessageFormat/format localized-text (to-array args))
            :cljs (-> (MessageFormat. localized-text)
                       (.format (->> args
                                     (map vector (range))
                                     (into {})
                                     (clj->js)))))
        text))
    nil))


(defn- values
  "Returns the corresponding values for the given paths."
  [data-provider paths data]
  (map (partial data-provider data) paths))


(defn- apply-constraints
  "Applies the constraints from consconds to the values. The arity of
  the functions in consconds must be at most the number of values.
  Whenever a condition returns false all subsequent constraints are ignored.
  Returns a map of {constraint-fn -> message-or-nil}."
  [consconds values]
  (->> consconds
       (reduce (fn [[msgs ignore?] c]
                 ;; c is either
                 ;;  - a constraint-fn (returning string or nil)
                 ;;  - a condition (returning true or false)
                 ;;  - a vector [constraint-fn alternative-message]
                 (let [constraint-fn (if (vector? c) (first c) c)
                       msg           (if-not ignore? (apply constraint-fn values))]
                   (case msg
                     false [msgs true]
                     true  [msgs ignore?]
                     nil   [(assoc msgs constraint-fn nil) ignore?]
                     (let [msg (if (vector? c) (second c) msg)]
                       [(assoc msgs constraint-fn msg) ignore?]))))
               [{} false])
       (first)))


(defn constraint-or-condition?
  "Returns true if x is either a function or a vector [fn
  alternative-message]."
  [x]
  (or (fn? x)
      (and (vector? x)
           (-> x first fn?)
           (-> x second string?))))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API

(defn rule-set
  "Creates a rule-set from paths, constraints and conditions.

  The general structure of `specs` is:
    path constraints-and-conditions+
    path constraints-and-conditions+

  A path points to data. It can either be a key, or a vector
  containing keys. n paths are grouped by a vector to specify
  multiple data locations that apply to n-arg constraints.

  Examples:
  :foo
     Specifies a path to value in {:foo value}
  [:foo :bar]
    Specifies 2 paths to values in {:foo value1 :bar value2}
  [[:foo :bar]]
     Specifies a single path pointing to value in {:foo {:bar value}}
  [:foo [:bar :baz :bum]]
     Specifies 2 paths in {:foo value1 :bar {:baz {:bum value2}}}

  Constraints are 1-arg functions that return a string to flag
  a constraint violation for the given value or nil otherwise.

  Conditions are 1-arg predicates returning true or false that stop
  application of subsequent constraints after returning false.

  Example:
    (rule-set :foo required (min-length 3)
              [[:bar :baz]] number? (in-range 0 10))
 "
  [ & specs]
  (->> specs
       (partition-by constraint-or-condition?)
       (partition 2)
       (map (fn [[paths consconds]]
              [(-> paths first as-vector) (vec consconds)]))
       (into {})))


(defn sub-set
  "Returns the subset of the rule-set that contains only those
  rules that reference at least one of the paths given in ps."
  [rule-set & ps]
  (let [ps-set (set ps)]
    (->> rule-set
         (filter (fn [[paths consconds]]
                   (not (empty? (cs/intersection ps-set (set paths))))))
         (into {}))))


(defn map-data-provider
  "Default data provider that uses get or get-in to retrieve
  data from a map/record."
  [m keyword-or-vector]
  (let [g (if (vector? keyword-or-vector) get-in get)]
    (g m keyword-or-vector)))



(defn validate
  "Creates validation results by applying the constraints of the rule-set
  to the data. If the data-provider arg is missing the map-data-provider
  is used."
  ([rule-set data]
     (validate map-data-provider rule-set data))
  ([data-provider rule-set data]
     (->> rule-set
          (map (fn [[paths consconds]]
                 (let [vs (values data-provider paths data)]
                   [paths (apply-constraints consconds vs)])))
          (into {}))))



(defn messages
  "Returns a map {path -> sequence-of-human-readable-strings}.
  If the localizer-fn is not given the default-localizer is used."
  ([validation-results]
     (messages i18n/default-localizer validation-results))
  ([localizer-fn validation-results]
     (->> validation-results
          (unpack)
          (group-by first)
          (map (fn [[path msg-pairs]]
                 [path (->> msg-pairs
                            (mapcat (comp seq second))
                            (map (partial render localizer-fn)))]))
          (into {}))))


(defn messages-for
  "Returns a sequence of human readable strings for the given path.
   If the localizer-fn is not given the default-localizer is used."
  ([path validation-results]
     (messages-for i18n/default-localizer path validation-results))
  ([localizer-fn path validation-results]
     (get (messages localizer-fn validation-results) path)))


(defn has-errors?
  "Returns true if the validation results contain at least
  one not-nil message."
  [validation-results]
  (not (empty? (messages validation-results))))


(defn update
  "Takes two validation results (or any map) and recursively updates
  the first with all values of the second. Existing values of the
  first are kept."
  [old-map new-map]
  (reduce (fn [m [k nv]]
            (let [ov (get m k)]
              (assoc m k (if (map? ov) (update ov nv) nv))))
          old-map
          new-map))
