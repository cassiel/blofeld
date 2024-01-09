(ns user
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [net.cassiel.blofeld.manifest :as m]
            [net.cassiel.blofeld.incoming :as in]
            [net.cassiel.blofeld.midi :as midi]
            [net.cassiel.blofeld.sysex-in :as sysex-in]
            [net.cassiel.blofeld.async-tools :as tt]
            [net.cassiel.blofeld.core :as core]
            [net.cassiel.blofeld.component.storage :as storage]
            [net.cassiel.blofeld.component.presets :as presets]
            [clojure.core.match :refer [match]]
            [cljs.core.async :refer [put! chan <! close!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [goog.string :as gstring]
            [goog.string.format]))

(def max-api (js/require "max-api"))

(storage/patch-index-tag 3 0)

(.getDict max-api "BLOFELD")

(nth (range 100) 33)

(nth "ABC" 1)

(js/Date.)

;; (a/extend-promises-as-pair-channels!)

(def *STATE* (atom {}))

;; Channel pair for slowdown. message is [hi lo] for program change.
(def in-ch (chan))
(def out-ch (chan))

(let [number (.-MESSAGE_TYPES.NUMBER max-api)]
  (dorun (map #(.removeHandlers max-api %) [number "ctlin" "pgmin"]))
  (doto max-api
    (.addHandler number #(in/handle-byte *STATE* max-api %))
    (.addHandler "ctlin" #(in/handle-ctlin *STATE* %1 %2))
    (.addHandler "pgmin" #(in/handle-pgmin *STATE* in-ch %1))))

(tt/slowdown in-ch out-ch)

(go-loop []
  (when-let [[hi lo] (<! out-ch)]
    (println "Out of slowdown" [hi lo])
    ;; In fact, [127 0] would work here because we've just loaded the edit buffer with the patch:
    (midi/output-seq max-api [m/SOX m/WALDORF m/BLOFELD m/BROADCAST-ID m/SNDR hi lo 0 m/EOX])
    (recur)))


;; Test: basic program change, channel 1 (but doesn't switch bank):

(midi/output-seq max-api [0xC0 0])



(deref *STATE*)


(-> (deref *STATE*)
    :location)

;; --- Channel timer test

(close! in-ch)
(close! out-ch)

(js/require "max-api")

(gstring/format "%03d" 99)


;; ----- COMPONENT

(presets/matches (seq [99]) [99])


;; -----

(reset! core/S (core/system))

(core/start)
(core/stop)

;; ----- DICTIONARIES

(keys {:A 1 :B 2})

(let [max-api (-> (deref core/S) :max-api :max-api)]
  (go
    (<p! (.updateDict max-api "BLOFELD" "a.b.c" (js/Date.)))))

(let [max-api (-> (deref core/S) :max-api :max-api)]
  (go
    (let [d (<p! (.getDict max-api "BLOFELD"))]
      (println (js->clj d :keywordize-keys true)))))
