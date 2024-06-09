(ns net.cassiel.blofeld.component.storage
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [com.stuartsierra.component :as component]
            [net.cassiel.lifecycle :refer [starting stopping]]
            [net.cassiel.blofeld.manifest :as m]
            [net.cassiel.blofeld.component.max-api :as max-api]
            [oops.core :refer [ocall]]
            [cljs.core.async :as a :refer [>!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [goog.string :as gstring]
            [goog.string.format]))

(defn patch-name
  "Extract patch name, trimming spaces. `bytes` is 0xF0 onwards."
  [bytes]
  (let [name-bytes (->> bytes
                        (drop m/SNDD-NAME-OFFSET)
                        (take m/SNDD-NAME-LENGTH))]
    (clojure.string/trim (apply str (map char name-bytes)))))

(defn patch-index-tag
  "The index form (e.g. `A001`) displayed on the device. We index pgm from zero."
  [bank pgm]
  (gstring/format "%s%03d"
                  (nth "ABCDEFGH" bank)
                  (inc pgm))
  )

(defn params-only
  "Remove header information (0xF0 onwards) and checksum. (We never see 0xF7.)"
  [bytes]
  (->> bytes
       (drop m/SNDD-DATA-START) ; Lose 0xF0 and all header information.
       (drop-last)              ; Lost checksum.
       )
  )

(defn handle-preset
  "'Store' a preset. Send it out to the dictionary, but also adopt it as our edit buffer.
   `bytes` starts from 0xF0, includes everything up to but excluding 0xF7.
   We strip out all header information and checksum.
   (The byte data might well encode a location as well, which we ignore and discard.)
   For now, storage just means outputting a dictionary with a single entry."
  [storage bank pgm param-bytes]
  (let [name    (patch-name param-bytes)
        tag     (patch-index-tag bank pgm)
        max-api (:max-api storage)]
    (go (<p! (ocall (:max-api max-api) :updateDict m/BLOFELD-DICT #js ["patches" tag] (clj->js {:name name
                                                                                                :data param-bytes
                                                                                                :time (js/Date.)}))))
    )
  )

(defrecord STORAGE [max-api installed?]
  Object
  (toString [this] "STORAGE")

  component/Lifecycle
  (start [this]
    (starting this
              :on installed?
              :action #(assoc this
                              :installed? true)))

  (stop [this]
    (stopping this
              :on installed?
              :action #(assoc this
                              :installed? false))))
