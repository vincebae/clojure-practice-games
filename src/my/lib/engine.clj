(ns my.lib.engine
  "Core game engine using LibGdx."
  (:require
   [my.lib.gdx :as g]
   [my.lib.state :refer [as! events! gs ms! state]])
  (:import
   [com.badlogic.gdx ApplicationListener Files$FileType Gdx InputProcessor]
   [com.badlogic.gdx.backends.lwjgl3
    Lwjgl3Application
    Lwjgl3ApplicationConfiguration]
   [com.badlogic.gdx.utils.viewport FitViewport]))

(defn- world-pos
  [x y]
  (let [pos-vector (.unproject (gs :viewport) (g/vector2 x y))]
    {:x (.-x pos-vector) :y (.-y pos-vector)}))

(defn- create-input-processor
  [process-input-fn]

  (reify InputProcessor
    (keyDown
      [_ keycode]
      (process-input-fn :key-down {:keycode keycode})
      true)

    (keyUp
      [_ keycode]
      (process-input-fn :key-up {:keycode keycode})
      true)

    (keyTyped
      [_ character]
      (process-input-fn :key-typed {:character character})
      true)

    (touchDown
      [_ x y pointer button]
      (let [{world-x :x world-y :y} (world-pos x y)]
        (process-input-fn :touch-down {:x world-x
                                       :y world-y
                                       :pointer pointer
                                       :button button}))
      true)

    (touchUp
      [_ x y pointer button]
      (let [{world-x :x world-y :y} (world-pos x y)]
        (process-input-fn :touch-up {:x world-x
                                     :y world-y
                                     :pointer pointer
                                     :button button}))
      true)

    (touchDragged
      [_ x y pointer]
      (let [{world-x :x world-y :y} (world-pos x y)]
        (process-input-fn :touch-dragged {:x world-x
                                          :y world-y
                                          :pointer pointer}))
      true)

    (touchCancelled
      [_ x y pointer button]
      (let [{world-x :x world-y :y} (world-pos x y)]
        (process-input-fn :touch-cancelled {:x world-x
                                            :y world-y
                                            :pointer pointer
                                            :button button}))
      true)

    (mouseMoved
      [_ x y]
      (let [{world-x :x world-y :y} (world-pos x y)]
        (process-input-fn :mouse-moved {:x world-x :y world-y}))
      true)

    (scrolled
      [_ amount-x amount-y]
      (process-input-fn :scrolled {:amount-x amount-x :amount-y amount-y})
      true)))

(defn- create-listener
  [{:keys [w h
           create-fn resize-fn render-fn pause-fn resume-fn dispose-fn
           process-input-fn]}]
  (reify ApplicationListener
    (create
      [_]
      (println "Create game")
      (reset! state {:batch (g/sprite-batch)
                     :events []
                     :exit? false
                     :viewport (FitViewport. w h)})
      (->> (create-input-processor process-input-fn)
           (.setInputProcessor Gdx/input))
      (ms! (create-fn)))

    (resize
      [_ width height]
      (println "Resized, width:" width "height:" height)
      (.update (gs :viewport) width height true)
      (when resize-fn (resize-fn)))

    (render
      [_]
      (when (gs :exit?)
        (println "Shutting down application...")
        (.exit Gdx/app))
      (println "FPS:" (.getFramesPerSecond Gdx/graphics))

      (let [batch (gs :batch)
            viewport (gs :viewport)]
        (g/clear)
        (.apply viewport)
        (doto batch
          (.setProjectionMatrix (-> viewport (.getCamera) (.-combined)))
          (.begin))

        (render-fn {:batch batch
                    :delta-time (.getDeltaTime Gdx/graphics)
                    :events (events!)
                    :world-width (.getWorldWidth viewport)
                    :world-height (.getWorldHeight viewport)})

        (.end batch)))

    (pause
      [_]
      (println "Paused")
      (when pause-fn (pause-fn)))

    (resume
      [_]
      (println "Resumed")
      (when resume-fn (resume-fn)))

    (dispose
      [_]
      (println "Disposing")
      (reset! state {})
      (when dispose-fn (dispose-fn)))))

(defn- default-config
  [{:keys [title w h]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setWindowedMode w h)
    (.useVsync true)
    ; (.setForegroundFPS (-> (Lwjgl3ApplicationConfiguration/getDisplayMode)
    ;                        (.-refreshRate)
    ;                        (inc)))
    (.setForegroundFPS 1000)
    (.setWindowIcon Files$FileType/Internal
                    (into-array String ["favicon-32x32.png" "favicon-16x16.png"]))))

(defn start
  [config]
  (println "Start game: " (:title config))
  (Lwjgl3Application. (create-listener config) (default-config config)))

(defn exit [] (as! :exit? true))
