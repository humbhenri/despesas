(ns dev
  (:require [com.stuartsierra.component.repl :as component-repl]
            [despesas.core :as core]
            [despesas.config :as config]))

(component-repl/set-init (fn [_old-system] (core/api-system (config/read-config))))
