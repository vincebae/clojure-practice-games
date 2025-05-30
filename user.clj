(ns user "Scratch pad for dev")

(require '[clj-commons.pretty.repl :as p]
         '[clojure.java.io :as io]
         '[clojure.pprint :refer [pprint]]
         '[clojure.string :as s]
         '[clojure.test :as test]
         '[dev :refer [game restart!]]
         '[my.app :refer [exit-game]])

game
(:config @game)
(deref (:state @game))
(deref (:resources @game))
(restart!)
(exit-game @game)


