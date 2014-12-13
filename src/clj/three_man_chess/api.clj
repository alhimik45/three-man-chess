(ns three-man-chess.api
  (:require [tailrecursion.castra :refer [defrpc]]
            [three-man-chess.move :as move]
            [three-man-chess.game :as game]))


(def matches (atom {}))

(def players 3)
(def all-sectors 24)

(defn rand-id []
  (clojure.string/join
   ""
   (repeatedly 10 #(rand-nth "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"))))

(defn days []
  (->  (System/currentTimeMillis) (quot 1000) (quot 60) (quot 60) (quot 24)))

(future
  (while true
    (try
      (Thread/sleep (* 1000 60 60 24 2)) ; 2days
      (swap! matches (fn [m]
                       (->> m
                            (filter #(< (- (days) (:last-touch (second %))) 3))
                            (into {}))))
      (catch Exception e
        (println (str "caught exception: " (.getMessage e)))))))

(defn new-match-data []
  {:game-data
   {:position game/start-position
    :turn 0
    :last-move []
    :avail-players (range 0 players)}
   :players []
   :last-touch (days)})

(defn result-answer [game-id player-id]
  (merge (get-in @matches [game-id :game-data])
         {:num
          (if (some #{player-id} (get-in @matches [game-id :players]))
            (.indexOf (get-in @matches [game-id :players]) player-id)
            -1)}))

(defrpc new-game []
  (let [game-id (rand-id)]
    (swap! matches assoc game-id (new-match-data))
    {:match game-id}))

(defrpc connect-to-game [game-id]
  {:rpc/pre [(get @matches game-id)]}
  (if (< (count (get-in @matches [game-id :players])) 3)
    (let [player-id (rand-id)]
      (swap! matches update-in [game-id :players] #(conj % player-id))
      {:player-id player-id
       :num (.indexOf (get-in @matches [game-id :players]) player-id)
       :match game-id})
    {:player-id "view"
     :match game-id}))

(defrpc get-state [game-id player-id]
  {:rpc/pre [(get @matches game-id)]}
  (result-answer game-id player-id))

(defrpc move [game-id player-id [from to]]
  {:rpc/pre [(get @matches game-id)]}
  (let [players (get-in @matches [game-id :players])
        turn (get-in @matches [game-id :game-data :turn])
        position (get-in @matches [game-id :game-data :position])]
    (if-not (and (some #{player-id} players)  ;its valid player
                 (= (.indexOf players player-id) ; its player's turn to move
                    (get-in @matches [game-id :game-data :turn]))
                 (move/possible? all-sectors position turn from to))
      (throw (Exception. "Impossible move"))
      (do
        (swap! matches
                 (fn [m]
                   (-> m
                       (assoc-in [game-id :last-touch] (days))
                       (assoc-in [game-id :game-data :last-move] [from to])
                       (update-in [game-id :game-data] game/player-move from to))))
        (result-answer game-id player-id)))))
