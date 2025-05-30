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

(def ^:private ^:const start-col 1)
(def ^:private ^:const start-row 12)

(def all-grids (vec (for [c (range cols) r (range rows)] [c r])))

(defn- random-food-pos
  [occupied]
  (let [occupied-set (set occupied)
        empties (filterv #(not (occupied-set %)) all-grids)
        choice (rand-int (count empties))]
    (get empties choice)))

(defn- grid-coord
  "returns the coordinate of top left corner of the given grid in vector,
  e.g. [48 60] for row 1 / col 2"
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
               :food (grid-texture Color/MAROON)}
    :fonts {:game-over (g/bitmap-font {:color Color/RED :scale 4})
            :press-enter (g/bitmap-font {:color Color/BLUE :scale 2})}}))

(defn- initial-state
  []
  (let [[food-col food-row] (random-food-pos [[start-col start-row]])]

    {:timers {:move {:value 0 :limit 0.1}}
     :conditions {:game-over? false
                  :grow? false
                  :move? false
                  :new-food? false}
     :entities {:head {:col start-col
                       :row start-row
                       :direction :right
                       :texture :head}
                :bodies []
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
        nil))]

   (case mode
     :key-down (key-down (:keycode data))
     nil)))

(defn- handle-events
  [state _]
  (let [game-over? (get-in state [:conditions :game-over?])]
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
    (let [head (get-in state [:entities :head])
          bodies (get-in state [:entities :bodies])
          occupied (->> (conj bodies head)
                        (mapv #(vector (:col %) (:row %))))
          [food-col food-row] (random-food-pos occupied)
          new-food (-> (get-in state [:entities :food])
                       (assoc :col food-col :row food-row))]
      (-> state
          (assoc-in [:entities :food] new-food)
          (assoc-in [:conditions :new-food?] false)))
    state))

(defn- move-entity
  [{:keys [col row direction] :as entity}]
  (let [[new-col new-row] (case direction
                            :right [(inc col) row]
                            :left [(dec col) row]
                            :up [col (inc row)]
                            :down [col (dec row)])]
    (assoc entity :col new-col :row new-row)))

(defn- update-bodies
  [state _]
  (letfn
   [(grow-body
      []
      (let [bodies (get-in state [:entities :bodies])
            head (get-in state [:entities :head])
            new-body (assoc head :texture :body)
            new-bodies (conj bodies new-body)]
        (-> state
            (assoc-in [:entities :bodies] new-bodies)
            (assoc-in [:conditions :grow?] false))))

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

   (if (get-in state [:conditions :move?])
     (if (get-in state [:conditions :grow?])
       (grow-body)
       (move-bodies))
     state)))

(defn- update-head
  [state _]
  (if (get-in state [:conditions :move?])
    (let [new-head (move-entity (get-in state [:entities :head]))]
      (assoc-in state [:entities :head] new-head))
    state))

(defn- check-collision
  [state _]
  (letfn
   [(collided?
      [{c1 :col r1 :row} {c2 :col r2 :row}]
      (and (= c1 c2) (= r1 r2)))]

   (let [{:keys [col row] :as head} (get-in state [:entities :head])
         out-of-bound? (or (< row 0)
                           (<= rows row)
                           (< col 0)
                           (<= cols col))
         bump? (->> (get-in state [:entities :bodies])
                    (some #(collided? head %)))
         food (get-in state [:entities :food])
         eat? (collided? head food)
         new-conditions (cond-> (:conditions state)
                          (or out-of-bound? bump?) (assoc :game-over? true)
                          eat? (assoc :grow? true :new-food? true))]
     (assoc state :conditions new-conditions))))

(defn- update-state
  [state data]
  (if (get-in state [:conditions :game-over?])
    (handle-events state data)
    (-> state
        (handle-events data)
        (update-timers data)
        (update-food data)
        (update-bodies data)
        (update-head data)
        (check-collision data))))

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

   (u/draw-texture batch (:background textures) {:x 0 :y 0})
   (->> (:entities state)
        (u/flatten-entities)
        (draw-entities batch))

   (when (get-in state [:conditions :game-over?])
     (u/draw-text batch (:game-over fonts) {:text "GAME OVER" :x 220 :y 400})
     (u/draw-text batch (:press-enter fonts) {:text "Press Enter to Restart"
                                              :x 250 :y 320}))))

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
  (engine/start game))

(defn exit-game
  [game]
  (engine/exit game))
