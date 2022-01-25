(ns nano.env
  "A collection of simple functions for reading,
  coercing, validating, and setting defaults for environment
  variables"
  (:require [clojure.string :as str]))

(defn ^:dynamic lookup-var!
  "Side-effectfully get environment variables from the runtime.
  Can be dynamically rebound to load env vars from different sources"
  [v]
  (System/getenv v))

(defn get-env-vars
  "Side-effectfully get the runtime environment variables
  based on the declared environment variables"
  [env-var-declarations]
  (map (fn [{:keys [env] :as m}]
         (assoc m :raw (lookup-var! (name env))))
       env-var-declarations))

(defn validate!
  "Validates each environment variable.
  Acts as identity function if no errors are thrown."
  [env-vars]
  (let [m->message (fn [{:keys [env predicate value]}]
                     (when-not (predicate value)
                       (str "Error: " (name env) " " value " " predicate)))
        messages   (->> env-vars
                        (map m->message)
                        (remove nil?)
                        seq)]
    (if-not messages
      env-vars
      (throw (Exception. (str/join "\n" messages))))))

(defn coerce
  "Coerce a value given a predicate"
  [{:keys [env raw ->clj]
    :or   {->clj #'identity}}]
  (try
    (->clj raw)
    (catch Exception _e
      (println (str "Could not coerce " (name env) " " raw " with " (:name (meta ->clj)))))))

(defn coerce-env-vars
  "Takes raw unix-y value and coerces it using the predicate or uses
  default if there is no raw value"
  [env-vars]
  (map
   (fn [{:keys [raw default] :as m}]
     (let [from-env (when raw
                      (coerce m))
           value    (or from-env default)]
       (assoc m :value value)))
   env-vars))

(defn keyed-by
  "Take collection of maps and create a map of
  (get-key m) -> (get-value m)
  Example usage to index a collection by :id
    (keyed-by :id coll)"
  ([get-key coll]
   (keyed-by get-key identity coll))
  ([get-key get-value coll]
   (reduce (fn [out m]
             (assoc out (get-key m) (get-value m)))
           {}
           coll)))

(defn env-data
  "Return declarations with enriched collection
  of validated and coereced environment variables"
  [declarations]
  (-> declarations
      get-env-vars
      coerce-env-vars
      validate!))

(defn env-map
  "Get a map of environment variables->coered & validated values"
  [env-var-declarations]
  (->> env-var-declarations
       env-data
       (keyed-by :env :value)))
