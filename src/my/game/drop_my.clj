(ns my.game.drop-my
  (:require
   [clojure.core.match :refer [match]]
   [my.lib.engine :as engine]
   [my.lib.state :as st :refer [as! us! gs add-event!]]
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
  {:texture {:background (g/texture "background.png")
             :bucket (g/texture "bucket.png")
             :drop (g/texture "drop.png")}

   :sound {:bgm (g/sound "music.mp3" {:loop? true :volumn 0.5 :play? true})
           :drop (g/sound "drop.mp3")}

   :font {:score (g/bitmap-font {:color Color/YELLOW :scale 2})}

   :score 0
   :drop-timer 0
   :entities {:bucket {:body {:x 0 :y 0 :w bucket-width :h bucket-height}
                       :velocity {:x 0 :y 0}
                       :graphics {:texture :bucket}}
              :drops []}})

(defn- process-input
  [mode data]
  (letfn
   [(key-down
      [keycode]
      (condp = keycode
        Input$Keys/RIGHT (add-event! [:move-right-start])
        Input$Keys/LEFT (add-event! [:move-left-start])
        nil))
    (key-up
      [keycode]
      (condp = keycode
        Input$Keys/RIGHT (add-event! [:move-right-end])
        Input$Keys/LEFT (add-event! [:move-left-end])
        nil))]

   (case mode
     :key-down (key-down (:keycode data))
     :key-up (key-up (:keycode data))
     :touch-down (add-event! [:touch (:x data) (:y data)])
     :touch-dragged (add-event! [:touch (:x data) (:y data)])
     nil)))

(defn- handle-events!
  [{:keys [events]}]
  (doseq [event events]
    (match event
      [:move-right-start] (as! [:entities :bucket :velocity :x] bucket-speed)
      [:move-left-start] (as! [:entities :bucket :velocity :x] (- bucket-speed))
      [:move-right-end] (as! [:entities :bucket :velocity :x] 0)
      [:move-left-end] (as! [:entities :bucket :velocity :x] 0)
      [:touch x _] (let [bucket-body (gs [:entities :bucket :body])
                         centered-x (g/center-x x bucket-body)]
                     (as! [:entities :bucket :body :x] centered-x))
      :else nil)))

(defn- new-droplet
  [world-width world-height]
  {:body {:x (g/random 0 (- world-width drop-width))
          :y world-height
          :w drop-width
          :h drop-height}
   :velocity {:x 0 :y drop-speed}
   :graphics {:texture :drop}})

(defn- update-entities!
  [{:keys [delta-time world-width]}]
  ;; bucket
  (st/update-entity-pos! [:entities :bucket] delta-time
                         {:x-lower-bound 0 :x-upper-bound world-width})
  ;; droplets
  (st/update-entities-pos! [:entities :drops] delta-time)
  ;; drop timer
  (st/update-timer! :drop-timer delta-time))

(defn- logic!
  [{:keys [world-width world-height]}]
  ;; check droplets behavior
  (let [bucket-rect (g/rectangle (gs [:entities :bucket :body]))
        categorized (atom nil)]
    (letfn
     [(categorize-drop
        [{:keys [body]}]
        (cond
          (.overlaps bucket-rect (g/rectangle body)) :collided
          (neg? (+ (:y body) (:h body))) :out-of-bound
          :else :normal))
      (check-drops
        [drops]
        ;; categorize drops into a map and return only normal drops vector
        (reset! categorized (group-by categorize-drop drops))
        (vec (:normal @categorized)))]
     ;; Remove collided and out-of-bound drops from the drop entities
     (us! [:entities :drops] check-drops))

    ;; play drop sound when collided and updated score
    (let [{:keys [collided out-of-bound]} @categorized]
      (when collided (.play (gs [:sound :drop])))
      (us! :score + (- (count collided) (count out-of-bound)))))

  ;; increase or reset drop timer and create a new droplet
  (when (> (gs :drop-timer) 1)
    (as! :drop-timer 0)
    (us! [:entities :drops] conj (new-droplet world-width world-height))))

(defn- draw!
  [{:keys [batch world-width world-height]}]
  ;; background
  (st/draw-texture batch [:texture :background]
                   {:x 0 :y 0 :w world-width :h world-height})
  ;; bucket
  (st/draw-entity batch [:entities :bucket])
  ;; droplets
  (st/draw-entities batch [:entities :drops])
  ;; score
  (st/draw-text batch [:font :score] {:text (str (gs :score))
                                      :x 10
                                      :y (- world-height 10)}))

(defn- render!
  [data]
  (handle-events! data)
  (update-entities! data)
  (logic! data)
  (draw! data))

(defn start-game
  []
  (engine/start
   {:title "My Drop"
    :w 800 :h 500
    :create-fn create-game
    :render-fn render!
    :process-input-fn process-input}))

(defn exit-game [] (engine/exit))

