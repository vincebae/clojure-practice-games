(ns my.snake.utils
  (:gen-class))

(def f float)

(defn calc-entity-pos
  "Calculate entity position based on the velocity and boundaries"
  ([entity delta-time] (calc-entity-pos entity delta-time {}))
  ([{:keys [body velocity] :as entity}
    delta-time
    {:keys [x-lower x-upper y-lower y-upper]}]

   (letfn
    [(calc-pos
       [{:keys [pos vel lower upper]}]
       (cond-> (+ pos (* vel delta-time))
         lower (max lower)
         upper (min upper)))]

     (-> entity
         (assoc-in [:body :x]
                   (calc-pos {:pos (:x body)
                              :vel (:x velocity)
                              :lower x-lower
                              :upper (some-> x-upper
                                             (- (:w body)))}))
         (assoc-in [:body :y]
                   (calc-pos {:pos (:y body)
                              :vel (:y velocity)
                              :lower y-lower
                              :upper (some-> y-upper
                                             (- (:h body)))}))))))

(defn calc-entities-pos
  ([entities delta-time] (calc-entities-pos entities delta-time {}))
  ([entities delta-time boundary]
   (mapv #(calc-entity-pos % delta-time boundary) entities)))

(defn draw-texture
  [batch texture {:keys [x y w h]}]
  (if (and w h)
    (.draw batch texture (f x) (f y) (f w) (f h))
    (.draw batch texture (f x) (f y))))

(defn draw-text
  [batch font {:keys [text x y]}]
  (.draw font batch text (f x) (f y)))

(defn flatten-entities
  [entities]
  (->> (reduce
        (fn [acc [_ v]] (if (vector? v) (into acc v) (conj acc v)))
        []
        entities)
       (sort-by :draw-priority)
       (vec)))

(defn play-sound
  [sound]
  (.play sound))

