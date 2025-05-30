# Clojure Practice Games

Collection of simple games written in clojure.

## Requirements

* [Clojure](https://clojure.org/guides/install_clojure)
* [Babashka](https://github.com/babashka/babashka#installation)
* [Java](https://openjdk.org/install/)

## How to run

```
$ bb run
```
or
```
$ bb run <game-name>
```

Current Game Lists:
* `snake` : Snake game. Default when game name is not provided.
* `drop-gdx` : Port of [LibGdx tutorial game](https://libgdx.com/wiki/start/a-simple-game)
* `drop` : Same as `drop-gdx` but implemented in more functional and data-oriented way

## Dev mode

Games can run in dev mode for easier REPL based development.
This dev mode is inspired by 
- [moon](https://github.com/damn/moon)
- [Clojure Workflow Reloaded](https://www.cognitect.com/blog/2013/06/04/clojure-workflow-reloaded)

Start dev mode using
```
$ bb dev
```

- [nREPL server](https://github.com/nrepl/nrepl) will be started with `.nrepl-port` file created.
- The default game (specified in `src/my/app.clj`) will be started.
- Any nREPL client can be used to interact with running game.
  - `bb nrepl` can also be used to start a nREPL client
- The game data, which may include config, resource, state and etc, will be stored to `dev/game` atom
  defined in `dev/dev.clj` file.
- When game window is closed, dev loop will automatically tries to reload all the changes and restart the game
- This can be triggered from REPL by evaluating `(dev/exit-game!)`
- When there is any exception, game will print out the error and wait to be restarted
- Once changes are made, game can be restarted from REPL by evaluating `(dev/restart!)`

`user.clj` in the top level directory contains various useful forms for this process.
