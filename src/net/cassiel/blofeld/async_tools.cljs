(ns net.cassiel.blofeld.async-tools
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [net.cassiel.blofeld.manifest :as m]
            [cljs.core.async :as a :refer [>! <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [oops.core :refer [ocall]]))

(defn throttle
  "Speed limit (by discarding) messages coming into `in-ch`, echoing to out-chan after a timeout."
  [in-ch out-ch]
  (go-loop [held-value nil]
    (println "into slowdown with" held-value)
    (if held-value
      (alt!
        in-ch ([v] (when v (recur v)))
        (a/timeout m/SOUND-REQUEST-MS) (do (>! out-ch held-value)
                                           (recur nil)))
      (when-let [v (<! in-ch)] (recur v)))))

(defn marshall-sysex-out
  "Start 'thread'. `in-ch` delivers complete sysex messages (sequences of bytes); this is a single
   output point to send the bytes in sequence without accidental interleaving."
  [max-api in-ch]
  (go-loop []
    (when-let [sysex (<! in-ch)]
      #_ (println "MARSHALLING" sysex)
      (doseq [b sysex]
        (<p! (ocall max-api :outlet "int" b)))
      (<! (a/timeout 150))          ; It seems like we do need to throttle slightly.
      (recur))))
