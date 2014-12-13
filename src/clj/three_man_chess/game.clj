(ns three-man-chess.game
  (:require [three-man-chess.move :as m]))

(def start-position [[[0 'pawn 'counterclockwise] [0 'rook] [0 'rook] [0 'pawn 'clockwise] nil nil nil nil [1 'pawn 'counterclockwise] [1 'rook] [1 'rook] [1 'pawn 'clockwise] nil nil nil nil [2 'pawn 'counterclockwise] [2 'rook] [2 'rook] [2 'pawn 'clockwise] nil nil nil nil]
                     [[0 'pawn 'counterclockwise] [0 'knight] [0 'knight] [0 'pawn 'clockwise] nil nil nil nil [1 'pawn 'counterclockwise] [1 'knight] [1 'knight] [1 'pawn 'clockwise] nil nil nil nil [2 'pawn 'counterclockwise] [2 'knight] [2 'knight] [2 'pawn 'clockwise] nil nil nil nil]
                     [[0 'pawn 'counterclockwise] [0 'bishop] [0 'bishop] [0 'pawn 'clockwise] nil nil nil nil [1 'pawn 'counterclockwise] [1 'bishop] [1 'bishop] [1 'pawn 'clockwise] nil nil nil nil [2 'pawn 'counterclockwise] [2 'bishop] [2 'bishop] [2 'pawn 'clockwise] nil nil nil nil]
                     [[0 'pawn 'counterclockwise] [0 'queen] [0 'king] [0 'pawn 'clockwise] nil nil nil nil [1 'pawn 'counterclockwise] [1 'queen] [1 'king] [1 'pawn 'clockwise] nil nil nil nil [2 'pawn 'counterclockwise] [2 'queen] [2 'king] [2 'pawn 'clockwise] nil nil nil nil]])

(defn owner-of [cell]
  (first cell))

(defn get-owner [position coord]
  (owner-of (get-in position coord)))

(defn map-cell [f coll]
  (mapv #(mapv f %) coll))

(defn remove-owner [game-data dead-owner new-owner]
  (-> game-data
      (update-in [:avail-players] #(remove #{dead-owner} %))
      (update-in [:position]
                 (fn [pos] (map-cell #(if (= (first %) dead-owner)
                                        (assoc % 0 new-owner)
                                        %)
                                     pos)))))

(defn change-pawn-direction [game-data dead-owner new-owner]
  (update-in game-data
             [:position]
             (fn [pos]
               (map-cell (fn [cell]
                           (if (and (= (owner-of cell) dead-owner)
                                    (= (m/piece-type cell) 'pawn))
                             (let [owner-diff (- new-owner dead-owner)]
                               (cond
                                (or (= owner-diff -1)
                                    (= owner-diff 2))   (assoc cell 2 'clockwise)
                                (or (= owner-diff 1)
                                    (= owner-diff -2))  (assoc cell 2 'counterclockwise)))
                             cell))
                         pos))))

(defn king-captured? [position to]
  (= (second (get-in position to)) 'king))

(defn check-king-captured [{:keys [position] :as game-data} from to]
  (if (king-captured? position to)
    (let [dead-owner (get-owner position to)
          new-owner (get-owner position from)]
      (-> game-data
          (change-pawn-direction dead-owner new-owner)
          (remove-owner dead-owner new-owner)))
    game-data))

(defn next-turn [{:keys [avail-players turn] :as game-data}]
  (let [next-player (nth avail-players
                         (mod (inc (.indexOf avail-players
                                             turn))
                              (count avail-players)))]
    (assoc game-data :turn next-player)))

(defn player-move [game-data from to]
  (-> game-data
      (check-king-captured from to)
      (next-turn)
      (update-in [:position] #(assoc-in %1 to (get-in %1 from)))
      (update-in [:position] #(assoc-in %1 from nil))))
