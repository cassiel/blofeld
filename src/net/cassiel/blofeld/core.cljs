(ns net.cassiel.blofeld.core
  (:require [com.stuartsierra.component :as component]
            [net.cassiel.blofeld.component.max-api :as max-api]
            [net.cassiel.blofeld.component.handlers :as handlers]
            [net.cassiel.blofeld.component.presets :as presets]
            [net.cassiel.blofeld.component.storage :as storage]
            ))

(defn system []
  (component/system-map :max-api (max-api/map->MAX_API {})
                        :storage (component/using (storage/map->STORAGE {})
                                                  [:max-api])
                        :presets (component/using (presets/map->PRESETS {})
                                                  [:max-api :storage])
                        :handlers (component/using (handlers/map->HANDLERS {})
                                                   [:max-api :presets])))

(defonce S (atom (system)))

(defn start []
  (swap! S component/start))

(defn stop []
  (swap! S component/stop))

#_ (reset! S (system))
