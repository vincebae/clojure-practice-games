(ns my.snake.core
  (:require
   [clojure.core.match :refer [match]]
   [my.snake.engine :as engine]
   [my.snake.utils :as u]
   [my.snake.gdx :as g])
  (:import
   [com.badlogic.gdx Input$Keys]
   [com.badlogic.gdx.graphics Color])
  (:gen-class))

(def ^:private ^:const cols 32)
(def ^:private ^:const rows 24)
(def ^:private ^:const grid-size 24)
(def ^:private ^:const grid-width 768) ;; (* cols grid-size))
(def ^:private ^:const grid-height 576) ;; (* rows grid-size))
(def ^:private ^:const screen-width 800)
(def ^:private ^:const screen-height 600)
(def ^:private ^:const pillar-box 16) ;; (/ (- screen-width grid-width) 2)
(def ^:private ^:const letter-box 12) ;; (/ (- screen-height grid-height) 2)

(def ^:private ^:const move-timer-limit 0.15)
(def ^:private ^:const grow-timer-limit 1)
(def ^:private ^:const start-col 1)
(def ^:private ^:const start-row 12)

(defn- grid-coord
  "returns the coordinate of top left corner of the given grid in vector,
  e.g. [48 60] for row 1 / col 2"
  [col row]
  [(+ (* col grid-size) pillar-box)
   (+ (* row grid-size) letter-box)])

(defn- initial-state
  []
  (let [[start-x start-y] (grid-coord start-col start-row)]
    {:timers {:move 0 :grow 0}
     :game-over? false
     :grow? false
     :move? false
     :entities {:head {:pos {:col start-col :row start-row}
                       :direction :right
                       :body {:x start-x :y start-y
                              :w grid-size :h grid-size}
                       :graphics {:texture :head}}
                :bodies []}}))

(defn- create-game
  []
  (letfn
   [(create-background-texture
      []
      (let [background (g/pixmap screen-width screen-height Color/BLACK)
            white (g/pixmap grid-size grid-size Color/WHITE)
            gray (g/pixmap grid-size grid-size Color/LIGHT_GRAY)]
        (doseq [c (range cols) r (range rows)]
          (let [color (if (odd? (+ c r)) white gray)
                [x y] (grid-coord c r)]
            (.drawPixmap background color x y)))
        (let [texture (g/texture background)]
          (.dispose background)
          (.dispose white)
          (.dispose gray)
          texture)))]

    {:resources {:texture {:background (create-background-texture)
                           :head (g/color-texture grid-size
                                                  grid-size
                                                  Color/NAVY)
                           :body (g/color-texture grid-size
                                                  grid-size
                                                  Color/OLIVE)}
                 :font {:game-over (g/bitmap-font {:color Color/RED
                                                   :scale 4})
                        :press-enter (g/bitmap-font {:color Color/BLUE
                                                     :scale 2})}}

     :state (initial-state)}))

(defn- process-input
  [mode data]
  (letfn
   [(key-down
      [keycode]
      (condp = keycode
        Input$Keys/RIGHT [:move :right]
        Input$Keys/LEFT [:move :left]
        Input$Keys/DOWN [:move :down]
        Input$Keys/UP [:move :up]
        Input$Keys/ENTER [:enter]
        nil))]

    (case mode
      :key-down (key-down (:keycode data))
      nil)))

(defn- handle-events
  [state _]
  (let [game-over? (:game-over? state)]
    (letfn
     [(handle-event
        [acc event]
        (if game-over?
          (match event
            [:enter] (initial-state)
            :else acc)
          (match event
            [:move direction] (assoc-in acc
                                        [:entities :head :direction]
                                        direction)
            :else acc)))]

      (-> (reduce handle-event state (:events state))
          (assoc :events [])))))

(defn- update-timers
  [state {:keys [delta-time]}]
  (letfn
   [(update-timer
      [timer-key limit]
      (as-> (get-in state timer-key) timer
        (+ timer delta-time)
        (if (> timer limit) 0 timer)))]

    (let [move-timer (update-timer [:timers :move] move-timer-limit)
          grow-timer (update-timer [:timers :grow] grow-timer-limit)
          move? (zero? move-timer)
          grow? (or (:grow? state) (zero? grow-timer))]
      (-> state
          (assoc :timers {:move move-timer :grow grow-timer})
          (assoc :move? move?)
          (assoc :grow? grow?)))))

(defn- move-entity
  [{:keys [pos direction] :as entity}]
  (let [{:keys [col row]} pos
        [new-col new-row] (case direction
                            :right [(inc col) row]
                            :left [(dec col) row]
                            :up [col (inc row)]
                            :down [col (dec row)])
        [new-x new-y] (grid-coord new-col new-row)]
    (-> entity
        (assoc :pos {:col new-col :row new-row})
        (assoc-in [:body :x] new-x)
        (assoc-in [:body :y] new-y))))

(defn- update-bodies
  [state _]
  (letfn
   [(grow-body
      []
      (let [bodies (get-in state [:entities :bodies])
            new-body (-> (get-in state [:entities :head])
                         (assoc-in [:graphics :texture] :body))
            new-bodies (conj bodies new-body)]
        (-> state
            (assoc-in [:entities :bodies] new-bodies)
            (assoc :grow? false))))

    (move-bodies
      []
      (let [bodies (get-in state [:entities :bodies])]
        (if (empty? bodies)
          state
          (let [new-bodies (mapv move-entity bodies)
                new-directions (->> (get-in state [:entities :head])
                                    (conj new-bodies)
                                    (rest)
                                    (mapv :direction))
                new-new-bodies (->> (mapv vector new-bodies new-directions)
                                    (mapv #(assoc (first %)
                                                  :direction
                                                  (second %))))]
            (assoc-in state [:entities :bodies] new-new-bodies)))))]

    (if (:move? state)
      (if (:grow? state) (grow-body) (move-bodies))
      state)))

(defn- update-head
  [state _]
  (if (:move? state)
    (let [new-head (move-entity (get-in state [:entities :head]))]
      (-> state
          (assoc-in [:entities :head] new-head)
          (assoc :move? false)))
    state))

(defn- check-collision
  [state _]
  (letfn
   [(collided?
      [{c1 :col r1 :row} {c2 :col r2 :row}]
      (and (= c1 c2) (= r1 r2)))]

    (let [{:keys [col row] :as head-pos} (get-in state [:entities :head :pos])
          out-of-bound? (or (< row 0)
                            (<= rows row)
                            (< col 0)
                            (<= cols col))
          bump? (->> (get-in state [:entities :bodies])
                     (mapv :pos)
                     (some #(collided? head-pos %)))]

      (if (or out-of-bound? bump?)
        (assoc state :game-over? true)
        state))))

(defn- update-state
  [state data]
  (if (:game-over? state)
    (handle-events state data)
    (-> state
        (handle-events data)
        (update-timers data)
        (update-bodies data)
        (update-head data)
        (check-collision data))))

(defn- render
  [state {:keys [batch]}]
  (u/draw-texture batch [:texture :background] {:x 0 :y 0})
  (u/draw-entities batch [:entities :bodies])
  (u/draw-entity batch [:entities :head])
  (when (:game-over? state)
    (u/draw-text batch [:font :game-over] {:text "GAME OVER" :x 220 :y 400})
    (u/draw-text batch [:font :press-enter] {:text "Press Enter to Restart"
                                             :x 250 :y 320})))

(defn start-game
  []
  (engine/start
   {:title "Snake"
    :w screen-width :h screen-height
    :create-fn create-game
    :update-fn update-state
    :render-fn render
    :process-input-fn process-input}))

(defn exit-game [] (engine/exit))

