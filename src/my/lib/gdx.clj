(ns my.lib.gdx
  (:import
   [com.badlogic.gdx Gdx]
   [com.badlogic.gdx.graphics Texture]
   [com.badlogic.gdx.graphics.g2d BitmapFont Sprite SpriteBatch]
   [com.badlogic.gdx.math Rectangle Vector2]))

(defn bitmap-font
  [{:keys [color scale]}]
  (let [font (BitmapFont.)]
    (when color (.setColor font color))
    (when scale (.setScale (.getData font) scale))
    font))

(defn center-x
  "returns the x of the body so that it's center become cx"
  [cx {:keys [w]}]
  (- cx (/ w 2)))

(defn center-y
  "returns the y of the body so that it's center become cy"
  [cy {:keys [h]}]
  (- cy (/ h 2)))

(defn rectangle
  ([{:keys [x y w h]}] (rectangle x y w h))
  ([x y w h]
   (let [rect (Rectangle.)]
     (.set rect x y w h)
     rect)))

(defn sprite
  [texture & {:keys [x y w h]}]
  (let [s (Sprite. texture)]
    (.setSize s w h)
    (when x (.setX s x))
    (when y (.setY s y))
    s))

(defn sound
  [file & {:keys [loop? play? volume]}]
  (let [s (->> (.internal Gdx/files file)
               (.newMusic Gdx/audio))]
    (when loop? (.setLooping s true))
    (when volume (.setVolume s volume))
    (when play? (.play s))
    s))

(defn texture
  [filename]
  (Texture. filename))

(defn vector2 [x y] (Vector2. x y))


