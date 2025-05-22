(ns my.app
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration]
           [com.badlogic.gdx.graphics GL20]))

(defn create-listener []
  (reify ApplicationListener
    (create [_]
      (println "Game started!"))

    (resize [_ width height]
      (println "Window resized to:" width "x" height))

    (render [_]
      (doto Gdx/gl
        (.glClearColor 0.2 0.4 0.6 1)
        (.glClear GL20/GL_COLOR_BUFFER_BIT)))

    (pause [_])
    (resume [_])
    (dispose [_])))

(defn render []
  (let [config (doto (Lwjgl3ApplicationConfiguration.)
                 (.setTitle "Hello LibGDX")
                 (.setWindowedMode 800 600))]
    (Lwjgl3Application. (create-listener) config)))

(defn -main [& args]
  (render))
