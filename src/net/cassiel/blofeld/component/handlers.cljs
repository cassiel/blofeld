(ns net.cassiel.blofeld.component.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [com.stuartsierra.component :as component]
            [net.cassiel.lifecycle :refer [starting stopping]]
            [net.cassiel.blofeld.component.presets :as presets]
            [net.cassiel.blofeld.async-tools :as async-tools]
            [cljs.core.async :refer [>!]]))

(defn handle-sysex-byte
  "Handle a byte of sysex MIDI input."
  [presets *state* i]
  ;; This flushing will output a sysex as [F0 nn nn nn ...] and as [F7] separately.
  ;; We flush on status byte, so the F7 delivers the F0 message and vice versa.
  (when (>= i 0x80)
    (when-let [msg (:partial-sysex (deref *state*))]
      (presets/handle-sysex presets (reverse msg))
      (swap! *state* dissoc :partial-sysex)))

  ;; Bytes accumulated backwards (hence `reverse` above):
  (swap! *state* update :partial-sysex conj i))

(defn handle-ctlin
  "handle control change. The only ones we care about are 0 and 32 for bank select.
   In fact we only have 8 banks so val will be 0 [A]..7 [H], for ctl as 0."
  [*state* val ctl]
  (println "Got ctl v=" val "c=" ctl)
  (when (= ctl 0)
    (swap! *state* assoc :bank val)))

(defn handle-pgmin
  "Handle program change, probably following a bank select: programs indexed from 1
   (thank you, Max)."
  [presets *state* pgm]
  (let [bank (or (-> *state* deref :bank) 0)
        p0 (dec pgm)]
    (swap! *state* assoc :program p0)
    (println "Got pgm b=" bank "p=" p0)
    ;; FIX: call into data component instead.
    (presets/handle-preset-recall presets bank pgm)))

(defrecord HANDLERS [max-api presets installed?]
  Object
  (toString [this] "HANDLERS")

  component/Lifecycle
  (start [this]
    (starting this
              :on installed?
              :action (fn [] (let [max-api (:max-api max-api)
                                   number (.-MESSAGE_TYPES.NUMBER max-api)
                                   *state* (atom nil)]
                               (doto max-api
                                 (.addHandler number (partial handle-sysex-byte presets *state*))
                                 (.addHandler "ctlin" (partial handle-ctlin *state*))
                                 (.addHandler "pgmin" (partial handle-pgmin presets *state*)))
                               (assoc this
                                      :*state* *state*
                                      :installed? true)))))

  (stop [this]
    (stopping this
              :on installed?
              :action (fn [] (let [max-api (:max-api max-api)
                                   number (.-MESSAGE_TYPES.NUMBER max-api)]
                               (do (dorun (map #(.removeHandlers max-api %) [number "ctlin" "pgmin"]))
                                   (assoc this
                                          :*state* nil
                                          :installed? false)))))))
