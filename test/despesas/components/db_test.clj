(ns despesas.components.db-test
  (:require [clojure.java.shell :refer [sh]]
            [clojure.test :refer [use-fixtures deftest is]]))

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
      (run-db-command "stop-database"))))

(use-fixtures :once database-fixture)

(deftest simple-test
  (is (= 1 1)))
