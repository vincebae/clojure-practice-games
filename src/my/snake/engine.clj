(ns my.snake.engine
  "Core game engine using LibGdx."
  (:require [my.snake.gdx :as g])
  (:import
   [com.badlogic.gdx ApplicationListener Files$FileType Gdx InputProcessor]
   [com.badlogic.gdx.backends.lwjgl3
    Lwjgl3Application
    Lwjgl3ApplicationConfiguration]
   [com.badlogic.gdx.utils.viewport FitViewport])
  (:gen-class))

(defn- create-input-processor
  [process-input-fn resources state]

  (letfn
   [(process-input
      [mode data]
      (let [event (process-input-fn mode data)]
        (when (some? event)
          (swap! state update :events #(conj (vec %) event))))
      true)

    (world-pos
      [x y]
      (let [viewport (get @resources :viewport)
            pos-vector (.unproject viewport (g/vector2 x y))]
        {:x (.-x pos-vector) :y (.-y pos-vector)}))]

    (reify InputProcessor
      (keyDown
        [_ keycode]
        (process-input :key-down {:keycode keycode}))

      (keyUp
        [_ keycode]
        (process-input :key-up {:keycode keycode}))

      (keyTyped
        [_ character]
        (process-input :key-typed {:character character}))

      (touchDown
        [_ x y pointer button]
        (let [{world-x :x world-y :y} (world-pos x y)]
          (process-input :touch-down {:x world-x
                                      :y world-y
                                      :pointer pointer
                                      :button button})))

      (touchUp
        [_ x y pointer button]
        (let [{world-x :x world-y :y} (world-pos x y)]
          (process-input :touch-up {:x world-x
                                    :y world-y
                                    :pointer pointer
                                    :button button})))

      (touchDragged
        [_ x y pointer]
        (let [{world-x :x world-y :y} (world-pos x y)]
          (process-input :touch-dragged {:x world-x
                                         :y world-y
                                         :pointer pointer})))

      (touchCancelled
        [_ x y pointer button]
        (let [{world-x :x world-y :y} (world-pos x y)]
          (process-input :touch-cancelled {:x world-x
                                           :y world-y
                                           :pointer pointer
                                           :button button})))

      (mouseMoved
        [_ x y]
        (let [{world-x :x world-y :y} (world-pos x y)]
          (process-input :mouse-moved {:x world-x :y world-y})))

      (scrolled
        [_ amount-x amount-y]
        (process-input :scrolled {:amount-x amount-x :amount-y amount-y})))))

(defn- create-listener
  [{:keys [config resources state]}]

  (reify ApplicationListener
    (create
      [_]
      (let [{:keys [w h resources-fn state-fn process-input-fn]} config]
        (println "Create game")
        (reset! state (-> (state-fn)
                          (assoc :events []
                                 :exit? false)))
        (reset! resources (-> (resources-fn)
                              (assoc :batch (g/sprite-batch)
                                     :viewport (FitViewport. w h))))
        (->> (create-input-processor process-input-fn resources state)
             (.setInputProcessor Gdx/input))))

    (resize
      [_ width height]
      (println "Resized, width:" width "height:" height)
      (.update (:viewport @resources) width height true)
      (when-let [resize-fn (:resize-fn config)]
        (resize-fn)))

    (render
      [_]
      (when (:exit? @state)
        (println "Shutting down application...")
        (.exit Gdx/app))
      (println "FPS:" (.getFramesPerSecond Gdx/graphics))

      (let [{:keys [update-fn render-fn]} config
            batch (:batch @resources)
            viewport (:viewport @resources)
            delta-time (.getDeltaTime Gdx/graphics)
            world-width (.getWorldWidth viewport)
            world-height (.getWorldHeight viewport)]

        (swap! state update-fn {:delta-time delta-time
                                :world-width world-width
                                :world-height world-height})

        (g/clear)
        (.apply viewport)
        (doto batch
          (.setProjectionMatrix (-> viewport (.getCamera) (.-combined)))
          (.begin))

        (render-fn @state @resources)

        (.end batch)))

    (pause
      [_]
      (println "Paused")
      (when-let [pause-fn (:pause-fn config)]
        (pause-fn)))

    (resume
      [_]
      (println "Resumed")
      (when-let [resume-fn (:resume-fn config)]
        (resume-fn)))

    (dispose
      [_]
      (println "Disposing")
      (reset! state {})
      (when-let [dispose-fn (:dispose-fn config)]
        (dispose-fn)))))

(defn- default-config
  [{:keys [title w h]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setWindowedMode w h)
    (.useVsync true)
    ; (.setForegroundFPS (-> (Lwjgl3ApplicationConfiguration/getDisplayMode)
    ;                        (.-refreshRate)
    ;                        (inc)))
    (.setForegroundFPS 250)))
    ; (.setWindowIcon Files$FileType/Internal
    ;                 (into-array String ["favicon-32x32.png" "favicon-16x16.png"]))))

(defn start
  [game]
  (let [config (:config game)]
    (println "Start game: " (:title config))
    (Lwjgl3Application. (create-listener game) (default-config config))))

(defn exit
  [game]
  (swap! (:state game) assoc :exit? true))
