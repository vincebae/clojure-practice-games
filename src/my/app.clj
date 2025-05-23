; import com.badlogic.gdx.ApplicationListener;
; import com.badlogic.gdx.Gdx;
; import com.badlogic.gdx.Input.Keys;
; import com.badlogic.gdx.InputProcessor;
; import com.badlogic.gdx.audio.Music;
; import com.badlogic.gdx.audio.Sound;

(ns my.app
  (:import [com.badlogic.gdx
            ApplicationListener
            Files$FileType
            Gdx
            Input$Keys] ;InputProcessor
           ; [com.badlogic.gdx.audio Music Sound]
           [com.badlogic.gdx.backends.lwjgl3
            Lwjgl3Application
            Lwjgl3ApplicationConfiguration]
           [com.badlogic.gdx.graphics
            Color
            GL20
            Texture]
           [com.badlogic.gdx.graphics.g2d Sprite SpriteBatch]
           [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx.utils ScreenUtils]
           [com.badlogic.gdx.utils.viewport FitViewport]))

(defn default-config []
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle "Drop")
    (.useVsync true)
    (.setForegroundFPS (-> (Lwjgl3ApplicationConfiguration/getDisplayMode)
                           (.-refreshRate)
                           (inc)))
    (.setWindowedMode 800 500)
    (.setWindowIcon Files$FileType/Internal
                    (into-array String ["favicon-32x32.png" "favicon-16x16.png"]))))

(defn create-listener []
  (let [background (atom nil)
        bucket (atom nil)
        bucket-sprite (atom nil)
        drop (atom nil)
        drop-sound (atom nil)
        music (atom nil)
        batch (atom nil)
        viewport (atom nil)
        touch-pos (atom nil)]

    (reify ApplicationListener
      (create [_]
        (println "Create!")
        (reset! background (Texture. "background.png"))
        (reset! bucket (Texture. "bucket.png"))
        (reset! bucket-sprite (Sprite. @bucket))
        (.setSize @bucket-sprite (float 1) (float 1))
        (reset! drop (Texture. "drop.png"))
        ; (reset! drop-sound (.newSound (Gdx/audio) (.internal (Gdx/files) "drop.mp3")))
        ; (reset! music      (.newMusic (Gdx/audio) (.internal (Gdx/files) "music.mp3")))
        (reset! batch (SpriteBatch.))
        (reset! viewport (FitViewport. 8 5))
        (reset! touch-pos (Vector2.)))

      (resize [_ width height]
        (println "width:" width "height:" height)
        (.update @viewport width height true))

      (render [_]
        (letfn [(input []
                  (let [speed (float 0.5)
                        delta-time (.getDeltaTime Gdx/graphics)
                        delta-dist (* speed delta-time)]
                    (cond
                      (.isKeyPressed Gdx/input Input$Keys/RIGHT) (.translateX @bucket-sprite delta-dist)
                      (.isKeyPressed Gdx/input Input$Keys/LEFT) (.translateX @bucket-sprite (- delta-dist))
                      (.isKeyPressed Gdx/input Input$Keys/UP) (.translateY @bucket-sprite delta-dist)
                      (.isKeyPressed Gdx/input Input$Keys/DOWN) (.translateY @bucket-sprite (- delta-dist))
                      (.isTouched Gdx/input) (do (.set @touch-pos (.getX Gdx/input) (.getY Gdx/input))
                                                 (.unproject @viewport @touch-pos)
                                                 (.setCenterX @bucket-sprite (.-x @touch-pos))))))
                (logic [])
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
                    (.draw @batch @background (float 0) (float 0) world-width world-height)
                    (.draw @bucket-sprite @batch)
                    (.end @batch)))]

          (input)
          (logic)
          (draw)))

      (pause [_])
      (resume [_])
      (dispose [_]
        ;; Dispose resources
        (doseq [res [@background @bucket @drop @drop-sound @music @batch]]
          (when res (.dispose res)))))))

(defn render []
  (Lwjgl3Application. (create-listener) (default-config)))

(defn -main [& _args]
  (render))
