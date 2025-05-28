(ns my.lib.utils
  (:require [my.lib.engine :refer [gs gr]]))

(def f float)

(defn- calc-pos
  [{:keys [delta pos vel lower-bound upper-bound]}]
  (cond-> (+ pos (* vel delta))
    lower-bound (max lower-bound)
    upper-bound (min upper-bound)))

(defn calc-entity-pos
  "Calculate entity position based on the velocity and boundaries"
  [{:keys [body velocity] :as entity}
   delta-time
   {:keys [x-lower-bound x-upper-bound y-lower-bound y-upper-bound]}]
  (-> entity
      (assoc-in [:body :x]
                (calc-pos {:delta delta-time
                           :pos (:x body)
                           :vel (:x velocity)
                           :lower-bound x-lower-bound
                           :upper-bound (some-> x-upper-bound
                                                (- (:w body)))}))
      (assoc-in [:body :y]
                (calc-pos {:delta delta-time
                           :pos (:y body)
                           :vel (:y velocity)
                           :lower-bound y-lower-bound
                           :upper-bound (some-> y-upper-bound
                                                (- (:h body)))}))))

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
  (let [size (count (gs entities-key))]
    (run! #(draw-entity batch (conj (vec entities-key) %)) (range size))))

(defn draw-text
  [batch font-key {:keys [text x y]}]
  (let [font (gr font-key)]
    (.draw font batch text (f x) (f y))))

(defn play-sound
  [sound-key]
  (.play (gr sound-key)))

