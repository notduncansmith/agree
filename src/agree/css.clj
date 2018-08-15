(ns agree.css
  (:require [garden.core :refer [css]]))

(def dark-gray "#4e4e4e")
(def medium-gray "#6e6e6e")
(def light-gray "#aaa")
(def medium-green "#1d961b")
(def bright-blue "#00f")

(def body-text-color dark-gray)
(def profile-text-color medium-gray)

(def losing-score-color light-gray)
(def winning-score-color bright-blue)
(def primary-button-color medium-green)

(def base-styles
  [ [:* {:box-sizing "border-box"}]
    [:body {:margin 0}]
    [:body :input :textarea :span
      {:line-height 1.5 :font-size "1.2rem" :color body-text-color}]
    [:.page
      {:max-width "400px" :margin "auto" :padding "0 20px" :text-align "center"}]])

(def header
  [:.header
    [:h1 {:font-size "7rem" :line-height 1 :margin-bottom "10px"}]
    [:p.profile {:color profile-text-color :font-family "sans-serif"}]])

(def login-form
  [:.login-form {:margin-top "25px" :opacity 0}
    [:input {:height "2rem"}
      [:&.token {:width "80%" :border (str "1px solid" light-gray) :font-size "1rem"}]
      [:&.submit {:background-color "#fff" :border "none" :color medium-green}]]])


(def new-claim-form
  [:.new-claim-form {:text-align "left"}
    [:input :textarea {:width "100%"}]
    [:textarea {:border "none" :margin-bottom "10px"}]
    [:input.submit
      {:background-color primary-button-color
       :color "#fff"
       :border "none"
       :cursor "pointer"}]])

(def newsfeed
  [:.newsfeed
    [:h2 {:text-align "left"}]])

(def claim
  [:.claim
    {:position "relative"
     :width "100%"
     :border "1px solid #ddd"
     :text-align "left"
     :padding "15px"
     :margin-bottom "20px"
     :box-shadow "1px 1px 5px #bbb"}
    [:.text {:margin "10px" :word-wrap "break-word"}]
    [:.vote-buttons :.scores
      {:position "absolute" :top "5px" :right "5px" :font-family "sans-serif"}]
    [:&.open
      [:.vote {:display "inline-block"}
        [:.vote-button.voted
          {:background-color "transparent" :border "none" :font-size "14px"}]]]
    [:&.closed
      [:.scores
        [:.winning {:color winning-score-color}]
        [:.losing {:color losing-score-color}]]]])

(def app-styles (into base-styles [header login-form new-claim-form newsfeed claim]))
(def app-css (apply str (mapv css app-styles)))
