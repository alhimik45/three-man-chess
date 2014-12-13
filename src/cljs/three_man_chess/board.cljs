(ns three-man-chess.board
  (:require [monet.canvas :as canvas]
            [three-man-chess.move :as move]
            [three-man-chess.rpc :as rpc])
  (:require-macros
   [tailrecursion.javelin :refer [defc defc= cell= dosync]]
   [three-man-chess.event :refer [declare-event-cell]]))

(defn log [& x]
  (.log js/console (str x)))

(defc selected-cell [])
(defc hovered-cell [])
(defc possible-moves-cell [])
(defc= last-move-cell (:last-move rpc/state))

(declare-event-cell move-cell [])
;(declare-event-cell win-cell nil)


(defc= position (:position rpc/state))
(def chess-pieces (atom {}))
(def pieces-count (atom 0))
(def pieces-loaded (atom 0)) ;must be equal pieces-count when all images loaded

(defc scale-num 300)
(def players 3)
(def sectors-per-player 8)
(def all-circles 4)
(def all-sectors (* players sectors-per-player))
(def angle-per-player-degree (/ 360 players))
(def angle-per-sector-degree (/ angle-per-player-degree sectors-per-player))
(def angle-per-sector-radian (/ (* angle-per-sector-degree Math/PI) 180))
(def colors {:light-cell "#fdc074"
             :dark-cell "#744409"
             :light-selected-cell "#4F81A2"
             :dark-selected-cell "#0A324A"
             :normal "#744409"
             :possible "#000"
             :selected "#0f0"
             :last-move "#f00"
             :hovered "#ff0"})

(defc= available-players (:avail-players rpc/state))
(defc= turn (:turn rpc/state)) ;who makes move now
(defc= me (:num rpc/state))

(defn change-scale! [new-scale]
  (reset! scale-num new-scale))

(defn scale
  ([x] (* x @scale-num))
  ([x additional-scale] (* x @scale-num additional-scale)))

(defc= center-offset scale-num)

(defn piece->filename [piece]
  (@chess-pieces (keyword (clojure.string/join "-" piece))))

(defn get-coordinates
  "Returns array [x y] of top left corner of single cell in canvas coordinates"
  [circle-number sector-number]
  (map #(+  @center-offset %)
       [(scale (Math/cos (* angle-per-sector-radian sector-number))
               (- 1 (/ circle-number 7)))
        (scale (Math/sin (* angle-per-sector-radian sector-number))
               (- 1 (/ circle-number 7)))]))

(defn get-cell-coordinates
  "Returns array [[x y] [x y] [x y] [x y]] of all corners of cell"
  [circle-number sector-number]
  [(get-coordinates circle-number (inc sector-number))
   (get-coordinates (inc circle-number) (inc sector-number))
   (get-coordinates (inc circle-number) sector-number)
   (get-coordinates circle-number sector-number)])

(defn canvas->board
  "Converts canvas coordinates to coordinate centered at the center of a board"
  [x y]
  [(- x @center-offset) (+ (- y) @center-offset)])


(defn circle-borders []
  (let [min-range (first (apply canvas->board (get-coordinates all-circles 0)))
        max-range (first (apply canvas->board (get-coordinates 0 0)))
        step (/ (- max-range min-range) all-circles)]
    (concat (take all-circles
                  (range min-range max-range step ))
            [max-range])))

(defn angle
  "Counts angle in polar coordinates from canvas coordinates"
  [x y]
  (let [[real-x real-y] (canvas->board x y)  ]
    (+ Math/PI
       (* (if (> real-y 0) -1 1)
          (Math/acos
           (/ real-x
              (Math/sqrt (+ (Math/pow real-x 2)
                            (Math/pow real-y 2)))))))))

(defn sector
  "Counts sector from canvas coordinates"
  [x y]
  (mod
   (+
    (int (/ (angle x y) angle-per-sector-radian))
    (/ all-sectors 2))
   all-sectors))

(defn circle
  "Counts circle from canvas coordinates. Returns nil if coordinates beyond the circles"
  [x y]
  (let [[real-x real-y] (canvas->board x y)
        distance (Math/sqrt (+ (Math/pow real-x 2)
                               (Math/pow real-y 2)))
        borders (circle-borders)
        results (for [[from to]  (partition 2 (interleave borders (rest borders)))]
                  (and
                   (>= distance from)
                   (<= distance to)))
        circle-number (first (keep-indexed #(when %2 %1) (reverse results)))]
    circle-number))

(defn canvas->game
  "Takes canvas coordinates and converts to game coordinates"
  [x y]
  [(circle x y) (sector x y)])

(defn draw-cell
  "Draw single cell of board"
  ([ctx circle-number sector-number] (draw-cell ctx circle-number sector-number :normal))
  ([ctx circle-number sector-number state]
   (canvas/begin-path ctx)
   (let [coordinates (get-cell-coordinates circle-number sector-number)]
     (apply canvas/move-to ctx (first coordinates))
     (doseq [[real-x real-y] (rest coordinates)]
       (canvas/line-to ctx real-x real-y)))
   (canvas/close-path ctx)

   (canvas/fill-style ctx
                      (cond
                       (and (odd? (+ circle-number sector-number))
                            (= state :possible))                    (:light-selected-cell colors)
                       (odd? (+ circle-number sector-number))       (:light-cell colors)
                       (= state :possible)                          (:dark-selected-cell colors)
                       :else                                        (:dark-cell colors)))
   (canvas/stroke-style ctx (get colors state))
   (canvas/stroke-width ctx
                        (case state
                          :normal 1
                          (scale 0.014)))
   (canvas/fill ctx)
   (canvas/stroke ctx)))

(defn draw-special-cells
  "Draws selected and hovered cells"
  [ctx cells]
  (doseq [[state cell] cells]
    (when-not (= cell [])
      (if (every? coll? cell)
        (doseq [cell-value cell]
          (apply draw-cell ctx (concat cell-value [state])))
        (apply draw-cell ctx (concat cell [state]))))))

(defn draw-piece
  "Draws single piece."
  [ctx circle-number sector-number piece]
  (let [piece-filename (piece->filename piece)
        coordinates (get-cell-coordinates circle-number sector-number)
        corner (first coordinates)
        opposite-corner (second (rest coordinates))
        [real-x real-y] (map #(- (/ (+ %1 %2) 2) (scale 0.071)) corner opposite-corner)]
    (canvas/draw-image ctx piece-filename {:x real-x
                                           :y real-y
                                           :w (scale 0.128)
                                           :h (scale 0.128)})))

(defn draw-pieces
  "Draws chess pieces"
  [ctx position]
  (doseq [circle-number (range 0 all-circles)
          sector-number (range 0 all-sectors)
          :let [piece (get-in position [circle-number sector-number])]]
    (when piece
      (draw-piece ctx circle-number sector-number piece))))

;; (defn check-win [position]
;;   (when (= (count @available-players) 1)
;;     (reset! win-cell (first @available-players))))

(defn change-cell!
  "Set cell to circular coordinates"
  [cell x y]
  (let [[circle-number sector-number] (canvas->game x y) ]
    (reset! cell (if (nil? circle-number)
                   []
                   [circle-number sector-number]))))

(defn empty-cell?
  "Checks if there is a chess piece in the cell"
  [[circle-number sector-number]]
  (nil? (get-in @position [circle-number sector-number])))

(defn not-player-piece [coords]
  (not= @me (first (get-in @position coords))))

(defn gen-move [from to]
  (when (and (= @me @turn)
             (move/possible? all-sectors @position @turn from to))
    (reset! move-cell [from to])))

(defn highlight-possible-moves [from]
  (dosync
   (reset! possible-moves-cell [])
   (doseq [circ (range all-circles)
           sec (range all-sectors)
           :let [to-pos [circ sec]]]
     (when (move/possible? all-sectors @position @turn from to-pos)
       (swap! possible-moves-cell conj to-pos)))))

(defn select-cell [coords]
  (dosync
   (reset! selected-cell coords)
   (highlight-possible-moves coords)))

(defn clear-select []
  (dosync
   (reset! selected-cell [])
   (reset! possible-moves-cell [])))

(defn handle-click
  "If click is end of move changes move-cell"
  [x y]
  (let [[circle-number sector-number :as new-coords] (canvas->game x y)]
    (if (and (not= @selected-cell [])
             (not (empty-cell? @selected-cell)))
      (do
        (when-not (or (nil? circle-number)
                      (= new-coords @selected-cell))
          (gen-move @selected-cell new-coords))
        (clear-select))
      (if (or (empty-cell? new-coords)
              (not-player-piece new-coords))
        (clear-select)
        (select-cell new-coords)))))

(defn on-mouse-move [event canvas]
  (let [client-rect (.getBoundingClientRect canvas)
        client-rect-left (.-left client-rect)
        client-rect-top (.-top client-rect)
        client-x (.-clientX event)
        client-y (.-clientY event)
        x ( - client-x client-rect-left)
        y ( - client-y client-rect-top)]
    (change-cell! hovered-cell x y)))

(defn on-click [event canvas]
  (let [client-rect (.getBoundingClientRect canvas)
        client-rect-left (.-left client-rect)
        client-rect-top (.-top client-rect)
        client-x (.-clientX event)
        client-y (.-clientY event)
        x ( - client-x client-rect-left)
        y ( - client-y client-rect-top)]
    (handle-click x y)))

(defn init-chess-pieces
  "Loads pieces images"
  []
  (doseq [side [0 1 2]
          direction ["clockwise" "counterclockwise"]] ;;process pawns first
    (let [img (js/Image.)]
      (set! (.-src img) (str "img/" side "_pawn_" direction ".svg"))
      (set! (.-onload img) #(swap! pieces-loaded inc))
      (swap! pieces-count inc)
      (swap! chess-pieces assoc (keyword (str side "-pawn-" direction)) img)))
  (doseq [side [0 1 2]
          piece ["bishop" "knight" "rook" "queen" "king"]]
    (let [img (js/Image.)]
      (set! (.-src img) (str "img/" side "_" piece ".svg"))
      (set! (.-onload img) #(swap! pieces-loaded inc))
      (swap! pieces-count inc)
      (swap! chess-pieces assoc (keyword (str side "-" piece)) img))))

(defn init-board []
  (let [canvas (.getElementById js/document "canvas")
        ctx (canvas/get-context canvas "2d")]
    (.addEventListener canvas
                       "mouseup"
                       (fn [event]
                         (on-click event canvas))
                       false)
    (.addEventListener canvas
                       "mousemove"
                       (fn [event]
                         (on-mouse-move event canvas))
                       false)
    (cell= ;;redraw board after changing selected, hovered or scale cell
     (do
       scale-num
       (when (= @pieces-loaded @pieces-count)
         (canvas/clear-rect ctx {:x 0 :y 0 :w (.-width canvas) :h (.-height canvas)})
         (doseq [circle-number (range 0 all-circles)
                 sector-number (range 0 all-sectors)]
           (draw-cell ctx circle-number sector-number))
         (draw-special-cells ctx [[:last-move last-move-cell] [:possible possible-moves-cell] [:selected selected-cell] [:hovered hovered-cell]])
         (draw-pieces ctx position))))))
;; (add-to-win-cell-listeners! (fn [player] (js/alert (str player " win!"))))
(init-chess-pieces)
