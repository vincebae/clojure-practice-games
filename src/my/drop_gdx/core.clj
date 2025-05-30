(ns my.drop-gdx.core
  (:import [com.badlogic.gdx
            ApplicationListener
            Files$FileType
            Gdx
            Input$Keys]
           [com.badlogic.gdx.backends.lwjgl3
            Lwjgl3Application
            Lwjgl3ApplicationConfiguration]
           [com.badlogic.gdx.graphics Color Texture]
           [com.badlogic.gdx.graphics.g2d
            BitmapFont
            Sprite
            SpriteBatch]
           [com.badlogic.gdx.math MathUtils Rectangle Vector2]
           [com.badlogic.gdx.utils ScreenUtils]
           [com.badlogic.gdx.utils.viewport FitViewport])
  (:gen-class))

(defonce state (atom {}))

(def f float)
(def f0 (f 0))
(def f1 (f 1))

(defn default-config []
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle "LibGdx Drop")
    (.useVsync true)
    (.setForegroundFPS (-> (Lwjgl3ApplicationConfiguration/getDisplayMode)
                           (.-refreshRate)
                           (inc)))
    (.setForegroundFPS 1000)
    (.setWindowedMode 800 500)))
    ; (.setWindowIcon Files$FileType/Internal
    ;                 (into-array String ["favicon-32x32.png" "favicon-16x16.png"]))))

(defn create-listener []
  (let [background (atom nil)
        bucket (atom nil)
        bucket-rectangle (atom nil)
        bucket-sprite (atom nil)
        drop (atom nil)
        drop-rectangle (atom nil)
        drop-sprites (atom nil)
        drop-sound (atom nil)
        drop-timer (atom nil)
        score (atom nil)
        score-font (atom nil)
        music (atom nil)
        batch (atom nil)
        viewport (atom nil)
        touch-pos (atom nil)]

    (letfn [(new-droplet [drop-texture]
              (let [drop-width (f 100)
                    drop-height (f 100)
                    world-width (.getWorldWidth @viewport)
                    world-height (.getWorldHeight @viewport)
                    drop-sprite (Sprite. drop-texture)]
                (doto drop-sprite
                  (.setSize drop-width drop-height)
                  (.setX (MathUtils/random f0 (f (- world-width drop-width))))
                  (.setY world-height))
                drop-sprite))

            (input []
              (let [speed (float 400.0)
                    delta-time (.getDeltaTime Gdx/graphics)
                    delta-dist (* speed delta-time)]
                (cond
                  (.isKeyPressed Gdx/input Input$Keys/RIGHT) (.translateX @bucket-sprite delta-dist)
                  (.isKeyPressed Gdx/input Input$Keys/LEFT) (.translateX @bucket-sprite (- delta-dist))
                  (.isTouched Gdx/input) (do (.set @touch-pos (.getX Gdx/input) (.getY Gdx/input))
                                             (.unproject @viewport @touch-pos)
                                             (.setCenterX @bucket-sprite (.-x @touch-pos))))))

            (logic []
              (let [delta-time (.getDeltaTime Gdx/graphics)
                    world-width (.getWorldWidth @viewport);
                    world-height (.getWorldHeight @viewport)
                    bucket-width (.getWidth @bucket-sprite)
                    bucket-height (.getHeight @bucket-sprite)]
                (.setX @bucket-sprite (-> @bucket-sprite
                                          (.getX)
                                          (MathUtils/clamp f0 (- world-width bucket-width))))
                (.setY @bucket-sprite (-> @bucket-sprite
                                          (.getY)
                                          (MathUtils/clamp f0 (- world-height bucket-height))))

                (.set @bucket-rectangle
                      (.getX @bucket-sprite)
                      (.getY @bucket-sprite)
                      bucket-width
                      bucket-height)

                (let [alives (atom [])]
                  (doseq [drop-sprite @drop-sprites]
                    (.translateY drop-sprite (* (f -200) delta-time))
                    (.set @drop-rectangle
                          (.getX drop-sprite)
                          (.getY drop-sprite)
                          (.getWidth drop-sprite)
                          (.getHeight drop-sprite))
                    (let [out-of-bound? (< (.getY drop-sprite)
                                           (- (.getHeight drop-sprite)))
                          collided? (.overlaps @bucket-rectangle @drop-rectangle)]
                      (cond
                        collided? (do (.play @drop-sound)
                                      (swap! score inc))
                        out-of-bound? (swap! score dec)
                        :else (swap! alives conj drop-sprite))))
                  (reset! drop-sprites @alives))

                (swap! drop-timer + delta-time)
                (when (> @drop-timer f1)
                  (reset! drop-timer f0)
                  (swap! drop-sprites conj (new-droplet @drop)))))

            (draw []
              (ScreenUtils/clear Color/BLACK)
              (.apply @viewport)
              (let [world-width (.getWorldWidth @viewport);
                    world-height (.getWorldHeight @viewport)]
                (doto @batch
                  (.setProjectionMatrix (-> @viewport
                                            (.getCamera)
                                            (.-combined)))
                  (.begin))

                (.draw @batch @background f0 f0 world-width world-height)
                (.draw @bucket-sprite @batch)
                (doseq [drop-sprite @drop-sprites]
                  (.draw drop-sprite @batch))

                (-> @score-font (.getData) (.setScale (f 2)))
                (doto @score-font
                  (.setColor Color/YELLOW)
                  (.draw @batch (str @score) (f 10) (f (- world-height 10))))

                (.end @batch)))]

      (reify ApplicationListener
        (create [_]
          (println "Create!")
          (reset! batch (SpriteBatch.))
          (reset! viewport (FitViewport. 800 500))
          (reset! touch-pos (Vector2.))
          (reset! background (Texture. "background.png"))
          (reset! music (.newMusic Gdx/audio (.internal Gdx/files "music.mp3")))
          (doto @music
            (.setLooping true)
            (.setVolume 0.5)
            (.play))

          (reset! bucket (Texture. "bucket.png"))
          (reset! bucket-sprite (Sprite. @bucket))
          (.setSize @bucket-sprite (f 100) (f 100))
          (reset! bucket-rectangle (Rectangle.))

          (reset! drop (Texture. "drop.png"))
          (reset! drop-sprites [(new-droplet @drop)])
          (reset! drop-rectangle (Rectangle.))
          (reset! drop-timer f0)
          (reset! drop-sound (.newSound Gdx/audio (.internal Gdx/files "drop.mp3")))

          (reset! score 0)
          (reset! score-font (BitmapFont.)))

        (resize [_ width height]
          (println "width:" width "height:" height)
          (.update @viewport width height true))

        (render [_]
          (println "FPS:" (.getFramesPerSecond Gdx/graphics))
          (when (:exit? @state)
            (println "Shutting down application...")
            (.exit Gdx/app))

          (input)
          (logic)
          (draw))

        (pause [_])
        (resume [_])
        (dispose [_]
          (reset! state {}))))))

(defn init-game [] {})

(defn start-game [_]
  (Lwjgl3Application. (create-listener) (default-config)))

(defn exit-game [_] (swap! state assoc :exit? true))

