(ns examine.constraints
  "Functions that yield constraints and a set of default constraints")


(defn from-pred
  "Takes a predicate and a message and returns a constraint."
  [pred message]
  (fn [& args]
    (when-not (apply pred args)
      message)))


(defn no-exception
  "Takes a function with arbitrary number of arguments and returns
   a constraint that returns the exception message text if any is thrown."
  [f]
  (fn [& args]
    (try (apply f args)
         nil
         (catch Exception ex ["exception-thrown" (.getMessage ex)]))))


(def not-nil?
  "Returns true if value is not nil."
  (complement nil?))


(defn not-blank?
  "Returns true if value is not nil and not an empty string"
  [x]
  (if (string? x)
    (> (count x) 0)
    (not-nil? x)))


(def required
  "Constraint-fn that passes if value is not nil."
  (from-pred not-nil? "value-required"))


(def has-items
  "Constraint-fn that passes if collection is not empty."
  (from-pred empty? "must-not-be-empty"))


(def is-string
  "Constraint-fn that passes if value is a string."
  (from-pred string? "string-required"))


(def is-number
  "Constraint-fn that passes if value is a number."
  (from-pred number? "number-required"))


(def is-integer
  "Constraint-fn that passes if value is an integer."
  (from-pred integer? "integer-required"))


(def is-date
  (from-pred #(some-> % .getClass .getName #{"java.util.Date"
                                             "org.joda.time.DateTime"
                                             "java.util.GregorianCalendar"})
             "date-required"))


(defn is-boolean
  "Constraint-fn that passes if value is true or false."
  [x]
  (when-not (or (true? x) (false? x))
    "boolean-required"))


(def min-le-max
  "Constraint-fn that passes if first arg is not greater than second arg."
  (from-pred <= "min-greater-max"))


(defn max-length
  "Returns constraint-fn that passes if collection or
   string has at most n items/characters."
  [n]
  (from-pred #(>= n (count %)) ["length-exceeded" n]))


(defn min-length
  "Returns constraint-fn that passes if collection or
   string has at least n items/characters."
  [n]
  (from-pred #(<= n (count %)) ["below-min-length" n]))


(defn in-range
  "Returns constraint-fn that passes if number is at
   least min, at most max."
  [min max]
  (from-pred #(and (>= % min) (<= % max)) ["not-in-range" min max]))


(defn one-of
  "Returns constraint-fn that passes if value is one
   of the specified values."
  [& values]
  (let [vset (set values)]    
    (fn [x]
      (when-not (vset x)
        ["unexpected-value" values]))))


(defn matches-re
  "Returns constraint-fn that passes if string matches
   regular expression re."
  [re]
  (fn [x]
    (if (string? x)
      (when-not (re-matches re x) ["no-re-match" (str re)])
      "string-required")))


(defn- index [xs]
  (map vector (iterate inc 0) xs))


(defn- as-seq [x]
  (if (coll? x) (seq x) (list x)))


(defn- cartesian-product [colls]
  (if (< 1 (count colls))
    (for [x (first colls) xs (cartesian-product (rest colls))]
      (cons x xs))
    (map list (first colls))))


(defn for-each
  "Returns constraint-fn that applies constraint-fn f to cartesian
   product of collections."
  [f]
  (fn [& args]
    (let [coll-args (->> args (filter coll?) set)]
      (->> args
           (map (comp index as-seq))
           cartesian-product
           (map (fn [pairs]
                  (let [indexes (map first pairs)
                        values (map second pairs)]
                    [(vec (map #(if (coll-args %2) %1 []) indexes args))
                     {f (apply f values)}])))
           (into {})))))


