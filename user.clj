(ns user "Scratch pad for dev")

(require '[clj-commons.pretty.repl :as p]
         '[clojure.java.io :as io]
         '[clojure.pprint :refer [pprint]]
         '[clojure.string :as s]
         '[clojure.test :as test]
         '[dev :refer [game exit-game! restart!]])

game
(:config @game)
(deref (:state @game))
(deref (:resources @game))
(restart!)
(exit-game!)


