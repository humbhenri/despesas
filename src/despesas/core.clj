(ns despesas.core
  (:require [despesas.config :as config]
            [com.stuartsierra.component :as component]
            [despesas.components.api :as api]
            [despesas.components.memory-state :as memory-state]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [clojure.tools.logging :as log])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defn migrate-db [ds]
  (.migrate
   (.. (org.flywaydb.core.Flyway/configure)
       (dataSource ds)
       (locations (into-array String ["classpath:database/migrations"]))
       (table "schema_version")
       (load))))

(defn api-system [config]
  (let [[host port database user password] (-> config :db (select-keys [:host :port :database :user :password]) vals)
        db-spec {:jdbcUrl (str "jdbc:postgresql://" host ":" port "/" database)
                 :username user
                 :password password}]
    (component/system-map
     :memory-state-component (memory-state/new-memory-state-component config)
     :data-source (connection/component HikariDataSource
                                        (assoc db-spec :init-fn
                                               (fn [ds]
                                                 (log/info "Database init")
                                                 (migrate-db ds))))
     :api-component (component/using (api/new-api-component config) [:memory-state-component :data-source]))))
