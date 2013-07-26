(ns examine.core
  "Function for creation of rule-sets, validating, and using validation results"
  (:require [clojure.set :as cs]
            [examine.internationalization :as i18n])
  (:import [java.text MessageFormat]))


(defn- as-vector
  "Returns a vector from xs.
   If xs is not sequential a wrapping vector is created."
  [xs]
  (if (vector? xs) xs (if (sequential? xs) (vec xs) (vector xs))))


(defn rule-set
  "Creates a rule-set from the keywords and functions (constraints as well as
   conditions).
   Examples:
   TODO"
  [ & specs]
  (->> specs
       (partition-by fn?)
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
                 (let [msg (if ignore? nil (apply c values))]
                   (case msg
                         false [msgs true]
                         true [msgs ignore?]
                         [(assoc msgs c msg) ignore?])))
               [{} false])
       first))


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


(defn- without-nil
  "Removes duplicates and the value nil from the sequence xs."
  [xs]
  (seq (disj (set xs) nil)))


(defn path-msgs-pair
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

; prefix                          paths
; [:foo :bar]  {2-arg-constraint {[:baz] {1-arg-constraint "msg"}}} <-- nonsense
;
; [:foo]       {1-arg-constraint {[:bar :baz] {2-arg-constraint "msg"}} <-- nested validation
; => [[:foo :bar] ("msg")] [[:foo :baz] ("msg")]
;
; [:max-age :contacts] {2-arg-constraint {[[] 0] {2-arg-constraint}}} <-- collection 
; => [:max-age ("msg")] [[:contacts 0] ("msg")] 


(defn- unpack
  "Takes possibly nested validation results and creates a seq of pairs where
   the first item is a path and the second a seq of messages. The paths of
   nested validation results will be prefixed with the path to the
   data that corresponds to the validation results."
  ([validation-results]
     (unpack nil validation-results))
  ([prefix validation-results]
     (mapcat (fn [[paths msgmap]]
               (let [{nested-vrs true
                      string-msgs false} (->> msgmap vals without-nil (group-by map?))]
                 (apply concat
                        (paths-msgs-pairs prefix paths string-msgs)
                        (map (partial unpack paths) nested-vrs))))
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
        (MessageFormat/format localized-text (to-array args))
        text))
    nil))


(defn messages
  "Returns a map {path -> sequence-of-human-readable-strings}.
   If the localizer-fn is not given the default-localizer is used."
  ([validation-results]
     (messages i18n/*default-localizer* validation-results))
  ([localizer-fn validation-results]
     (->> validation-results
          unpack
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
     (messages-for i18n/*default-localizer* path validation-results))
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


(defmacro defvalidator
  "Yields a defn for a one-argument validator function.
   The validator function returns a localized messages map,
   using the default localizer. The data is accessed by
   the map-data-provider.
   An empty map represents valid data."
  [sym & specs]
  `(defn ~sym [data#]
     (let [rules# (rule-set ~@specs)]
       (messages i18n/*default-localizer* (validate rules# data#)))))

