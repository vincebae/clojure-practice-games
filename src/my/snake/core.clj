(ns my.snake.core
  (:require
   [clojure.core.async :refer [>!! <! chan go poll!]]
   [clojure.core.match :refer [match]]
   [my.snake.engine :refer [start exit]]
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

(def ^:private ^:const start-col 2)
(def ^:private ^:const start-row 12)

(def all-grids (vec (for [c (range cols) r (range rows)] [c r])))

(defonce dev-chan (chan))

(defn wait-dev-chan [msg] (>!! dev-chan msg))

(defn poll-dev-chan [] (poll! dev-chan))

(defn clear-dev-chan
  []
  (go (when-let [_ (poll! dev-chan)]
        (<! dev-chan))))

(defn- random-food-pos
  [occupied]
  (let [occupied-set (set occupied)
        empties (filterv #(not (occupied-set %)) all-grids)
        choice (rand-int (count empties))]
    (get empties choice)))

(defn- grid-coord
  "returns the coordinate of top left corner of the given grid in vector"
  [col row]
  [(+ (* col grid-size) pillar-box)
   (+ (* row grid-size) letter-box)])

(defn- initial-resources
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
          texture)))

    (grid-texture [color] (g/color-texture grid-size grid-size color))]

   {:textures {:background (create-background-texture)
               :head (grid-texture Color/NAVY)
               :body (grid-texture Color/OLIVE)
               :tail (grid-texture Color/YELLOW)
               :food (grid-texture Color/MAROON)}
    :fonts {:game-mode (g/bitmap-font {:color Color/RED :scale 4})
            :press-key (g/bitmap-font {:color Color/BLUE :scale 2})}}))

(defn- initial-state
  []
  (let [[food-col food-row] (random-food-pos [[start-col start-row]])]
    {:mode :running
     :events []
     :conditions {:grow? false
                  :move? false
                  :new-food? false}
     :timers {:move {:value 0 :limit 0.1}}
     :entities {:snake [{:col start-col
                         :row start-row
                         :direction :right
                         :texture :head}
                        {:col (dec start-col)
                         :row start-row
                         :direction :right
                         :texture :tail}]
                :food {:col food-col
                       :row food-row
                       :texture :food}}}))

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
        Input$Keys/ESCAPE [:esc]
        nil))]

   (case mode
     :key-down (key-down (:keycode data))
     nil)))

(defn- handle-events
  [state _]
  (letfn
   [(handle-event
      [acc event]
      (let [mode (:mode state)]
        (match [mode event]
          [:running [:esc]] (assoc acc :mode :paused)
          [:running [:move direction]] (assoc-in acc
                                                 [:entities :snake 0 :direction]
                                                 direction)
          [:game-over [:enter]] (initial-state)
          [:paused [:enter]] (assoc acc :mode :running)
          :else acc)))]

   (-> (reduce handle-event state (:events state))
       (assoc :events []))))

(defn- update-timers
  [state {:keys [delta-time]}]
  (letfn
   [(update-timer
      [timer-key]
      (let [{:keys [value limit]} (get-in state timer-key)]
        (as-> (+ value delta-time) v
          (if (> v limit) 0 v))))]

   (let [move-timer-value (update-timer [:timers :move])]
     (-> state
         (assoc-in [:timers :move :value] move-timer-value)
         (assoc-in [:conditions :move?] (zero? move-timer-value))))))

(defn- update-food
  [state _]
  (if (get-in state [:conditions :new-food?])
    (let [occupied (->> (get-in state [:entities :snake])
                        (mapv #(vector (:col %) (:row %))))
          [food-col food-row] (random-food-pos occupied)
          new-food (-> (get-in state [:entities :food])
                       (assoc :col food-col :row food-row))]
      (-> state
          (assoc-in [:entities :food] new-food)
          (assoc-in [:conditions :new-food?] false)))
    state))

(defn- move-snake
  [state _]
  (letfn
   [(move-piece
      [{:keys [col row direction] :as entity}]
      (let [[new-col new-row] (case direction
                                :right [(inc col) row]
                                :left [(dec col) row]
                                :up [col (inc row)]
                                :down [col (dec row)])]
        (assoc entity :col new-col :row new-row)))

    (update-direction
      [snake original]
      (->> (cons (first snake) (butlast original))
           (mapv vector snake)
           (mapv (fn [[piece {d :direction}]]
                   (assoc piece :direction d)))))]

   (if (get-in state [:conditions :move?])
     (let [snake (get-in state [:entities :snake])
           new-snake (-> (mapv move-piece snake)
                         (update-direction snake))]
       (assoc-in state [:entities :snake] new-snake))
     state)))

(defn- grow-snake
  [state {old-state :state}]
  (let [grow? (get-in state [:conditions :grow?])
        move? (get-in state [:conditions :move?])]
    (if (and grow? move?)
      (let [snake (get-in state [:entities :snake])
            old-tail (last (get-in old-state [:entities :snake]))
            new-snake (-> snake
                          (assoc-in [(dec (count snake)) :texture] :body)
                          (conj old-tail))]
        (-> state
            (assoc-in [:entities :snake] new-snake)
            (assoc-in [:conditions :grow?] false)))
      state)))

(defn- check-collision
  [state _]
  (letfn
   [(collided?
      [{c1 :col r1 :row} {c2 :col r2 :row}]
      (and (= c1 c2) (= r1 r2)))]

   (let [{:keys [col row] :as head} (get-in state [:entities :snake 0])
         out-of-bound? (or (< row 0)
                           (<= rows row)
                           (< col 0)
                           (<= cols col))
         bump? (->> (get-in state [:entities :snake])
                    (rest)
                    (some #(collided? head %)))
         food (get-in state [:entities :food])
         eat? (collided? head food)]
     (cond-> state
       (or out-of-bound? bump?) (assoc :mode :game-over)
       eat? (-> (assoc-in [:conditions :grow?] true)
                (assoc-in [:conditions :new-food?] true))))))

(defn- update-state
  [state data]
  (let [new-state (handle-events state data)
        mode (:mode new-state)]
    (if (= mode :running)
      (-> new-state
          (update-timers data)
          (update-food data)
          (move-snake data)
          (grow-snake data)
          (check-collision data))
      new-state)))

(defn- render
  [state {:keys [batch textures fonts]} & _]
  (letfn
   [(draw-entity
      [batch {:keys [col row texture]}]
      (let [[x y] (grid-coord col row)
            tex (get textures texture)]
        (u/draw-texture batch tex {:x x :y y :w grid-size :h grid-size})))

    (draw-entities
      [batch entities]
      (run! #(draw-entity batch %) entities))]

   (let [mode (:mode state)]

      (u/draw-texture batch (:background textures) {:x 0 :y 0})
      (->> (:entities state)
           (u/flatten-entities)
           (draw-entities batch))

      (when (= mode :game-over)
        (u/draw-text batch (:game-mode fonts) {:text "GAME OVER" :x 220 :y 400})
        (u/draw-text batch (:press-key fonts) {:text "Press Enter to Restart"
                                               :x 250 :y 320}))
      (when (= mode :paused)
        (u/draw-text batch (:game-mode fonts) {:text "Paused" :x 300 :y 400})
        (u/draw-text batch (:press-key fonts) {:text "Press Enter to Resume"
                                               :x 260 :y 320})))))

(def config
  {:title "Snake"
   :w screen-width
   :h screen-height
   :update-fn update-state
   :render-fn render
   :process-input-fn process-input
   :resources-fn initial-resources
   :state-fn initial-state})

(defn init-game
  []
  {:config config
   :resources (atom nil)
   :state (atom nil)})

(defn start-game
  [game]
  (start game))

(defn exit-game
  [game]
  (exit game))

