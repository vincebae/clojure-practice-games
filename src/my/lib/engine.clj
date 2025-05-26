(ns my.lib.engine
  (:import
   [com.badlogic.gdx ApplicationListener Files$FileType Gdx]
   [com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration]
   [com.badlogic.gdx.utils.viewport FitViewport]))

(defn- create-listener
  [state
   {:keys [create-game
           input-processor
           handle-events
           update-entities
           logic
           draw
           dispose]}]
  (reify ApplicationListener
    (create [_]
      (reset! state {:exit? false
                     :viewport (FitViewport. 800 500)})
      (create-game)
      (.setInputProcessor Gdx/input (input-processor)))

    (resize [_ width height]
      (println "width:" width "height:" height)
      (.update (get @state :viewport) width height true))

    (render [_]
      ; (println "FPS:" (.getFramesPerSecond Gdx/graphics))
      (when (get @state :exit?)
        (println "Shutting down application...")
        (.exit Gdx/app))

      (handle-events)
      (update-entities)
      (logic)
      (draw))

    (pause [_])
    (resume [_])
    (dispose [_] (dispose))))

(defn- default-config
  [{:keys [title w h]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.useVsync true)
    (.setForegroundFPS (-> (Lwjgl3ApplicationConfiguration/getDisplayMode)
                           (.-refreshRate)
                           (inc)))
    (.setForegroundFPS 1000)
    (.setWindowedMode w h)
    (.setWindowIcon Files$FileType/Internal
                    (into-array String ["favicon-32x32.png" "favicon-16x16.png"]))))

(defn start
  [state fns config]
  (println "Start game")
  (Lwjgl3Application. (create-listener state fns)
                      (default-config config)))
