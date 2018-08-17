(ns agree.state
  "Operate on the in-memory application state and synchronize it with on-disk state via db"
  (:require [agree.db :as db]
            [agree.newsfeed :as feed]
            [agree.scoreboard :as score]
            [agree.util :refer [now generate-token]]
            [clojure.set :refer [union]]))

(def feed-state "Newest-first sorted map of claim-ids to claims" (atom feed/empty-feed-state))
(def user-state "Map of user-ids to user profiles" (atom {}))
(def score-state "Map of user-ids to user score data" (agent {}))

(defn load-score! [score] (send score-state assoc (score :user-id) score))
(defn load-user! [user] (swap! user-state assoc (user :id) user))
(defn load-claim! [claim] (swap! feed-state feed/add-claim claim))
(defn load-vote! [vote] (swap! feed-state feed/add-vote vote))

(defn create-score!
  "Create a score and return what was stored" ; internally-generated so no error-checking for now
  [s]
  (let [stored (db/create-score s)] (load-score! stored) stored))

(defn create-claim!
  "Create a claim and return what was stored, or an error message if invalid"
  [c]
  (or (feed/claim-error c)
      (let [stored (db/create-claim c)] (load-claim! stored) stored)))

(defn create-vote!
  "Create a claim and return what was stored, or an error message if invalid"
  [v]
  (or (feed/vote-error @feed-state v)
      (if (-> v (:user-id) (@score-state) (:karma) (= 0)) "Not enough karma")
      (let [stored (db/create-vote v)] (load-vote! stored) stored)))

(defn create-user! [user]
  (let [token (generate-token)
        stored (-> user (assoc :access-token token) (db/create-user))
        _ (create-score! (score/empty-user-score (stored :id)))]
    (load-user! stored)
    stored))

(defn update-score! [s] (load-score! (db/update-score s)))
(defn update-vote! [v] (or (feed/vote-error @feed-state v) (load-vote! (db/change-vote v))))
(defn update-claim-totals! [claim] (do (db/update-claim-totals claim) (load-claim! claim)))
(defn drop-claims-beyond-hours! [n] (swap! feed-state feed/claims-within-hours n))

(defn update-user-scores!
  [scores current-feed user-ids]
  (let [next-scores (score/update-user-scores scores current-feed user-ids)]
    (mapv (comp db/update-score (partial get next-scores)) user-ids)
    next-scores))

(defn finalize-claims!
  "Update claim vote totals and asynchronously update relevant user scores"
  []
  (let [finalized (feed/update-claim-totals @feed-state)
        current-feed (do (mapv update-claim-totals! finalized) @feed-state)
        voter-ids (apply union (mapv feed/claim-voters finalized))]
    (send score-state update-user-scores! current-feed voter-ids)))

(defn get-user [id] (get @user-state id))
(defn get-user-votes [user-id] (feed/user-votes @feed-state user-id))

(defn get-user-score [user-id]
  (let [user-score (get @score-state user-id) current-feed @feed-state]
    (if (score/revive-user? user-score current-feed)
      (do (send score-state score/revive-user-score user-id)
          (await score-state)
          (get-user-score user-id))
      (select-keys user-score [:latest-scored-claim-id :karma :streak :user-id]))))

(defn get-user-profile [user-id]
  (-> (get-user user-id)
      (dissoc :access-token)
      (merge {:score (get-user-score user-id)
              :votes (get-user-votes user-id)})))
