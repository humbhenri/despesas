(ns despesas.components.db-test
  (:require [clojure.java.shell :refer [sh]]
            [clojure.test :refer [use-fixtures deftest is]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [com.stuartsierra.component :as component]
            [despesas.config :as config]
            [despesas.core :as core]
            [despesas.utils :refer [with-system]]))

(defn run-db-command [command]
  (let [{:keys [exit err]} (sh command)]
    (when-not (zero? exit)
      (throw (ex-info (str "Erro executando " command)
                      {:exit exit :err err})))))

(defn database-fixture [tests]
  (run-db-command "start-database")
  (try
    (tests)
    (finally
      #(run-db-command "stop-database"))))

(deftest test-db-is-alive
  (let [config (config/read-config)
        [host port database user password] (-> config :test :db (select-keys [:host :port :database :user :password]) vals)
        ds (jdbc/get-datasource {:jdbcUrl (str "postgresql://" host ":" port "/" database)
                                 :user user
                                 :password password})]
    (is (= {:r 1} (first (jdbc/execute! ds ["select 1 as r;"]))))))

(use-fixtures :once database-fixture)

(defn datasource-only-system [config]
  (component/system-map
   :data-source (core/new-datasource-component config)))

(deftest migration-test
  (with-system [sut (datasource-only-system (:test (config/read-config)))]
    (let [{:keys [data-source]} sut
          [schema-version :as schema-versions]
          (jdbc/execute! (data-source) ["select * from schema_version"]
                         {:builder-fn rs/as-unqualified-lower-maps})]
      (is (= 1 (count schema-versions)))
      (is (= {:description "add despesas tables"
              :script "V1__add_despesas_tables.sql"
              :success true}
             (select-keys schema-version [:description :script :success]))))))
