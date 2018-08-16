(ns agree.util
  (:require [clojure.string :as s]))

(defn now [] (System/currentTimeMillis))
(defn hours-ago [n] (- (now) (bigint (* 360000.0 n))))
(defn hours-since [n] (float (/ (- (now) n) 360000)))
(defn index-by [f coll] "Map of {(f x) x}" (reduce #(assoc % (f %2) %2) {} coll))
(defn hyphenate-keyword [k] (keyword (s/replace (name k) #"\_" "-")))
(defn hyphenate-keys [m] (reduce-kv #(assoc % (hyphenate-keyword %2) %3) {} m))
(defn mvals "Monadic sorted-map vals" [c] (if (= (type c) clojure.lang.PersistentTreeMap) (vals c) c))
(defn generate-token [] (.toString (java.util.UUID/randomUUID)))
(defn echo [value] (do (println value) value))