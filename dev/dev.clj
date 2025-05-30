;;; Initially copied from https://github.com/damn/moon and modified.
(ns dev
  (:require [clj-commons.pretty.repl :as p]
            [clojure.core.async :refer [<! >!! chan go poll!]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [disable-reload! refresh]]
            [my.app]
            [nrepl.server :as nrepl])
  (:gen-class))

(disable-reload!)

(defonce nrepl-server (atom nil))
(defonce game (atom nil))

(def dev-chan (chan))

(defn- init-and-start-game!
  []
  (eval `(do
           (require 'my.app)
           (reset! game (eval (read-string "(my.app/init-game)")))
           ((eval (read-string "my.app/start-game")) @game))))

(defn exit-game!
  []
  (eval `(do
           (require 'my.app)
           ((eval (read-string "my.app/exit-game")) @game))))

(defn- handle-throwable! [t]
  (binding [*print-level* 3]
    (p/pretty-pst t 24))
  ;; Block thread until taken by `restart!`
  (>!! dev-chan :err))

(defn restart!
  []
  (go (case (poll! dev-chan)
        :err (do (println "\n=== RESTARTING ===")
                     ;; clearing channel message to unblock dev loop.
                 (<! dev-chan))
        (println "\n Application sill running! Cannot restart."))))

(defn- start-dev-loop!
  []
  (try (init-and-start-game!)
       (catch Throwable t
         (handle-throwable! t)))
  (loop []
    ;; called only channel is cleared
    (handle-throwable! (refresh :after 'dev/start-dev-loop!))
    (recur)))

(defn- start-nrepl-server!
  "Start nrepl server write the port to .nrepl-port file"
  []
  (let [server (nrepl/start-server)
        port (:port server)
        port-file (io/file ".nrepl-port")]
    (.deleteOnExit ^java.io.File port-file)
    (spit port-file port)
    server))

(defn -main [& _args]
  (println "Start dev main")
  (reset! nrepl-server (start-nrepl-server!))
  (println "Started nrepl server on port" (:port @nrepl-server))
  (start-dev-loop!))
