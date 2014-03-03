# examine

Validating Clojure data.

[![Build Status](https://travis-ci.org/friemen/examine.png?branch=master)](https://travis-ci.org/friemen/examine)

## Key Features

* Declarative style for formulating constraints.
* Extensible with arbitrary constraint functions.
* Supports n-arity constraints.
* Supports conditional validation.
* Decoupled data retrieval from validation.
* Nested validation.
* Supports I18N / localization.
* Narrowing of applicable constraints, in response to isolated changes of data.

## Usage

Include the following dependency in your project.clj 

    [examine "1.1.0"]

### Getting started

To get you started try the following in a REPL:
```clojure
(use 'examine.core)
;= nil

(use 'examine.constraints)
;= nil

(def data {:name "Donald Duck"
           :zipcode "1234"
           :city "Duckberg"})
;= #'user/data

(use 'examine.constraints)
;= nil

(defvalidator examine-address :zipcode (matches-re #"\d{5}"))
;= #'user/examine-address

(examine-address data)
;= {:zipcode ("Must match pattern \\d{5}")}
```

### Basics

Basically there are three important functions in the examine.core namespace:

* Rule-sets are maps, and can conveniently be created by the expression 
`(rule-set keywords-conditions-and-constraints)`. See the next section for
examples.
* The expression `(validate rule-set data)` applies all rules of the rule-set 
to the data and returns a validation results map. 
* To conveniently retrieve human readable texts from validation results,
the expression `(messages validation-results)` creates a map 
{path -> seq-of-texts}.

The `defvalidator` macro combines those functions to define a 1-arg 
function that returns a map with human readable texts.


### Rule set specification

Examples for rule-set specifications:
```clojure
(def r (rule-set :firstname required is-string
                 :lastname is-string))
```
applies constraint functions `required` and `is-string` to a value 
retrieved using `:firstname` keyword. A value retrieved by keyword
`:lastname` is only checked by `is-string`.

```clojure
(def r (rule-set [:from :to] min-le-max))
```
applies the two-arg constraint function 'min-le-max' to the
arguments retrieved by keywords `:from` and `:to`.

```clojure
(def r (rule-set [[:address :zipcode]] (matches-re #"\d{5}")))
```
applies the regex constraint `matches-re` to a value retrieved
by navigating a data structure using `:address` and `:zipcode`
in that order.

```clojure
(def r (rule-set :age is-number number? (in-range 0 120))
```
applies first the `is-number` constraint, and applies the `in-range` 
constraint only if the predicate `number?` returns true.

### Samples

See the [samples](test/examine/samples.clj) to learn more about
how examine can be used.

## Concepts

A *path* points to a value. 
For maps, usually a keyword denotes the path to a piece of data.
But a path can also be a vector of keywords that navigates into a
nested structure.

A *value-provider* is a function that takes a path as single argument
and returns the corresponding value.

A *constraint* is a function that takes one or more arguments, and 
returns nil if the data is valid. Otherwise it returns either a
message or a validation-result. 

A *condition* is a predicate. Conditions are used to stop application
of constraints after a condition returned false.

A *rule* is a pair of a path vector and a vector of constraints
and conditions. If a condition returns false for the data all subsequent
constraints and conditions will be ignored.

A *rule-set* is a map {path-vector -> constraint-condition-vector}. 

A *message* is a string or a vector. If it is a vector the first
item must be a string and all subsequent items are values.
The string denotes a key that has to be translated to human readable 
text by a localizer. The values are used to replace placeholders in
the localized text.

A *localizer* is a function that takes one key and returns a human
readable text, possibly containing placeholders (see 
java.text.MessageFormat).

A *validation-result* is a map of the following form:
    {data-path -> {constraint -> message}}.
If a message is nil then the validation for data-path + constraint 
was successful.


## API

Namespaces:

* core -- Contains validation functions.
* constraints -- Contains concrete constraints.
* internationalization -- Contains default localizer using resource bundles. 

### Namespace validation

**map-data-provider** --
Returns a value from a possibly nested data structure
using the given path. Path can either be a keyword or a vector of keywords
and indexes (as expected by get-in).

**has-errors?** --
Returns true if the given validation-result contains no messages.

**rule-set** --
Creates a rule-set from keywords, constraints and conditions.

**sub-set** --
Creates a rule-set from a given rule-set that contains only constraints that
apply to the given paths.

**update** --
Creates a validation-result from the first argument by recursively adding all 
validation-results from the second argument.

**messages** --
Returns a map of all message seqs from the given validation-result.

**messages-for** --
Returns a sequence of all messages from the given validation-result 
that apply to the given path.

**validate** --
Takes a value-provider, a rule-set and data and returns a validation-result.

**defvalidator** -- Defines a function in the current namespace that
takes data and returns a map of message seqs.


### Namespace constraints

**from-pred** --
Takes a predicate and a message and returns a constraint.

**no-exception** --
Takes a function with arbitrary number of arguments and returns
a constraint that returns the exception message text if any is thrown.

**Concrete constraints**:

* required -- Value must not be nil
* has-items -- Collection is not empty
* is-string -- Value is a string
* is-number -- Value is a number
* is-integer -- Value is an integer
* is-boolean -- Value is true or false
* is-date -- Value is of type Joda DateTime, Date or GregorianCalendar
* min-length n -- Collection or string has at least n items/characters
* max-length n -- Collection or string has at most n items/characters
* in-range min max -- Number is at least min, at most max
* matches-re regex -- String matches regular expression
* one-of values -- Value is one of the specified values
* for-each f -- Applies constraint-fn to cartesian product of collections
* min-le-max -- First value is not greater than second value

**Additional predicates**:

Predicates stop further validation when they return false.

 * not-nil? -- False if value is nil
 * not-blank? -- False if value is empty string or nil


### Namespace internationalization

**load-messages-map** -- Loads a property file messages_XY.properties from classpath 
and returns a map.

**default-language** -- Returns the language code using Javas Locale.getDefault.

**localize** -- Takes a messages map and translates the key to the corresponding
human readable text.

**\*default-localizer\*** -- A dynamically scoped var that points to a localizer
that uses the default language and a corresponding property file content for
translation.


# License

Copyright 2013 F.Riemenschneider

Distributed under the Eclipse Public License, the same as Clojure.
