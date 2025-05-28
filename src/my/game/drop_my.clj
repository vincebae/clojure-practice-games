(ns my.game.drop-my
  (:require
   [clojure.core.match :refer [match]]
   [my.lib.engine :as engine]
   [my.lib.utils :as u]
   [my.lib.gdx :as g])
  (:import
   [com.badlogic.gdx Input$Keys]
   [com.badlogic.gdx.graphics Color]))

(def ^:private ^:const bucket-width 100)
(def ^:private ^:const bucket-height 100)
(def ^:private ^:const bucket-speed 400)

(def ^:private ^:const drop-width 100)
(def ^:private ^:const drop-height 100)
(def ^:private ^:const drop-speed -200)

(defn- create-game
  []
  {:resources {:texture {:background (g/texture "background.png")
                         :bucket (g/texture "bucket.png")
                         :drop (g/texture "drop.png")}

               :sound {:bgm (g/sound "music.mp3"
                                     {:loop? true :volumn 0.5 :play? true})
                       :drop (g/sound "drop.mp3")}

               :font {:score (g/bitmap-font {:color Color/YELLOW :scale 2})}}

   :state {:score 0
           :drop-timer 0
           :entities {:bucket {:body {:x 0 :y 0 :w bucket-width :h bucket-height}
                               :velocity {:x 0 :y 0}
                               :graphics {:texture :bucket}}
                      :drops []}}})

(defn- process-input
  [mode data]
  (letfn
   [(key-down
      [keycode]
      (condp = keycode
        Input$Keys/RIGHT [:move-right-start]
        Input$Keys/LEFT [:move-left-start]
        nil))
    (key-up
      [keycode]
      (condp = keycode
        Input$Keys/RIGHT [:move-right-end]
        Input$Keys/LEFT [:move-left-end]
        nil))]

    (case mode
      :key-down (key-down (:keycode data))
      :key-up (key-up (:keycode data))
      :touch-down [:touch (:x data) (:y data)]
      :touch-dragged [:touch (:x data) (:y data)]
      nil)))

(defn- handle-events
  [state _]
  (letfn
   [(set-bucket-velocity-x
      [state velocity]
      (assoc-in state [:entities :bucket :velocity :x] velocity))

    (set-bucket-x
      [state x]
      (assoc-in state [:entities :bucket :body :x] x))

    (handle-event
      [state event]
      (match event
        [:move-right-start] (set-bucket-velocity-x state bucket-speed)
        [:move-left-start] (set-bucket-velocity-x state (- bucket-speed))
        [:move-right-end] (set-bucket-velocity-x state 0)
        [:move-left-end] (set-bucket-velocity-x state 0)
        [:touch x _] (let [bucket-body (get-in state [:entities :bucket :body])
                           centered-x (g/center-x x bucket-body)]
                       (set-bucket-x state centered-x))
        :else nil))]
    (-> (reduce handle-event state (:events state))
        (assoc :events []))))

(defn- update-entities-pos
  [state {:keys [delta-time world-width]}]
  (let [bucket (get-in state [:entities :bucket])
        new-bucket (u/calc-entity-pos bucket
                                      delta-time
                                      {:x-lower-bound 0
                                       :x-upper-bound world-width})
        drops (get-in state [:entities :drops])
        new-drops (mapv #(u/calc-entity-pos % delta-time {}) drops)]
    (-> state
        (assoc-in [:entities :bucket] new-bucket)
        (assoc-in [:entities :drops] new-drops))))

(defn- update-timers
  [state {:keys [delta-time]}]
  (let [drop-timer (:drop-timer state)
        updated-drop-timer (+ drop-timer delta-time)
        new-drop-timer (if (< updated-drop-timer 1) updated-drop-timer 0)]
    (assoc state :drop-timer new-drop-timer)))

(defn- process-drops
  [state {:keys [world-width world-height]}]
  (let [bucket-body (get-in state [:entities :bucket :body])
        bucket-rect (g/rectangle bucket-body)]
    (letfn
     [(categorize-drop
        [{:keys [body]}]
        (cond
          (.overlaps bucket-rect (g/rectangle body)) :collided
          (neg? (+ (:y body) (:h body))) :out-of-bound
          :else :remaining))

      (new-droplet
        []
        {:body {:x (g/random 0 (- world-width drop-width))
                :y world-height
                :w drop-width
                :h drop-height}
         :velocity {:x 0 :y drop-speed}
         :graphics {:texture :drop}})]

      (let [drops (get-in state [:entities :drops])
            categorized (group-by categorize-drop drops)
            {:keys [remaining collided out-of-bound]} categorized
            new-drops (if (zero? (:drop-timer state))
                        (conj (vec remaining) (new-droplet))
                        (vec remaining))
            score (:score state)
            new-score (+ score (- (count collided) (count out-of-bound)))]
        (-> state
            (assoc-in [:entities :drops] new-drops)
            (assoc :collided? (some? collided))
            (assoc :score new-score))))))

(defn- update-state
  [state data]
  (-> state
      (handle-events data)
      (update-entities-pos data)
      (update-timers data)
      (process-drops data)))

(defn- render
  [state {:keys [batch world-width world-height]}]
  ;; play collision sound
  (when (:collided? state) (u/play-sound [:sound :drop]))
  ;; background
  (u/draw-texture batch [:texture :background]
                  {:x 0 :y 0 :w world-width :h world-height})
  ;; bucket
  (u/draw-entity batch [:entities :bucket])
  ;; droplets
  (u/draw-entities batch [:entities :drops])
  ;; score
  (u/draw-text batch [:font :score] {:text (str (:score state))
                                     :x 10
                                     :y (- world-height 10)}))

(defn start-game
  []
  (engine/start
   {:title "My Drop"
    :w 800 :h 500
    :create-fn create-game
    :update-fn update-state
    :render-fn render
    :process-input-fn process-input}))

(defn exit-game [] (engine/exit))

