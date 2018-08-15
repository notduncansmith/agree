(ns agree.core
  (:gen-class)
  (:require [clojure.tools.nrepl.server :as nrepl]
            [org.httpkit.server :refer [run-server]]
            [agree.http :refer [app-routes]]
            [agree.state :as state]
            [agree.db :as db]
            [agree.util :refer [hours-ago hours-since]]))

(def max-claim-age "Number of hours to display a claim in the feed" 72)
(def port (Integer. (or (System/getenv "PORT") 3000)))
(def nrepl-port (let [port (System/getenv "NREPL_PORT")] (if port (Integer. port))))

(defn initialize!
  "Migrate to the latest schema, then load application state from the database"
  []
  (do (db/migrate)
      (mapv state/load-score! (db/all-scores))
      (println "Loaded scores")
      (mapv state/load-user! (db/all-users))
      (println "Loaded users")
      (mapv state/load-claim! (db/claims-since (hours-ago max-claim-age)))
      (println "Loaded claims")
      (mapv state/load-vote! (db/votes-since (hours-ago max-claim-age)))
      (println "Loaded votes")
      (state/finalize-claims!)
      (println "Finalized closed claims")
      (println "Initialized!")))

(defonce stop-server (atom #()))
(defn start-server! [] (reset! stop-server (run-server app-routes {:port port})))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do (initialize!)
      (start-server!)
      (if nrepl-port (nrepl/start-server :port nrepl-port :bind "localhost"))
      (println (str "Started server on :" port " (nrepl on :" nrepl-port ")"))))
