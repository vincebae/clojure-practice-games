;; scratch pad to collect useful debugging commands.
(ns scratch)

(load-file "src/my/dev.clj")
(my.dev/restart!)
(my.dev/restart-force!)

(load-file "src/my/app.clj")
my.app/game
(my.app/exit-game)
(my.app/change-game "drop")
(my.app/change-game "drop-gdx")

(load-file "src/my/lib/engine.clj")
my.lib.engine/state
my.lib.engine/resources

