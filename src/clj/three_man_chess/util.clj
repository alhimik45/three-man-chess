(ns three-man-chess.util
  (:refer-clojure :exclude [with-local-vars var-get var-set]))


(defmacro var-get
  "Gets the value in the var object"
  [x] `(.-val ~x))

(defmacro var-set
  "Sets the value in the var object to val. The var must be
  thread-locally bound."
  [x val] `(set! (.-val ~x) ~val))

(defmacro with-local-vars
  "varbinding=> symbol init-expr
  Executes the exprs in a context in which the symbols are bound to
  vars with per-thread bindings to the init-exprs.  The symbols refer
  to the var objects themselves, and must be accessed with var-get and
  var-set"
  [name-vals-vec & body]
  `(let [~@(interleave (take-nth 2 name-vals-vec)
                       (repeat '(js/Object.)))]
     ~@(for [[variable value] (partition 2 name-vals-vec)]
         `(var-set ~variable ~value))
     ~@body))
