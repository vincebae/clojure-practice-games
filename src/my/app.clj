(ns my.app
  (:require [my.game.drop :as drop]))

(defn -main [& _args]
  (drop/start-game))
