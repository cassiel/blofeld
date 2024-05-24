(ns net.cassiel.blofeld.component.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [com.stuartsierra.component :as component]
            [net.cassiel.lifecycle :refer [starting stopping]]
            [net.cassiel.blofeld.component.presets :as presets]
            [net.cassiel.blofeld.async-tools :as async-tools]
            [cljs.core.async :as async :refer [>!]]
            [oops.core :refer [ocall]]))

(defn handle-sysex-byte
  "Handle a byte of sysex MIDI input."
  [max-api presets *state* i]
  ;; This flushing will output a sysex as [F0 nn nn nn ...] and as [F7] separately.
  ;; We flush on status byte, so the F7 delivers the F0 message and vice versa.
  (when (>= i 0x80)
    (when-let [msg (:partial-sysex (deref *state*))]
      (presets/handle-sysex max-api presets (reverse msg))
      (swap! *state* dissoc :partial-sysex)))

  ;; Bytes accumulated backwards (hence `reverse` above):
  (swap! *state* update :partial-sysex conj i))

(defn handle-ctlin
  "handle control change. The only ones we care about are 0 and 32 for bank select.
   In fact we only have 8 banks so val will be 0 [A]..7 [H], for ctl as 0."
  [max-api *state* val ctl]
  (println "Got ctl v=" val "c=" ctl)
  (when (= ctl 0)
    (swap! *state* assoc :bank val)
    (ocall max-api :outlet "seen-bank" val)))

(defn handle-pgmin
  "Handle program change, probably following a bank select: programs indexed from 1
   (thank you, Max)."
  [presets *state* pgm]
  (let [bank (or (-> *state* deref :bank) 0)
        p0 (dec pgm)]
    (swap! *state* assoc :program p0)
    (println "Got pgm b=" bank "p=" p0)
    ;; FIX: call into data component instead.
    (presets/handle-preset-recall presets bank p0)))

(defn handle-force-bank
  "Blofeld doesn't reliably send bank select, so this manually puts us into the right
   bank according to what the Blofeld has selected.
   Bonus behaviour: it can also do a bank select regardless of the Blofeld selection.
   `bank` is 0..7 inclusive."
  [max-api presets *state* bank]
  (when-let [p0 (:program (deref *state*))]
    (swap! *state* assoc :bank bank)
    (println "Forcing bank=" bank "when p0=" p0)
    (doto max-api
      (ocall :outlet "ctlout" bank 0 1)
      (ocall :outlet "pgmout" (inc p0) 1))

    (presets/handle-preset-recall presets bank p0)))

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
                                 (ocall :addHandler number (partial handle-sysex-byte max-api presets *state*))
                                 (ocall :addHandler "ctlin" (partial handle-ctlin max-api *state*))
                                 (ocall :addHandler "pgmin" (partial handle-pgmin presets *state*))
                                 (ocall :addHandler "force-bank" (partial handle-force-bank max-api presets *state*)))
                               (assoc this
                                      :*state* *state*  ;; Plant that there in case we want to debug.
                                      :installed? true)))))

  (stop [this]
    (stopping this
              :on installed?
              :action (fn [] (let [max-api (:max-api max-api)
                                   number (.-MESSAGE_TYPES.NUMBER max-api)]
                               (do (dorun (map #(ocall max-api :removeHandlers %) [number "ctlin" "pgmin" "force-bank"]))
                                   (assoc this
                                          :*state* nil
                                          :installed? false)))))))
