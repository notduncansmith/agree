(ns agree.score-test
  (:require [clojure.test :refer :all]
            [agree.util :refer [hours-ago]]
            [agree.newsfeed :as feed]
            [agree.scoreboard :as score]))

(defn claim [id hours-old votes]
  (let [c {:id id :author-id id :text (str "claim-" id)
           :created-at (hours-ago hours-old) :votes votes}]
    (if (>= hours-old feed/claim-open-hours) (merge c (feed/claim-totals c)) c)))

(defn test-feed [& claims] (reduce feed/add-claim feed/empty-feed-state claims))

(deftest test-outcome-streaks
  (testing "Calculates the run lengths of a series of boolean values"
    (is (= [0 1 0 1 2 3] (score/outcome-streaks [0] [true false true true true])))
    (is (= [1 2 0 1 2 3] (score/outcome-streaks [1] [true false true true true])))))

(deftest test-streaks-karma
  (testing "Streaks sum to proper karma amounts"
    (is (= 20 (score/streaks-karma [1 2])))
    (is (= 30 (score/streaks-karma [1 2 0 1 2])))
    (is (= 60 (score/streaks-karma [1 2 0 1 2 3])))
    (is (= 80 (score/streaks-karma [1 2 3 4])))))

(deftest test-next-user-score
  (let [feed-state (test-feed (claim 2 2 {"up" #{1 3} "down" #{4}}))
        user-score (assoc (score/empty-user-score 3) :latest-scored-claim-id 1)
        loser-score (assoc (score/empty-user-score 4) :latest-scored-claim-id 1)]
    (testing "Starts with 100 karma"
      (is (= 100 (:karma user-score))))
    (testing "Increments the streak if the user wins"
      (is (= 1 (:streak (score/next-user-score user-score feed-state)))))
    (testing "Increases score by 20 if the user's streak reaches 2"
      (is (= 120 (-> user-score (assoc :streak 1) (score/next-user-score feed-state) (:karma)))))
    (testing "Increases score by 50 if the user's streak reaches 3 or higher"
      (is (= 150 (-> user-score (assoc :streak 2) (score/next-user-score feed-state) (:karma))))
      (is (= 160 (-> user-score (assoc :streak 3) (score/next-user-score feed-state) (:karma)))))
    (testing "Decreases score by 10 and resets streak to 0 if the user loses"
      (is (= 90 (-> loser-score (assoc :streak 1) (score/next-user-score feed-state) (:karma))))
      (is (= 0 (-> loser-score (assoc :streak 1) (score/next-user-score feed-state) (:streak)))))))

