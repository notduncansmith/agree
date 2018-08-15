(ns agree.schema
  (:require [hugsql.core :refer [def-db-fns map-of-db-fns]]))

(def-db-fns "migrate.sql")

(def migrations (map-of-db-fns "migrations.sql"))
(defn migration-version [conn] (-> (count-saved-migrations conn) (first) (:count)))

(defn migrate!
  "Run all migrations between the most-recently-run and `target-version`"
  [conn target-version]
  (let [current-version (migration-version conn)
        remaining-migrations (->> (keys migrations) (sort) (take target-version) (drop current-version))]
     (mapv #(do (println %
                  (((migrations %) :fn) conn)
                  (record-migration conn {:name %})))
           remaining-migrations)))