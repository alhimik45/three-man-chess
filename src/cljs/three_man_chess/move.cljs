(ns three-man-chess.move)

(defn empty-point? [position point] ;do i need this fn?
  (nil? (get-in position point)))

(defn enemy-piece? [piece test-piece]
  (and (seq test-piece)
       (not= (first piece) (first test-piece))))

(defn piece-can-go-here [position from to]
  (let [piece (get-in position from)
        dest (get-in position to)]
    (or (empty-point? position to)
        (enemy-piece? piece dest))))

(defn dist-sec [all-sectors from-sec to-sec]
  (let [max-sec (max from-sec to-sec)
        min-sec (min from-sec to-sec)]
    (min (Math/abs (- max-sec min-sec))
         (Math/abs (- max-sec all-sectors min-sec)))))

(defn dist-circ [from-circ to-circ]
  (Math/abs (- from-circ to-circ)))

(defn piece-type [piece]
  (piece 1))

(defn pawn-direction [piece]
  (piece 2))

(defn possible-pawn? [all-sectors position [from-circ from-sec :as from] [to-circ to-sec :as to]]
  (let [pawn (get-in position from)
        dest (get-in position to)]
    (and (condp = (pawn-direction pawn)
           'clockwise (and (or (= (- to-sec from-sec) 1)
                               (and (= from-sec (dec all-sectors))
                                    (= to-sec 0))))
           'counterclockwise (and (or (= (- to-sec from-sec) -1)
                                      (and (= from-sec 0)
                                           (= to-sec (dec all-sectors))))))
         (or (and (= from-circ to-circ)
                  (empty-point? position to))
             (and (= (dist-circ from-circ to-circ) 1)
                  (enemy-piece? pawn dest))))))

(defn possible-king? [all-sectors position [from-circ from-sec :as from] [to-circ to-sec :as to]]
  (and (<= (dist-circ from-circ to-circ) 1)
       (<= (dist-sec all-sectors from-sec to-sec) 1)))

(defn possible-knight? [all-sectors position [from-circ from-sec :as from] [to-circ to-sec :as to]]
  (or (and (= (dist-sec all-sectors from-sec to-sec) 2)
           (= (dist-circ from-circ to-circ) 1))
      (and (= (dist-sec all-sectors from-sec to-sec) 1)
           (= (dist-circ from-circ to-circ) 2))))

(defn possible-rook? [all-sectors position [from-circ from-sec :as from] [to-circ to-sec :as to]]
  (or (and (= from-circ to-circ)
           (let [circ (position to-circ)
                 min-sec (min from-sec to-sec)
                 max-sec (max from-sec to-sec)
                 first-path (subvec circ (inc min-sec) max-sec)
                 second-path (concat (subvec circ 0 min-sec)
                                     (subvec circ (inc max-sec)))
                 first-path-is-clear (apply (every-pred nil?) first-path)
                 second-path-is-clear (apply (every-pred nil?) second-path)]
             (or first-path-is-clear
                 second-path-is-clear)))
      (and (= from-sec to-sec)
           (let [min-circ (min from-circ to-circ)
                 max-circ (max from-circ to-circ)]
             (empty? (keep-indexed #(when (and (> %1 min-circ)
                                               (< %1 max-circ))
                                      (%2 to-sec))
                                   position))))))

(defn possible-bishop? [all-sectors position [from-circ from-sec :as from] [to-circ to-sec :as to]]
  (let [dist-s (dist-sec all-sectors from-sec to-sec)
        dist-c (dist-circ from-circ to-circ)
        min-sec (min from-sec to-sec)
        max-sec (max from-sec to-sec)
        min-circ (min from-circ to-circ)
        max-circ (max from-circ to-circ)
        diff-c (if (> to-circ from-circ) 1 -1)
        pre-diff-s (if (> to-sec from-sec) 1 -1)
        diff-s (if (not= (- max-sec min-sec)
                         dist-s)
                 (- pre-diff-s)
                 pre-diff-s)]
    (and (= dist-c dist-s)
         (loop [c (+ from-circ diff-c)
                s (mod (+ from-sec diff-s) all-sectors)]
           (if (= [c s] to)
             true
             (if (seq (get-in position [c s]))
               false
               (recur (+ c diff-c) (mod (+ s diff-s) all-sectors))))))))

(defn possible-queen? [all-sectors position from to]
  (or (possible-rook? all-sectors position from to)
      (possible-bishop? all-sectors position from to)))

(defn possible? [all-sectors position turn from to]
  (let [piece (get-in position from)]
    (and (= turn (first piece))
         (piece-can-go-here position from to)
         ((condp = (piece-type piece)
            'pawn    possible-pawn?
            'king    possible-king?
            'knight  possible-knight?
            'rook    possible-rook?
            'bishop  possible-bishop?
            'queen   possible-queen?)
          all-sectors position from to)
         )))
