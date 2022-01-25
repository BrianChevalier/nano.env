# nano.env
Goal: [Simple](https://www.youtube.com/watch?v=SxdOUGdseq4) collection of functions for reading environment variables, coercing values, setting defaults, and documenting usage.

- Single place to read, coerce, validate, set default values, and document environment variables
- Unix-y environment variables ≠ Clojure values!
    - A naive coercion strategy can cause unexpected results (i.e. 0 can me true and 1 can mean false, or handling a env var of string `“true”`) 
    - This coercion strategy is up to you to implement
- **Static** & **Explicit** declaration of environment variables that are easy to search for in a project
- Allow rebinding the source of environment variables
    - Env vars come from `System/getenv` by default
    - In development, this provides a path to use an `env.edn` file as source of ‘env vars’
        - Helpful for GUI emacs users & per-project env vars 
- Easily see exact state of the current environment variables as interpreted by Clojure
- Helpful error messaging when an environment is mis-configured, before the system is started
- Env vars are just data and the side-effectfulness is constrained to a single place

``` clojure
{:env       :NAME_OF_ENV_VAR
 :doc       "Doc string to explain the var"
 :default   8080
 :->clj     #'->int ;; var to error report name of failure, default is identity
 :predicate #'int?}
```

An 'environment variable declaration' is a collection of of maps of this shape. Only `:env` and `:predicate` are required

## Extensibility
Open maps and plain data are *always* extensible. Add any additional data that is helpful to you.Use namespaced keys to avoid collision. Use this data to modify your environment variable pipeline.

If you want to redact secrets, for instance, add a flag to indicate this in each var. 
- `:secret?` flag to redact vars from logs

``` clojure
(defn env-without-secrets
  "Redact secret env vars"
  [declarations]
  (->> declarations
       (remove #(= true (:secret? %)))
       get-env-vars
       coerce-env-vars
       validate!))
```
