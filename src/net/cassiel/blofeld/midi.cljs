(ns net.cassiel.blofeld.midi
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async.interop :refer-macros [<p!]]))

;; TODO: return value of output-seq so that we can chain them.

(defn output-seq [max-api bytes]
  (go
    (loop [b (seq bytes)]
      (when-let [h (first b)]
        (<p! (.outlet max-api h))
        (recur (rest b))))))
