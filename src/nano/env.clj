(ns nano.env
  "A collection of simple functions for reading,
  coercing, validating, and setting defaults for environment
  variables"
  (:require [clojure.pprint :as pprint]))

(defn- ^:dynamic lookup-var!
  "Side-effectfully get environment variables from the runtime.
  Can be dynamically rebound to load env vars from different sources"
  [v]
  (System/getenv (name v)))

(defn get-env-vars
  "Side-effectfully get the runtime environment variables
  based on the declared environment variables"
  [schema]
  (map (fn [{:keys [env] :as m}]
         (assoc m :raw (lookup-var! env)))
       schema))

(defn- coerce
  "Coerce a value given a predicate"
  [{:keys [raw parse]
    :or   {parse #'identity}
    :as   m}]
  (try
    (parse raw)
    (catch Exception ex
      (throw (ex-info "Could not coerce value" m ex)))))

(defn coerce-env-vars
  "Takes raw unix-y value and coerces it using the predicate or uses
  default if there is no raw value"
  [schema]
  (map
   (fn [{:keys [raw default] :as m}]
     (let [from-env (when raw
                      (coerce m))
           value    (or from-env default)]
       (assoc m :value value)))
   schema))

(defn- valid?
  [{:keys [predicate value]}]
  (if predicate
    (predicate value)
    true))

(defn validate!
  "Validates each environment variable.
  Acts as identity function if no errors are thrown."
  [schema]
  (let [errors (remove valid? schema)]
    (when (seq errors)
      (throw (ex-info (str "One or more `:value` does not conform to `:predicate`\n"
                           (with-out-str (pprint/print-table errors)))
                      {:errors errors})))
    schema))

(defn keyed-by
  "Take collection of maps and create a map of
  (get-key m) -> (get-value m)
  Example usage to index a collection by :id
    (keyed-by :id coll)"
  ([get-key coll]
   (keyed-by get-key identity coll))
  ([get-key get-value coll]
   (zipmap (map get-key coll)
           (map get-value coll))))

(defn env-data
  "Return declarations with enriched collection
  of validated and coereced environment variables"
  ([schema]
   (env-data schema lookup-var!))
  ([schema lookup-var]
   (binding [lookup-var! lookup-var]
     (-> schema
         get-env-vars
         coerce-env-vars
         validate!))))

(defn env
  "Get a map of environment variables->coered & validated values"
  ([schema]
   (env schema lookup-var!))
  ([schema lookup-var!]
   (let [schema (env-data schema lookup-var!)]
     (with-meta (keyed-by :env :value schema)
       {::schema schema}))))
