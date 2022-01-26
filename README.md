# nano.env

Simple functions for reading environment variables with coercion, validation, defaults, and docstrings.

- Single place to read, coerce, validate, set default values, and document environment variables
- Unix-y environment variables ≠ Clojure values!
    - A naive coercion strategy can cause unexpected results (i.e. 0 can me true and 1 can mean false, or handling a env var of string `“true”`) 
    - The coercion strategy is open 
- Static & explicit declaration of environment variables that are easy to search for in a project
- Allow changing the source of environment variables
    - `System/getenv` is the default 
- Helpful error messages when values fail checks

An environment variable is described by a 'schema' map with the following keys

``` clojure
:env       ;; Name of the environment variable
:doc       ;; A docstring used in erorr messages
:default   ;; Default value if no raw value could be found
:parse     ;; Funciton to parse the raw value to a value
:predicate ;; Function to validate the parsed value 
```

## Installation

Include the following in your `deps.edn` file
``` clojure
{io.github.BrianChevalier/nano.env {:git/tag "v1.0.0" :git/sha "a2cb4f2"}}
```

## Typical Usage

Define a schema of a collection of environment variables and create a map with the environment varibles.

``` clojure
(defn ->int [v] (Long/parseLong v))

(def schema 
  [{:env       :USER
    :doc       "Current OS user"
    :predicate #'string?}
   {:env       :HOME
    :doc       "Home directory of user"
    :predicate #'string?}
   {:env       :PORT
    :doc       "Port for server" 
    :predicate #'int?
    :parse     #'->int}])

(nano.env/env schema)
=> {:USER "bchevalier", :HOME "/Users/bchevalier", :PORT 123}
```

Helpful validation error when `:PORT` is not set in the environment

```
Execution error (ExceptionInfo) at nano.env/validate! (REPL:55).
One or more `:value` does not conform to `:predicate`

|  :env |            :doc |          :predicate |           :parse | :raw | :value |
|-------+-----------------+---------------------+------------------+------+--------|
| :PORT | Port for server | #'clojure.core/int? | #'nano.env/->int |      |        |
```

## Sourcing Beyond the Environment
You can pass a custom function to look up an value instead of reading from the system environment. Here, values are read from an `EDN` file instead.

``` clojure
(defn from-file
  "Return function that reads env vars from file on the classpath instead of from environment"
  [filename]
  (let [m (-> filename
              io/resource
              slurp
              edn/read-string)]
    (fn [env] (get m env))))

(nano.env/env schema (from-file "env.edn"))
```
