(ns despesas.components.api
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]))

(defn response [status body]
  {:status status :body body :headers nil})

(def ok (partial response 200))

(defn hello [req] {:status 200 :body "Hello world!"})

(defn get-despesa-by-id [{:keys [memory-state-component]} id]
  (->> @(:state-atom memory-state-component)
       (filter (fn [despesa] (= id (:id despesa))))))

(def get-despesa-handler
  {
   :name :get-despesa
   :enter (fn [{:keys [dependencies] :as context}]
            (println "get-despesa-handler" dependencies)
            (let [request (:request context)
                  id (-> request :path-params :id (parse-uuid))
                  response (ok (get-despesa-by-id dependencies id))]
              (assoc context :response response)))})

(def routes
  (route/expand-routes
   #{["/hello" :get hello :route-name :hello]
     ["/despesas/:id" :get get-despesa-handler :route-name :get-despesa]}))

(defn inject-dependencies [dependencies]
  (interceptor/interceptor
   {:name ::inject-dependencies
    :enter (fn [context]
             (assoc context :dependencies dependencies))}))


(defrecord ApiComponent [config memory-state-component]
  component/Lifecycle

  (start [component]
    (println "Starting Api component")
    (let [server (-> {::http/join? false ::http/routes routes ::http/type :jetty ::http/port (-> config :server :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat [(inject-dependencies component)])
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
