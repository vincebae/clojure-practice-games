(ns my.lib.state
  "Library to define and manipulate game state"
  (:require
   [my.lib.gdx :as g]))

(defonce state (atom {}))

(def f float)

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

(defn add-event!
  "Convenience function to add an event to state"
  [event]
  (us! :events conj event))

(defn events!
  "Return all the events and clear them in state"
  []
  (-> (swap-vals! state assoc :events []) first :events))

(defn- calc-pos
  [{:keys [delta pos vel lower-bound upper-bound]}]
  (cond-> (+ pos (* vel delta))
    lower-bound (max lower-bound)
    upper-bound (min upper-bound)))

(defn- calc-entity-pos
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

(defn update-entity-pos!
  ([entity-key delta-time]
   (update-entity-pos! entity-key delta-time {}))
  ([entity-key delta-time boundary]
   (us! entity-key calc-entity-pos delta-time boundary)))

(defn update-entities-pos!
  ([entities-key delta-time]
   (update-entities-pos! entities-key delta-time {}))
  ([entities-key delta-time boundary]
   (us! entities-key
        (fn [entities]
          (mapv #(calc-entity-pos % delta-time boundary) entities)))))

(defn update-timer!
  [timer-key delta-time]
  (us! timer-key + delta-time))

(defn draw-texture
  [batch texture-key {:keys [x y w h]}]
  (let [texture (gs texture-key)]
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
  (let [font (gs font-key)]
    (.draw font batch text (f x) (f y))))

