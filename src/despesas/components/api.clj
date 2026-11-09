(ns despesas.components.api
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defn hello [req] {:status 200 :body "Hello world! Clojure sucks!"})

(def routes (route/expand-routes #{["/hello" :get hello :route-name :hello]}))

(defrecord ApiComponent [config]
  component/Lifecycle

  (start [component]
    (println "Starting Api component")
    (let [server (-> {::http/join? false ::http/routes routes ::http/type :jetty ::http/port (-> config :server :port)}
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (println "Stopping Api component")
    (when-let [server (:server component)]
      (println "Stopping web server")
      (http/stop server))
    (assoc component :server nil)))

(defn new-api-component [config]
  (map->ApiComponent {:config config}))
