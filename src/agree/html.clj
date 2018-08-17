(ns agree.html
  (:require [hiccup.core :as h]
            [clojure.string :as s]
            [agree.css :refer [app-css]]
            [agree.newsfeed :as feed]
            [agree.scoreboard :as score]))

(def page-template (s/replace (slurp "./resources/index.html") #"\{style\}" app-css))

(defn page [content] (s/replace page-template #"\{content\}" content))
(defn form-value [nm v] [:input {:type "hidden" :name nm :value v}])

(defn open-claim [{:keys [id text created-at]}]
  [:div.claim.open {:id (str "claim-" id) :data-timestamp created-at}
    [:p.text text]
    [:div.vote-buttons
      [:form.vote.up {:method "POST" :action (str "/vote/" id "/up")}
        [:input {:type "hidden" :name "token"}]
        [:input.vote-button {:type "submit" :value "↑"}]]
      [:form.vote.down {:method "POST" :action (str "/vote/" id "/down")}
        [:input {:type "hidden" :name "token"}]
        [:input.vote-button {:type "submit" :value "↓"}]]]])

(defn closed-claim [{:keys [id text final-up final-down created-at winning-direction]}]
  [:div.claim.closed {:id (str "claim-" id) :data-timestamp created-at}
    [:span.scores
      [:span.up {:class (if (= winning-direction "up") "winning" "losing")}
        (str "&uarr;" final-up)]
      " "
      [:span.down {:class (if (= winning-direction "down") "winning" "losing")}
        (str "&darr;" final-down)]]
    [:p.text text]])

(def claim-form
  [:form.new-claim-form {:method "POST" :action "/claim"}
    [:textarea {:name "text"
                :placeholder (str "Make a new claim (max " feed/max-claim-chars " characters)")
                :minlength 10
                :required true
                :maxlength feed/max-claim-chars}]
    [:input {:type "hidden" :name "token"}]
    [:input.submit {:type "submit" :value "+ submit"}]])

(def login-form
  [:form.login-form {:action "#" :onsubmit "login(event)"}
    [:input.token {:id "token" :type "text" :placeholder "Paste your access token here"}]
    [:input.submit {:type "submit" :value "Log in"}]])

(def header [:div.header [:h1 "Agree"] login-form])

(defn newsfeed-html
  [unsafe-claims]
  (let [safe-claims (map #(update % :text h/h) unsafe-claims) ; escape HTML, prevent XSS
        open-claims (feed/open-claims safe-claims)
        closed-claims (feed/closed-claims safe-claims)]
    (h/html
      [:div.page
        header
        [:div.newsfeed
          claim-form
          [:h2 "Open Claims"]
          [:div.open-claims (map open-claim open-claims)]
          [:h2 "Closed Claims"]
          [:div.closed-claims (->> closed-claims
                                (map #(assoc % :winning-direction (score/winning-direction %)))
                                (map closed-claim))]]])))

(defn token-html [username token]
  (h/html [:div.token {}
            [:h1 (str username "'s access token:")]
            [:p [:em "This is the only time this will be shown."]]
            [:pre token]]))
