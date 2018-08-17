(ns agree.newsfeed
  "Operations on the state of the newsfeed"
  (:require [agree.util :refer [now hours-since mvals]]
            [clojure.set :refer [union]]))

(def empty-feed-state (sorted-map-by >))
(def claim-open-hours "Number of hours for which a claim can be voted on" 1)
(def min-claim-chars "Minimum characters required in claim text" 10)
(def max-claim-chars "Maximum characters allowed in claim text" 280)

(defn hours-since-created [claim] (-> (:created-at claim) (hours-since)))
(defn claims-within-hours [feed n] (take-while #(< (hours-since-created %) n) (mvals feed)))
(defn claims-since-id [feed id] (take-while #(> (:id %) id) (mvals feed)))
(defn open-claim? [claim] (< (hours-since-created claim) claim-open-hours))
(defn open-claims [feed] (take-while open-claim? (mvals feed)))
(defn closed-claims [feed] (drop-while open-claim? (mvals feed)))
(defn untotaled-claim? [claim] (nil? (:final-up claim)))
(defn closed-untotaled-claims [feed] (->> feed (mvals) (closed-claims) (take-while untotaled-claim?)))

(defn vote-count
  "The number of users who voted in `direction` (`\"up\"` or `\"down\"`) on `claim`"
  [claim direction]
  (count (get-in claim [:votes direction])))

(defn claim-voters
  "A set of ids of users who voted on `claim`"
  [claim]
  (union (get-in claim [:votes "up"]) (get-in claim [:votes "down"])))

(defn claim-totals
  "A map of vote totals for a claim"
  [claim]
  {:final-up (vote-count claim "up") :final-down (vote-count claim "down")})

(defn update-claim-totals
  "Update closed claims with their final vote totals"
  [feed]
  (->> feed (closed-untotaled-claims) (mapv #(merge % (claim-totals %)))))

(defn user-claim-vote-direction
  "If the user voted on a claim, returns the direction; otherwise, nil"
  [user-id claim]
  (condp contains? user-id
    (get-in claim [:votes "up"]) "up"
    (get-in claim [:votes "down"]) "down"
    #{user-id} nil))

(defn user-claim-vote
  "If the user voted on this claim, this returns a vote record; otherwise, nil"
  [user-id claim]
  (let [direction (user-claim-vote-direction user-id claim)]
    (if direction {:user-id user-id :claim-id (claim :id) :direction direction})))

(defn user-votes
  "Returns a vector of a user's votes on claims in the feed"
  [feed user-id]
  (->> feed (mvals)
            (mapv (partial user-claim-vote user-id))
            (filterv (partial not= nil))))

(defn add-claim
  "Add a claim to the feed"
  [feed claim]
  (->> claim
      (merge {:final-up nil :final-down nil :votes {"up" #{} "down" #{}}})
      (assoc {} (:id claim))
      (into feed)))

(defn add-vote
  "Add a vote to a claim in the feed"
  [feed {:keys [claim-id user-id direction created-at]}]
  (update-in feed [claim-id :votes]
    (condp = direction
      "up" #(merge % {"up" (conj (% "up") user-id) "down" (disj (% "down") user-id)})
      "down" #(merge % {"down" (conj (% "down") user-id) "up" (disj (% "up") user-id)})
      identity)))

(defn vote-error
  "Returns a string error message, or nil if `vote` is valid"
  [feed vote]
  (let [claim (get feed (vote :claim-id)) created-at (or (vote :created-at) (now))]
    (cond (= nil claim) "Could not find claim"
          (not (open-claim? claim)) "Claim is closed"
          (= (claim :author-id) (vote :user-id)) "Cannot vote for own claim"
          true nil)))

(defn claim-error
  "Returns a string error message, or nil if `claim` is valid"
  [claim]
  (cond (< (count (:text claim)) min-claim-chars) "Claim too short"
        (> (count (:text claim)) max-claim-chars) "Claim too long"
        true nil))
