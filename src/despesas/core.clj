(ns despesas.core
  (:require [despesas.config :as config]
            [com.stuartsierra.component :as component]
            [despesas.components.api :as api]
            [despesas.components.memory-state :as memory-state]))

(defn api-system [config]
  (component/system-map
   :memory-state-component (memory-state/new-memory-state-component config)
   :api-component (component/using (api/new-api-component config) [:memory-state-component])))
