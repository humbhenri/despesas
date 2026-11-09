(ns despesas.components.memory-state
  (:require [com.stuartsierra.component :as component]))

(defrecord MemoryStateComponent [config]
  component/Lifecycle

  (start [component]
    (println "Starting MemoryState component")
    (assoc component :state-atom (atom [])))

  (stop [component]
    (println "Stopping MemoryState component")
    (assoc component :state-atom nil)))

(defn new-memory-state-component [config]
  (map->MemoryStateComponent {:config config}))
