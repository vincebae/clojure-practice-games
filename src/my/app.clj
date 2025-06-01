(ns my.app
  (:gen-class))

(def default-game "snake")

(defn init
  ([] (init default-game))
  ([game-name]
   (let [core-namespace (symbol (str "my." game-name ".core"))]
     (require [core-namespace])
     (let [init-fn (ns-resolve (find-ns core-namespace) (symbol "init-game"))]
       (assoc (init-fn) :namespace core-namespace)))))

(defn start
  [game]
  (let [core-namespace (:namespace game)]
    (require [core-namespace])
    (let [start-fn (ns-resolve (find-ns core-namespace) (symbol "start-game"))]
      (start-fn game))))

(defn stop
  [game]
  (let [core-namespace (:namespace game)]
    (require [core-namespace])
    (let [exit-fn (ns-resolve (find-ns core-namespace) (symbol "exit-game"))]
      (exit-fn game))))

(defn -main [& _args]
  (let [game-name (or (first _args) default-game)]
    (println "Playing: " game-name)
    (start (init game-name))))
