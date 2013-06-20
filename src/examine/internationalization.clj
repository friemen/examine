(ns examine.internationalization
  "Java property file based i18n facility"
  (:require [clojure.java.io :as io])
  (:import [java.util Locale]))


(defn default-language
  "Returns the language code using Javas Locale.getDefault."
  []
  (-> (Locale/getDefault) (.getLanguage)))


(defn- load-props
  [resource-name]
  (with-open [^java.io.Reader reader (io/reader (io/resource resource-name))] 
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [k (read-string v)])))))


(defn load-messages-map
  "Loads a property file messages_XY.properties from classpath 
   and returns a map."
  [language-code]
  (load-props (str "messages_" language-code ".properties")))


(defn localize
  "Takes a messages map and translates the key to the corresponding
   human readable text."
  [messages-map key]
  (or (get messages-map key)
      key))

(def ^:dynamic *default-localizer* (->> (default-language)
                                        load-messages-map
                                        (partial localize)))

