(ns examine.internationalization
  "Java property file based i18n facility"
  (:require #+clj [clojure.java.io :as io])
  (:import #+clj [java.util Locale]))


(defn default-language
  "Returns the language code using Javas Locale.getDefault."
  []
  #+clj (-> (Locale/getDefault) (.getLanguage))
  #+cljs goog/LOCALE)


#+clj
(defn- load-props
  [resource-name]
  (with-open [^java.io.Reader reader (io/reader (io/resource resource-name))] 
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [k (read-string v)])))))

#+clj
(defn load-messages-map
  "Loads a property file messages_XY.properties from classpath 
   and returns a map."
  [language-code]
  (load-props (str "messages_" language-code ".properties")))


(defn localize
  "Looks up the key in the messages-map, returns either the value if
  present or the key itself."
  [messages-map key]
  (or (get messages-map key)
      key))


(def default-msgs
  {"exception-thrown"  "Exception was thrown: {0}"
   "value-required"    "A value is required"
   "must-not-be-empty" "Collection must not be empty"
   "string-required"   "Must be a string"
   "number-required"   "Must be a number"
   "integer-required"  "Must be an integer number"
   "date-required"     "Must be a date"
   "boolean-required"  "Must be true or false"
   "min-greater-max"   "Minmax violation"
   "length-exceeded"   "Max {0} characters allowed"
   "below-min-length"  "Min {0} characters required"
   "not-in-range"      "Must be between {0} and {1}"
   "unexpected-value"  "Must be one of {0}"
   "no-re-match"       "Must match pattern {0}"})

(def default-localizer (partial localize default-msgs))
