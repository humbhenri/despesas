(ns despesas.components.db-test
  (:require [clojure.java.shell :refer [sh]]
            [clojure.test :refer [use-fixtures deftest is]]
            [next.jdbc :as jdbc]
            [despesas.config :as config]))

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

(use-fixtures :once database-fixture)

(deftest simple-test
  (let [config (config/read-config)
        [host port database user password] (-> config :test :db (select-keys [:host :port :database :user :password]) vals)
        ds (jdbc/get-datasource {:jdbcUrl (str "postgresql://" host ":" port "/" database)
                                 :user user
                                 :password password})]
    (is (= {:r 1} (first (jdbc/execute! ds ["select 1 as r;"]))))))
