(ns net.cassiel.blofeld.component.presets
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [com.stuartsierra.component :as component]
            [net.cassiel.lifecycle :refer [starting stopping]]
            [net.cassiel.blofeld.manifest :as m]
            [net.cassiel.blofeld.component.max-api :as max-api]
            [net.cassiel.blofeld.async-tools :as async-tools]
            [net.cassiel.blofeld.midi :as midi]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :as a :refer [>!]]))

(defn handle-preset-recall
  "Preset recall into the 'fast' channel; gets throttled to the slow channel."
  [presets bank pgm]
  (go (>! (-> presets :fast-chan)
          [bank pgm])))

(defn matches [wanted actual]
  (match [(seq wanted) (seq actual)]
         [nil _] true
         [_ nil] false
         [([\. & wanted'] :seq) ([_ & actual'] :seq)] (recur wanted' actual')
         [([w & wanted'] :seq) ([a & actual'] :seq)] (and (= w a) (recur wanted' actual'))
         :else false))

(defn handle-sysex
  "Bytes is a seq containing a leading 0xF0, or else an isolated 0xF7."
  [presets bytes]
  (println "sysex chunk starting " (first bytes) " len " (count bytes))

  (cond
    (matches [m/EOX] bytes)
    (println "> EOX")

    (matches [m/SOX m/WALDORF m/BLOFELD \. m/SNDD] bytes)
    (println "> SNDD - Sound Dump")

    (matches [m/SOX m/WALDORF m/BLOFELD \. m/SNDP] bytes)
    (println "> SNDP - Sound Parameter Change")

    :else
    (println "> unknown sysex")))

(defrecord PRESETS [max-api fast-chan slow-chan installed?]
  Object
  (toString [this] "PRESETS")

  component/Lifecycle
  (start [this]
    (starting this
              :on installed?
              :action #(let [fast-chan (a/chan)
                             slow-chan (a/chan)]
                         ;; Channel throttling, to wait for pause after program change:
                         (async-tools/throttle fast-chan slow-chan)
                         ;; Deal with program change:
                         (go-loop []
                           (when-let [[hi lo] (<! slow-chan)]
                             (midi/output-seq (-> max-api :max-api)
                                              [m/SOX m/WALDORF m/BLOFELD m/BROADCAST-ID m/SNDR hi lo 0 m/EOX])
                             (recur)))
                         (assoc this
                                :fast-chan fast-chan
                                :slow-chan slow-chan
                                :installed? true))))

  (stop [this]
    (stopping this
              :on installed?
              :action #(do (a/close! fast-chan)
                           (a/close! slow-chan)
                           (assoc this
                                  :fast-chan nil
                                  :slow-chan nil
                                  :installed? false)))))
