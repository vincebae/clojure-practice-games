;; scratch pad to collect useful debugging commands.
(ns scratch)

(load-file "src/my/dev.clj")
(my.dev/restart!)
(my.dev/restart-force!)

(load-file "src/my/app.clj")
my.app/game
(my.app/exit-game)

(load-file "src/my/snake/engine.clj")
my.snake.engine/state
my.snake.engine/resources

