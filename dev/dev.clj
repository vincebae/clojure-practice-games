;;; Copied from https://github.com/damn/moon and modified.
(ns dev 
  (:require [clj-commons.pretty.repl :as p]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [disable-reload! refresh]]
            [my.app]
            [nrepl.server :as nrepl])
  (:gen-class))

(disable-reload!)

(defonce nrepl-server (atom nil))
(defonce game (atom nil))
(defonce ^:private thrown (atom false))
(defonce ^:private ^Object obj (Object.))
(defonce ^:private refresh-error (atom nil))

(defn- init-and-start-game!
  []
  (eval `(do
           (require 'my.app)
           (reset! game (eval (read-string "(my.app/init-game)")))
           ((eval (read-string "my.app/start-game")) @game))))

(defn- handle-throwable! [t]
  (binding [*print-level* 3]
    (p/pretty-pst t 24))
  (reset! thrown t))

(defn- wait! []
  (locking obj
    (Thread/sleep 10)
    (println "\n\n>>> WAITING FOR RESTART <<<")
    (.wait obj)))

(defn restart!
  "Calls refresh on all namespaces with file changes and restarts the application."
  []
  (if @thrown
    (do
      (reset! thrown false)
      (locking obj
        (println "\n\n>>> RESTARTING <<<")
        (.notify obj)))
    (println "\n Application still running! Cannot restart.")))

(defn- start-dev-loop!
  []
  (try (init-and-start-game!)
       (catch Throwable t
         (handle-throwable! t)))
  (loop []
    (when-not @thrown
      (reset! refresh-error (refresh :after 'dev/start-dev-loop!))
      (handle-throwable! @refresh-error))
    (wait!)
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
