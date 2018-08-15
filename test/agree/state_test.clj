(ns agree.state-test
  (:require [clojure.test :refer :all]
            [agree.state :refer :all]
            [agree.core :refer [initialize!]]
            [agree.db :as db]
            [agree.newsfeed :as feed]
            [agree.scoreboard :as score]
            [agree.util :as util]
            [clojure.java.io :refer [delete-file]]))

(def test-user {:username "test-user"})
(def test-user-2 {:username "test-user-2"})
(def test-claim {:author-id 1 :text "test-text"})
(def test-vote {:user-id 2 :claim-id 1 :direction "up"})
(def test-score {:id 1 :user-id 1 :latest-scored-claim-id 1 :streak 1 :karma 100})
(def test-db-path "./resources/test.db")

(defn reset-state! []
  (alter-var-root #'agree.db/conn (fn [prev] (assoc prev :subname test-db-path)))
  (delete-file test-db-path)
  (reset! user-state {})
  (reset! feed-state feed/empty-feed-state)
  (initialize!))

(defn create-test-data! [& ks]
  (->> {:user #(create-user! test-user)
        :user-2 #(create-user! test-user-2)
        :claim #(create-claim! test-claim)
        :vote #(create-vote! test-vote)}
    (#(select-keys % ks))
    (vals)
    (mapv #(%))))

(deftest test-create-user
  (reset-state!)
  (let [result (create-user! test-user)
        stored (select-keys result [:id :username :access-token :created-at])]
    (testing "generates token" (is (not= nil (stored :access-token))))
    (testing "saves the user to the database" (is (= (db/all-users) [stored])))
    (testing "loads the user into user-state" (is (= @user-state {1 stored})))))

(deftest test-load-user
  (reset-state!)
  (create-test-data! :user)
  (let [users @user-state]
    (reset! user-state {})
    (load-user! (get users 1))
    (testing "loads the user into the user state" (is (= @user-state users)))))

(deftest test-create-claim
  (reset-state!)
  (create-test-data! :user :claim)
  (let [stored (first (db/claims-since (util/hours-ago 1)))]
    (testing "saves the claim to the db" (is (= (stored :text) "test-text")))
    (testing "adds the claim to the state" (is (= (get-in @feed-state [1 :text]) "test-text")))))

(deftest test-load-claim
  (reset-state!)
  (create-test-data! :user :claim)
  (reset! feed-state feed/empty-feed-state)
  (load-claim! (first (db/claims-since (util/hours-ago 1))))
  (testing "loads the claim into the feed state"
    (is (= (get-in @feed-state [1 :text]) "test-text"))))

(deftest test-add-new-vote
  (reset-state!)
  (create-test-data! :user :user-2 :claim :vote)
  (let [saved-votes (db/votes-since (util/hours-ago 1))
        feed-state-upvotes (get-in @feed-state [1 :votes "up"])]
    (testing "saves the vote to the db" (is (= 1 (get-in saved-votes [0 :claim-id]))))
    (testing "tracks the user's vote in the feed" (is (= #{2} feed-state-upvotes)))
    (testing "ignores votes from users with insufficient karma"
      (do (send score-state assoc-in [2 :karma] 0)
          (create-test-data! :claim)
          (is (= "Not enough karma" (create-vote! {:user-id 2 :claim-id 2 :direction "up"})))))))

(deftest test-add-existing-vote
  (reset-state!)
  (create-test-data! :user :user-2 :claim :vote)
  (reset! feed-state feed/empty-feed-state)
  (initialize!)
  (testing "loads the vote into the state" (is (= #{2} (get-in @feed-state [1 :votes "up"]))))
  (testing "with a different direction"
    (update-vote! (assoc test-vote :direction "down"))
    (let [saved-direction (get-in (db/votes-since (util/hours-ago 1)) [0 :direction])
          feed-votes (get-in @feed-state [1 :votes])]
      (testing "updates the vote in the database" (is (= "down" saved-direction)))
      (testing "updates the vote in the state" (is (= {"up" #{} "down" #{2}} feed-votes))))))

(deftest test-get-user-profile
  (reset-state!)
  (create-test-data! :user :user-2 :claim :vote)
  (let [profile (get-user-profile 2)]
    (testing "includes the user's loaded votes"
      (is (= [{:user-id 2 :claim-id 1 :direction "up"}] (profile :votes))))
    (testing "includes the user's score"
      (is (= {:user-id 2 :latest-scored-claim-id 0 :streak 0 :karma 100} (profile :score))))
    (testing "revives the user if necessary"
      (do (swap! feed-state assoc-in [1 :created-at]
            (util/hours-ago (+ 2 score/user-revival-period feed/claim-open-hours)))
          (send score-state update 2 merge {:karma 0 :latest-scored-claim-id 1})
          (await score-state)
          (is (= 20 (get-in (get-user-profile 2) [:score :karma])))))))

(deftest test-finalize-claims!
  (reset-state!)
  (let [created-at (util/hours-ago 2)]
    (create-test-data! :user :user-2)
    (load-claim! (merge test-claim {:id 1 :final-up nil :final-down nil :created-at created-at}))
    (load-vote! (merge test-vote {:id 1 :created-at (inc created-at)}))
    (finalize-claims!)
    (testing "updates relevant user score state" (= @score-state (sorted-map 1 test-score)))
    (testing "updates stored scores" (= (db/user-score 1) test-score))))

