#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.set :as set])

(def deps
  (-> *command-line-args*
      (first)
      (or "deps.edn")
      (slurp)
      (edn/read-string)))

(def secret
  (try
    (-> *command-line-args*
        (second)
        (or "secret-deps.edn")
        (slurp)
        (edn/read-string))
    (catch Exception _)))

(def all-deps (merge-with merge deps secret))

(def alias-recipes
  (let [lite (-> #{:performance/benchmark :imre/hashp
                   :imre/trace :inspect/reveal}
                 (sort))
        ->full #(-> #{:imre/measure :imre/speculative
                      :alpha/reflect :imre/decompile}
                    (into %)
                    (sort))
        full   (->full lite)]
    {:cursive/lite   lite
     :cursive/full   full

     ;; backwards-compatibility
     :cursive-lite   lite
     :cursive-full   full}))

(defn recipe->alias [recipe]
  (-> all-deps
      (:aliases)
      ((apply juxt recipe))
      (->> (reduce (fn [acc m] (merge-with merge acc m))))
      (select-keys [:extra-deps])))

(update all-deps :aliases
        into (map (juxt key (comp recipe->alias val))) alias-recipes)
