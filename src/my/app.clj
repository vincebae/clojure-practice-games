(ns my.app
  (:gen-class))

(defonce game (atom nil))

(defn choose-game
  [name]
  (case name
    "drop" 'my.game.drop-my
    "drop-gdx" 'my.game.drop-gdx
    'my.game.drop-my))

(defn start-game
  [namespace]
  (require [namespace])
  (apply (ns-resolve (find-ns namespace) (symbol "start-game")) []))

(defn exit-game
  ([] (exit-game @game))
  ([namespace]
   (require [namespace])
   (apply (ns-resolve (find-ns namespace) (symbol "exit-game")) [])))

(defn change-game
  [new-game]
  (let [[old new] (reset-vals! game (choose-game new-game))]
    (println "Changing game from" old "to" new)
    (exit-game old)))

(defn game-state
  []
  (require [@game])
  (deref (ns-resolve (find-ns @game) (symbol "state"))))

(defn -main [& _args]
  (when-not @game
    (let [g (choose-game (first _args))]
      (println "Playing: " (str g))
      (reset! game g)
      (println "game:" game)))
  (start-game @game))
