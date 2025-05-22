(ns my.app
  (:require [cljfx.api :as fx]
            [my.components.event-handler :refer [event-handler *state]]
            [my.components.root-window :refer [root-window]])
  (:gen-class))


(def renderer
  (fx/create-renderer
    :middleware
    (fx/wrap-map-desc assoc :fx/type root-window)
    :opts
    {:fx.opt/map-event-handler event-handler}))

(defn render
  []
  (fx/mount-renderer *state renderer))

(defn -main [& args]
  (render))

