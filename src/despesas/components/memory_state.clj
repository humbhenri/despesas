(ns despesas.components.memory-state
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]))

(defrecord MemoryStateComponent [config]
  component/Lifecycle

  (start [component]
    (log/info "Starting MemoryState component")
    (assoc component :state-atom (atom [])))

  (stop [component]
    (log/info "Stopping MemoryState component")
    (assoc component :state-atom nil)))

(defn new-memory-state-component [config]
  (map->MemoryStateComponent {:config config}))
