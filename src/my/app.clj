(ns my.app
  (:require [clojure.string :as s])
  (:gen-class))

(defn greet [xs]
  (if (empty? xs)
    "Hello, World!"
    (str "Hello, " (s/join ", " xs) "!")))

(defn -main [& args]
  (println (greet args)))
