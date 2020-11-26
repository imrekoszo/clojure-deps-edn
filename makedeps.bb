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
  (let [->lite #(-> #{:performance/benchmark :imre/hashp
                      #_:imre/nrebl
                      :imre/trace :inspect/reveal}
                    (conj %)
                    (sort))
        ->full #(-> #{:imre/measure :imre/speculative
                      :alpha/reflect :imre/decompile}
                    (into %)
                    (sort))
        lite   (->lite :inspect/rebl)
        full   (->full lite)
        lite-8 (->lite :inspect/rebl-java8)]
    {:cursive/lite   lite
     :cursive/full   full

     :cursive/lite-8 lite-8
     :cursive/full-8 (->full lite-8)

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
