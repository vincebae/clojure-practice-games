(ns my.lib.utils
  (:require [my.lib.engine :refer [gs gr]]))

(def f float)

(defn calc-entity-pos
  "Calculate entity position based on the velocity and boundaries"
  [{:keys [body velocity] :as entity}
   delta-time
   {:keys [x-lower x-upper y-lower y-upper]}]

  (letfn
   [(calc-pos
      [{:keys [delta pos vel lower upper]}]
      (cond-> (+ pos (* vel delta))
        lower (max lower)
        upper (min upper)))]

    (-> entity
        (assoc-in [:body :x]
                  (calc-pos {:delta delta-time
                             :pos (:x body)
                             :vel (:x velocity)
                             :lower x-lower
                             :upper (some-> x-upper
                                            (- (:w body)))}))
        (assoc-in [:body :y]
                  (calc-pos {:delta delta-time
                             :pos (:y body)
                             :vel (:y velocity)
                             :lower y-lower
                             :upper (some-> y-upper
                                            (- (:h body)))})))))

(defn draw-texture
  [batch texture-key {:keys [x y w h]}]
  (let [texture (gr texture-key)]
    (.draw batch texture (f x) (f y) (f w) (f h))))

(defn draw-entity
  [batch entity-key]
  (let [{:keys [body graphics]} (gs entity-key)
        texture-key [:texture (:texture graphics)]]
    (draw-texture batch texture-key body)))

(defn draw-entities
  [batch entities-key]
  (let [size (count (gs entities-key))
        keys (map #(conj (vec entities-key) %) (range size))]
    (run! #(draw-entity batch %) keys)))

(defn draw-text
  [batch font-key {:keys [text x y]}]
  (let [font (gr font-key)]
    (.draw font batch text (f x) (f y))))

(defn play-sound
  [sound-key]
  (.play (gr sound-key)))

