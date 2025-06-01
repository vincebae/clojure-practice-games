(ns user "Scratch pad for dev")

(require '[clj-commons.pretty.repl :as p]
         '[clojure.core.async :as a]
         '[clojure.java.io :as io]
         '[clojure.pprint :refer [pprint]]
         '[clojure.string :as s]
         '[clojure.test :as test]
         '[dev :refer [game stop! restart!]]
         '[my.snake.core :refer [clear-dev-chan poll-dev-chan]])

game
(:config @game)
(deref (:state @game))
(deref (:resources @game))
(restart!)
(stop!)
(load-file "src/my/snake/core.clj")
(poll-dev-chan)
(clear-dev-chan)



