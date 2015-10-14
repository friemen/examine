(ns examine.macros
  "Macros for convenient validator creation"
  (:require [examine.core]
            [examine.internationalization :as i18n]))


(defmacro defvalidator
  "Yields a defn for a one-argument validator function.
   The validator function returns a localized messages map,
   using the default localizer. The data is accessed by
   the map-data-provider.
   An empty map represents valid data."
  [sym & specs]
  `(defn ~sym [data#]
     (let [rules# (examine.core/rule-set ~@specs)]
       (examine.core/messages i18n/default-localizer (examine.core/validate rules# data#)))))

