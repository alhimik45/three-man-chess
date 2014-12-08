(ns three-man-chess.rpc
  (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]])
  (:require
   [tailrecursion.javelin :refer [cell]]
   [tailrecursion.castra :refer [mkremote]]))

(defc state {:position (repeat 4 (repeat 24 nil))
             :turn 0
             :avail-players []})
(defc error nil)
(defc loading [])

(cell= (when error (.log js/console (:cause error))))

(defc= random-number (get state :random))
(defc= match-id (get state :match))
(defc= player-id (get state :player-id))

(def get-state
  (mkremote 'three-man-chess.api/get-state state error loading))

(def new-game
  (mkremote 'three-man-chess.api/new-game state error loading))

(def connect-to-game
  (mkremote 'three-man-chess.api/connect-to-game state error loading))

(def move
  (mkremote 'three-man-chess.api/move (cell nil) (cell nil) (cell nil)))

(defn game-loop [game-id player-id pos last-move turn avail-players me]
  (get-state game-id player-id)
  (js/setInterval #(get-state game-id player-id) 3000)
  (cell= (reset! @pos (:position state)))
  (cell= (reset! @last-move (:last-move state)))
  (cell= (reset! me (:num state)))
  (cell= (reset! turn (:turn state)))
  (cell= (reset! avail-players (:avail-players state))))
