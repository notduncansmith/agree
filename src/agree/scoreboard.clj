(ns agree.scoreboard
  "Logic for calculating scores and managing scoreboard state"
  (:require [agree.newsfeed :as feed]))

(def user-revival-period "Hours a user must wait to receive more karma to vote with" 24)
(defn sufficient-karma-to-vote? [score] (> (score :karma) 0))

(defn empty-user-score [id] {:user-id id :latest-scored-claim-id 0 :streak 0 :karma 100})

(defn streak-karma
  "The amount of karma earned for a streak of length n"
  [n]
  (condp = n 0 -10, 1 0, 2 20, 30))

(defn streaks-karma
  "The amount of karma earned over a series of progressive streak lengths"
  [streaks]
  (reduce #(+ % (streak-karma %2)) 0 streaks))

(defn winning-direction
  "The winning vote direction, `\"up\"` or `\"down\"` (returns nil in the case of a tie)"
  [{fu :final-up fd :final-down}]
  (if (= fu fd) nil (if (> fu fd) "up" "down")))

(defn wins-claim?
  "Whether a user-id is in the claim's winning set of voters"
  [user-id claim]
  (let [direction (winning-direction claim)]
    (if direction (= (feed/user-claim-vote-direction user-id claim) direction))))

(defn outcome-streaks
  "The streak lengths after each in a series of outcomes, ignoring ties"
  [starting-streaks outcomes]
  (reduce #(if (nil? %2) %
               (conj % (if %2 (inc (or (peek %) 0)) 0)))
          starting-streaks
          outcomes))

(defn user-outcomes [user-id claims] (mapv #(wins-claim? user-id %) claims))

(defn revive-user?
  "Determine whether a user should be revived"
  [{:keys [latest-scored-claim-id karma streak user-id]} feed]
  (let [latest-scored-claim (get feed latest-scored-claim-id)]
    (and (= karma 0)
         (or (nil? latest-scored-claim) (> (feed/hours-since-created latest-scored-claim)
                                           (+ feed/claim-open-hours user-revival-period))))))

(defn revive-user-score [scores user-id] (assoc-in scores [user-id :karma] 20))

(defn next-user-score
  [{:keys [latest-scored-claim-id karma streak user-id] :as user-score} feed]
  (let [fresh-streak (= 0 latest-scored-claim-id)
        unscored-claims (->> feed (keys)
                          (take-while #(> % latest-scored-claim-id))
                          (reverse) ; newest-first -> oldest-first
                          (mapv feed))
        user-outcomes (user-outcomes user-id unscored-claims)
        user-streaks (outcome-streaks (if fresh-streak [] [streak]) user-outcomes)]
    (if (empty? unscored-claims) user-score
      {:latest-scored-claim-id (:id (first unscored-claims))
       :karma (+ karma (streaks-karma user-streaks))
       :streak (peek user-streaks)
       :user-id user-id})))

(defn update-user-scores [scoreboard feed user-ids]
  (reduce #(update % %2 next-user-score feed) scoreboard user-ids))
