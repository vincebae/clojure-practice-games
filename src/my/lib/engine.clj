(ns my.lib.engine
  "Core game engine using LibGdx."
  (:require [my.lib.gdx :as g])
  (:import
   [com.badlogic.gdx ApplicationListener Files$FileType Gdx InputProcessor]
   [com.badlogic.gdx.backends.lwjgl3
    Lwjgl3Application
    Lwjgl3ApplicationConfiguration]
   [com.badlogic.gdx.utils.viewport FitViewport])
  (:gen-class))

(defonce state (atom {}))
(defonce resources (atom {}))

(defn as!
  "Convenience function to assoc / assoc-in value to state"
  [ks v]
  (if (coll? ks)
    (swap! state assoc-in ks v)
    (swap! state assoc ks v)))

(defn ms!
  "Convenience function to merge value to state"
  [v]
  (swap! state merge v))

(defn us!
  "Convenience function to update / update-in value in state"
  [ks f & args]
  (if (coll? ks)
    (apply swap! state update-in ks f args)
    (apply swap! state update ks f args)))

(defn gs
  "Convenience function to get / get-in value in state"
  [ks]
  (if (coll? ks)
    (get-in @state ks)
    (get @state ks)))

(defn gr
  "Convenience function to get / get-in value in resources"
  [ks]
  (if (coll? ks)
    (get-in @resources ks)
    (get @resources ks)))

(defn add-event!
  "Convenience function to add an event to state"
  [new-event]
  (when (some? new-event) (us! :events #(conj (vec %) new-event))))

(defn- create-input-processor
  [process-input-fn]

  (letfn
   [(process-input
      [mode data]
      (add-event! (process-input-fn mode data))
      true)

    (world-pos
      [x y]
      (let [pos-vector (.unproject (gr :viewport) (g/vector2 x y))]
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
  [{:keys [w h
           create-fn resize-fn update-fn render-fn pause-fn resume-fn dispose-fn
           process-input-fn]}]
  (reify ApplicationListener
    (create
      [_]
      (println "Create game")
      (let [{new-state :state new-resources :resources} (create-fn)]
        (reset! state (merge {:events []
                              :exit? false}
                             new-state))
        (reset! resources (merge {:batch (g/sprite-batch)
                                  :viewport (FitViewport. w h)}
                                 new-resources)))
      (->> (create-input-processor process-input-fn)
           (.setInputProcessor Gdx/input)))

    (resize
      [_ width height]
      (println "Resized, width:" width "height:" height)
      (.update (gr :viewport) width height true)
      (when resize-fn (resize-fn)))

    (render
      [_]
      (when (gs :exit?)
        (println "Shutting down application...")
        (.exit Gdx/app))
      (println "FPS:" (.getFramesPerSecond Gdx/graphics))

      (let [batch (gr :batch)
            viewport (gr :viewport)
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

        (render-fn @state {:batch batch
                           :world-width world-width
                           :world-height world-height})

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
    (.setForegroundFPS 1000)))
    ; (.setWindowIcon Files$FileType/Internal
    ;                 (into-array String ["favicon-32x32.png" "favicon-16x16.png"]))))

(defn start
  [config]
  (println "Start game: " (:title config))
  (Lwjgl3Application. (create-listener config) (default-config config)))

(defn exit [] (as! :exit? true))
