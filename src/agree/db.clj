(ns agree.db
  "Interface to durable storage (currently SQLite)"
  (:require [hugsql.core :as sql]
            [agree.schema :as schema]
            [agree.util :refer [now index-by hyphenate-keys]]))

(def conn {:classname "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname (or (System/getenv "AGREE_DB_PATH") "./resources/data.db")})

(sql/def-db-fns-from-string "-- :name execute-raw :!\n:sql:raw")
(sql/def-db-fns "queries.sql")
(sql/def-db-fns "operations.sql")

(defn execute-sql! [sql-str] (execute-raw conn {:raw sql-str}))
(defn schema-version [] (nth schema/migrations (dec (schema/migration-version conn))))
(defn migrate
  ([] (migrate (count schema/migrations)))
  ([version] (do (schema/create-migrations-table conn)
                 (schema/migrate! conn version))))

(defn create [f m]
  (let [params (assoc m :created-at (now)) result (f conn params)]
    (assoc params :id (get result (keyword "last_insert_rowid()")))))

(defn create-claim [c] (create new-claim c))
(defn create-user [u] (create new-user u))
(defn create-vote [v] (create new-vote v))
(defn create-score [s] (create new-score s))
(defn update-score [s] (do (update-score-for-user conn s) s))
(defn update-claim-totals [totals] (set-claim-totals conn totals))
(defn change-vote [v] (do (set-vote-direction conn v) v))

(defn all-users [] (mapv hyphenate-keys (get-all-users conn)))
(defn all-scores [] (mapv hyphenate-keys (get-all-scores conn)))
(defn user-score [user-id] (score-for-user conn {:user-id user-id}))
(defn claims-since [timestamp] (mapv hyphenate-keys (claims-since-timestamp conn {:since timestamp})))
(defn votes-since [timestamp] (mapv hyphenate-keys (votes-since-timestamp conn {:since timestamp})))
(defn user-votes-since-claim [user-id claim-id]
  (mapv hyphenate-keys (user-votes-since-claim-id conn {:user-id user-id :claim-id claim-id})))

