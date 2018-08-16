(ns agree.http
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.coercions :refer [as-int]]
            [clojure.string :as s]
            [clojure.data.json :as json]
            [clojure.set :refer [union]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [agree.state :as state]
            [agree.newsfeed :as feed]
            [agree.html :as html]
            [agree.util :refer [echo]]))

(defn send-redirect [location] {:status 302 :headers {"Location" location}})
(defn send-json [body]
  {:status 200 :headers {"Content-Type" "application/json"} :body (json/write-str body)})

(defn valid-user [token]
  (let [[id secret] (s/split token #"\:") user (state/get-user (Integer. id))]
    (if (and (not= nil user) (= secret (user :access-token)))
      user
      (println "No user for token" token))))

(defn with-user-from [token f] (let [user (valid-user token)] (if user (f user) {:status 404})))
(defn with-user-id-from [token f] (with-user-from token (comp f :id)))

(def feed-cache (atom [{} (html/page (html/newsfeed-html []))]))
(defn next-feed-cache
  [[prev-data prev-html]]
  (let [feed-state @state/feed-state]
    (if (= prev-data feed-state)
      [prev-data prev-html]
      [feed-state (->> feed-state (vals) (html/newsfeed-html) (html/page))])))

(defroutes page-routes
  (GET "/" [] (send-redirect "/feed"))

  (GET "/feed" []
    (do (state/finalize-claims!)
        (second (swap! feed-cache next-feed-cache))))

  (GET "/profile" [token] (with-user-id-from token (comp send-json state/get-user-profile)))

  (GET "/register/~/:username" [username]
    (let [user (state/create-user! {:username username})
          token (str (user :id) ":" (user :access-token))]
      (html/page (html/token-html username token))))

  (POST "/vote/:claim-id/:direction" [token claim-id :<< as-int direction]
    (with-user-id-from token
      #(do (if (contains? (feed/claim-voters (get @state/feed-state claim-id)) %)
              (state/update-vote! {:user-id % :claim-id claim-id :direction direction})
              (state/create-vote! {:user-id % :claim-id claim-id :direction direction}))
           (send-redirect (str "/feed#claim-" claim-id)))))

  (POST "/claim" [text token :as req]
    (with-user-id-from token
      #(let [allowed-text (subs text 0 (min (count text) feed/max-claim-chars))]
          (println % allowed-text (state/create-claim! {:author-id % :text allowed-text}))
          (send-redirect "/feed"))))

  (route/not-found "Not Found :("))

(def app-routes (wrap-defaults page-routes api-defaults))
