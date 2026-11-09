(ns despesas.core
  (:require [despesas.config :as config]
            [com.stuartsierra.component :as component]
            [despesas.components.api :as api]))

(defn api-system [config]
  (component/system-map
   :api-component (component/using (api/new-api-component config) [])))

;; (defn -main []
;;   (->> (config/read-config)
;;        start))
