(ns examine.macros
  "Macros for convenient validator creation"
  (:require [examine.core :refer [messages rule-set validate]]
            [examine.internationalization :as i18n]))


(defmacro defvalidator
  "Yields a defn for a one-argument validator function.
   The validator function returns a localized messages map,
   using the default localizer. The data is accessed by
   the map-data-provider.
   An empty map represents valid data."
  [sym & specs]
  `(defn ~sym [data#]
     (let [rules# (rule-set ~@specs)]
       (messages i18n/default-localizer (validate rules# data#)))))

