(ns despesas.components.api
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as content-negotation]
            [io.pedestal.http.body-params :as body-params]
            [cheshire.core :as json]
            [schema.core :as s]))

(defn response
  ([status]
   (response status nil))
  ([status body]
   (merge
    {:status status :headers {"Content-Type" "application/json"}}
    (when body {:body (json/encode body)}))))

(def ok (partial response 200))

(def not-found (partial response 404))

(def created (partial response 201))

(defn hello [req] {:status 200 :body "Hello world!"})

(defn get-despesa-by-id [{:keys [memory-state-component]} id]
  (->> @(:state-atom memory-state-component)
       (filter (fn [despesa] (= id (:id despesa))))
       (first)))

(def get-despesa-handler
  {:name :get-despesa
   :enter (fn [{:keys [dependencies] :as context}]
            (println "get-despesa-handler" dependencies)
            (let [request (:request context)
                  id (-> request :path-params :id (parse-uuid))
                  despesa (get-despesa-by-id dependencies id)
                  response (if despesa (ok (json/encode despesa)) (not-found))]
              (assoc context :response response)))})

(defn save-despesa! [{:keys [memory-state-component]} despesa]
  (swap! (:state-atom memory-state-component) conj despesa))

(s/defschema Despesa
  {:id s/Str
   :content s/Str})

(def post-despesa-handler
  {:name :post-despesa
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  despesa (s/validate Despesa (:json-params request))]
              (save-despesa! dependencies despesa)
              (assoc context :response (created despesa))))})

(def content-negotiation-interceptor (content-negotation/negotiate-content ["application/json"]))

(def routes
  (route/expand-routes
   #{["/hello" :get hello :route-name :hello]
     ["/despesas/:id" :get get-despesa-handler :route-name :get-despesa]
     ["/despesas" :post [(body-params/body-params) post-despesa-handler] :route-name :post-despesa]}))

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
                     (update ::http/interceptors concat [(inject-dependencies component) content-negotiation-interceptor])
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
