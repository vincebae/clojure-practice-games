(ns my.drop.core
  (:require
   [clojure.core.match :refer [match]]
   [my.drop.engine :as engine]
   [my.drop.utils :as u]
   [my.drop.gdx :as g])
  (:import
   [com.badlogic.gdx Input$Keys]
   [com.badlogic.gdx.graphics Color])
  (:gen-class))

(def ^:private ^:const bucket-width 100)
(def ^:private ^:const bucket-height 100)
(def ^:private ^:const bucket-speed 400)

(def ^:private ^:const drop-width 100)
(def ^:private ^:const drop-height 100)
(def ^:private ^:const drop-speed -200)

(defn- create-game
  []
  {:resources {:texture {:background (g/texture "assets/background.png")
                         :bucket (g/texture "assets/bucket.png")
                         :drop (g/texture "assets/drop.png")}

               :sound {:bgm (g/sound "assets/music.mp3"
                                     {:loop? true :volumn 0.5 :play? true})
                       :drop (g/sound "assets/drop.mp3")}

               :font {:score (g/bitmap-font {:color Color/YELLOW :scale 2})}}

   :state {:score 0
           :drop-timer 0
           :move-stack '()
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
        Input$Keys/RIGHT [:move-start :right]
        Input$Keys/LEFT [:move-start :left]
        nil))

    (key-up
      [keycode]
      (condp = keycode
        Input$Keys/RIGHT [:move-end :right]
        Input$Keys/LEFT [:move-end :left]
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
   [(add-move-stack
      [state direction]
      (let [move-stack (:move-stack state)
            new-move-stack (if (some #{direction} move-stack)
                             move-stack
                             (conj move-stack direction))]
        (assoc state :move-stack new-move-stack)))

    (remove-move-stack
      [state direction]
      (->> (:move-stack state)
           (filter #(not= direction %))
           (assoc state :move-stack)))

    (handle-event
      [state event]
      (match event
        [:move-start direction] (add-move-stack state direction)
        [:move-end direction] (remove-move-stack state direction)
        [:touch x _] (let [bucket-body (get-in state [:entities :bucket :body])
                           centered-x (g/center-x x bucket-body)]
                       (assoc-in state [:entities :bucket :body :x] centered-x))
        :else state))

    (process-move-stack
      [state]
      (let [direction (first (:move-stack state))
            velocity (case direction
                       :right bucket-speed
                       :left (- bucket-speed)
                       0)]
        (assoc-in state [:entities :bucket :velocity :x] velocity)))]

   (-> (reduce handle-event state (:events state))
       (process-move-stack)
       (assoc :events []))))

(defn- update-entities-pos
  [state {:keys [delta-time world-width]}]
  (let [bucket (get-in state [:entities :bucket])
        new-bucket (u/calc-entity-pos bucket delta-time
                                      {:x-lower 0 :x-upper world-width})
        drops (get-in state [:entities :drops])
        new-drops (u/calc-entities-pos drops delta-time)]
    (-> state
        (assoc-in [:entities :bucket] new-bucket)
        (assoc-in [:entities :drops] new-drops))))

(defn- update-timers
  [state {:keys [delta-time]}]
  (assoc state
         :drop-timer
         (as-> (:drop-timer state) timer
           (+ timer delta-time)
           (if (< timer 1) timer 0))))

(defn- process-drops
  [state {:keys [world-width world-height]}]
  (let [bucket-body (get-in state [:entities :bucket :body])
        bucket-rect (g/rectangle bucket-body)]
    (letfn
     [(process-drops-helper
        [drops score]
        (reduce
         (fn [{:keys [score drops] :as acc}
              {:keys [body] :as drop}]
           (cond
             (.overlaps bucket-rect (g/rectangle body))
             {:collided? true :score (inc score) :drops drops}

             (neg? (+ (:y body) (:h body)))
             (assoc acc :score (dec score))

             :else
             (assoc acc :drops (conj drops drop))))
         {:collided? false :score score :drops []}
         drops))

      (new-droplet
        []
        {:body {:x (g/random 0 (- world-width drop-width))
                :y world-height
                :w drop-width
                :h drop-height}
         :velocity {:x 0 :y drop-speed}
         :graphics {:texture :drop}})]

     (let [drops (get-in state [:entities :drops])
           score (:score state)
           result (process-drops-helper drops score)
           drop-timer (:drop-timer state)
           new-drops (cond-> (:drops result)
                       (zero? drop-timer) (conj (new-droplet)))]
       (-> state
           (assoc-in [:entities :drops] new-drops)
           (assoc :collided? (:collided? result))
           (assoc :score (:score result)))))))

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

