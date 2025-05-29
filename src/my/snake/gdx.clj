(ns my.snake.gdx
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture]
   [com.badlogic.gdx.graphics.g2d BitmapFont Sprite SpriteBatch]
   [com.badlogic.gdx.math MathUtils Rectangle Vector2]
   [com.badlogic.gdx.utils ScreenUtils])
  (:gen-class))

(def f float)

(defn bitmap-font
  [{:keys [color scale]}]
  (let [font (BitmapFont.)]
    (when color (.setColor font color))
    (when scale (.setScale (.getData font) (f scale)))
    font))

(defn center-x
  "returns the x of the body so that it's center become cx"
  [cx {:keys [w]}]
  (- cx (/ w 2)))

(defn center-y
  "returns the y of the body so that it's center become cy"
  [cy {:keys [h]}]
  (- cy (/ h 2)))

(defn clear
  ([] (clear Color/BLACK))
  ([color] (ScreenUtils/clear color)))

(defn random
  [min max]
  (MathUtils/random (f min) (f max)))

(defn rectangle
  ([{:keys [x y w h]}] (rectangle x y w h))
  ([x y w h]
   (Rectangle. (f x) (f y) (f w) (f h))))

(defn sound
  [file & {:keys [loop? play? volume]}]
  (let [s (->> (.internal Gdx/files file)
               (.newMusic Gdx/audio))]
    (when loop? (.setLooping s true))
    (when volume (.setVolume s volume))
    (when play? (.play s))
    s))

(defn sprite
  [texture & {:keys [x y w h]}]
  (let [s (Sprite. texture)]
    (.setSize s w h)
    (when x (.setX s x))
    (when y (.setY s y))
    s))

(defn sprite-batch [] (SpriteBatch.))

(defn texture [source] (Texture. source))

(defn pixmap
  [^long width ^long height ^Color color]
  (let [pixmap (Pixmap. width height Pixmap$Format/RGBA8888)]
    (when color
      (.setColor pixmap color)
      (.fill pixmap))
    pixmap))

(defn color-texture
  "Creates a LibGDX Texture of the given width and height filled
  with the specified color."
  [^long width ^long height ^Color color]
  (let [pix (pixmap width height color)
        texture (texture pix)]
    (.dispose pix)
    texture))

(defn vector2 [x y] (Vector2. x y))


