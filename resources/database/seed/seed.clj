(ns database.seed.seed
  (:require [next.jdbc :as jdbc]
            [clojure.java.io :as io]
            [despesas.config :as config]))

(defn get-datasource [config]
  (let [[host port database user password] (-> config :db (select-keys [:host :port :database :user :password]) vals)
        ds (jdbc/get-datasource {:jdbcUrl (str "postgresql://" host ":" port "/" database)
                                 :user user
                                 :password password})]
    ds))

(defn run-seed []
  (let [config (config/read-config)
        ds (get-datasource config)
        sql (slurp (io/resource "database/seed/despesas.sql"))]
    (jdbc/execute! ds [sql])))
