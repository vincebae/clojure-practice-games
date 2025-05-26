(ns my.game.drop-my
  (:require
   [clojure.core.match :refer [match]]
   [my.lib.engine :as engine]
   [my.lib.gdx :as g]
   [my.lib.nums :as n :refer [f f+ f- f* f0 f1 f2]]
   [my.lib.state :as st])
  (:import
   [com.badlogic.gdx Gdx Input$Keys InputAdapter]
   [com.badlogic.gdx.graphics Color]
   [com.badlogic.gdx.graphics.g2d BitmapFont SpriteBatch]
   [com.badlogic.gdx.math MathUtils]
   [com.badlogic.gdx.utils ScreenUtils]))

(defonce state (atom {}))

(defmacro as! [ks v] `(st/as! state ~ks ~v))
(defmacro ms! [v] `(st/ms! state ~v))
(defmacro us! [ks f & args] `(st/us! state ~ks ~f ~@args))
(defmacro gs [ks] `(st/gs state ~ks))
(defn add-event! [event] (us! :events conj event))
(defn events!
  []
  (let [[old _] (swap-vals! state assoc :events [])]
    (:events old)))

(def ^:private bucket-speed (f 400))
(def ^:private drop-speed (f -200))

(defn- create-game
  []
  (ms! {:exit? false
        :events []
        :batch (SpriteBatch.)
        :score 0
        :score-font (BitmapFont.)

        :texture {:background (g/texture "background.png")
                  :bucket (g/texture "bucket.png")
                  :drop (g/texture "drop.png")}

        :sound {:bgm (g/sound "music.mp3" {:loop? true :volumn 0.5 :play? true})
                :drop (g/sound "drop.mp3")}

        :drop-timer f0
        :entities {:bucket {:body {:x 0 :y 0
                                   :w (f 100) :h (f 100)}
                            :velocity {:x f0 :y f0}
                            :graphics {:texture :bucket}}
                   :drops []}}))

(defn- input-processor
  []
  (proxy [InputAdapter] []

    (keyDown
      [keycode]
      (println "Key down:" keycode)
      (cond
        (= keycode Input$Keys/RIGHT) (add-event! [:move-right-start])
        (= keycode Input$Keys/LEFT) (add-event! [:move-left-start]))
      true)

    (keyUp
      [keycode]
      (println "Key up" keycode)
      (cond
        (= keycode Input$Keys/RIGHT) (add-event! [:move-right-end])
        (= keycode Input$Keys/LEFT) (add-event! [:move-left-end]))
      true)

    (touchDragged
      [x y pointer]
      (let [world-pos (.unproject (gs :viewport) (g/vector2 x y))]
        (add-event! [:touch (.-x world-pos) (.-y world-pos)])
        true))

    (touchDown
      [x y pointer button]
      (.touchDragged this x y pointer))))

(defn- handle-events
  []
  (doseq [event (events!)]
    (match [event]
      [[:move-right-start]] (as! [:entities :bucket :velocity :x]
                                 bucket-speed)
      [[:move-left-start]] (as! [:entities :bucket :velocity :x]
                                (- bucket-speed))
      [[:move-right-end]] (as! [:entities :bucket :velocity :x] 0)
      [[:move-left-end]] (as! [:entities :bucket :velocity :x] 0)
      [[:touch x _]] (let [world-width (.getWorldWidth (gs :viewport))
                           bucket-body (gs [:entities :bucket :body])]
                       (as! [:entities :bucket :body :x]
                            (-> (g/center-x x bucket-body)
                                (MathUtils/clamp f0 (f- world-width (:w bucket-body))))))
      :else nil)))

(defn- update-entities
  []
  (let [delta-time (.getDeltaTime Gdx/graphics)
        world-width (.getWorldWidth (gs :viewport))
        bucket-width (gs [:entities :bucket :body :w])]

    ;; Update position of bucket
    (let [bucket (gs [:entities :bucket])
          velocity (get-in bucket [:velocity :x])]
      (when (not (zero? velocity))
        (us! [:entities :bucket :body :x]
             #(-> (f+ % (* velocity delta-time))
                  (MathUtils/clamp f0 (f- world-width bucket-width))))))

    ;; Update positions of droplets
    (us! [:entities :drops]
         #(mapv (fn [drop]
                  (->> (get-in drop [:velocity :y])
                       (f* delta-time)
                       (f+ (get-in drop [:body :y]))
                       (assoc-in drop [:body :y])))
                %))

    ;; Update timer
    (us! :drop-timer + delta-time)))

(defn- new-droplet
  []
  (let [drop-width (f 100)
        drop-height (f 100)
        world-width (.getWorldWidth (gs :viewport))
        world-height (.getWorldHeight (gs :viewport))]
    {:body {:x (MathUtils/random f0 (f- world-width drop-width))
            :y world-height
            :w drop-width
            :h drop-height}
     :velocity {:x f0 :y drop-speed}
     :graphics {:texture :drop}}))

(defn- logic
  []
  (let [bucket-rect (g/rectangle (gs [:entities :bucket :body]))]
    (->> (gs [:entities :drops])
         (reduce
          (fn [acc x]
            (let [collided? (.overlaps bucket-rect (g/rectangle (:body x)))
                  out-of-bound? (< (get-in x [:body :y]) (- (get-in x [:body :h])))]
              (cond
                collided? (do (.play (gs [:sound :drop]))
                              (us! :score inc)
                              acc)
                out-of-bound? (do (us! :score dec)
                                  acc)
                :else (conj acc x))))
          [])
         (as! [:entities :drops])))

  (when (> (gs :drop-timer) f1)
    (as! :drop-timer f0)
    (us! [:entities :drops] conj (new-droplet))))

(defn- draw-entity
  [batch {:keys [body graphics]}]
  (let [{:keys [x y w h]} body
        texture (gs [:texture (:texture graphics)])]
    (.draw batch texture (f x) (f y) (f w) (f h))))

(defn- draw-text
  [batch {:keys [text color scale x y]}]
  (let [font (g/bitmap-font {:color color :scale scale})]
    (.draw font batch text x y)))

(defn- draw
  []
  (ScreenUtils/clear Color/BLACK)
  (.apply (gs :viewport))
  (let [batch (gs :batch)
        world-width (.getWorldWidth (gs :viewport))
        world-height (.getWorldHeight (gs :viewport))]
    (doto batch
      (.setProjectionMatrix (-> (gs :viewport) (.getCamera) (.-combined)))
      (.begin))

    ;; background
    (.draw batch (gs [:texture :background]) f0 f0 world-width world-height)

    ;; bucket
    (draw-entity batch (gs [:entities :bucket]))

    ;; droplets
    (doseq [drop (gs [:entities :drops])]
      (draw-entity batch drop))

    ;; score
    (draw-text batch {:text (str (gs :score))
                      :color Color/YELLOW :scale f2
                      :x (f 10) :y (f- world-height 10)})

    (.end batch)))

(defn- dispose
  []
  (reset! state {}))

(defn start-game
  []
  (engine/start state
                {:create-game create-game
                 :input-processor input-processor
                 :handle-events handle-events
                 :update-entities update-entities
                 :logic logic
                 :draw draw
                 :dispose dispose}
                {:title "My Drop" :w 800 :h 500}))

(defn exit-game [] (as! :exit? true))

