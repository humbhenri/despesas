(ns despesas.components.api
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as content-negotation]
            [io.pedestal.http.body-params :as body-params]
            [cheshire.core :as json]
            [schema.core :as s]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.tools.logging :as log]
            [honey.sql :as sql]
            [clojure.pprint :refer [pprint]]))

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
            (let [request (:request context)
                  id (-> request :path-params :id (parse-uuid))
                  {:keys [data-source]} dependencies
                  query (-> {:select :* :from :despesas :where [:= :id id]} (sql/format))
                  despesa (-> (jdbc/execute! (data-source) query {:builder-fn rs/as-unqualified-lower-maps}) first)
                  response (if despesa (ok despesa) (not-found))]
              (assoc context :response response)))})

(defn save-despesa! [{:keys [memory-state-component]} despesa]
  (swap! (:state-atom memory-state-component) conj despesa))

(s/defschema Despesa
  {:content s/Str
   :val s/Num})

(defn spy [x]
  (pprint x)
  x)

(def post-despesa-handler
  {:name :post-despesa
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  despesa (s/validate Despesa (:json-params request))
                  content (:content despesa)
                  val (:val despesa)
                  {:keys [data-source]} dependencies
                  query (-> {:insert-into :despesas
                             :columns [:content :val]
                             :values [[content val]]
                             :returning :*}
                            (sql/format))
                  results (jdbc/execute! (data-source) query
                                         {:builder-fn rs/as-unqualified-lower-maps})]
              (assoc context :response (created (first results)))))})

(def content-negotiation-interceptor (content-negotation/negotiate-content ["application/json"]))

(def info-handler
  {:name :info
   :enter (fn [{:keys [dependencies] :as context}]
            (let [{:keys [data-source]} dependencies
                  db-response (:server_version (first (jdbc/execute! (data-source) ["show server_version"])))]
              (assoc context :response (ok (str "Database version: " db-response)))))})

(def get-despesas-handler
  {:name :get-despesas
   :enter (fn [{:keys [dependencies] :as context}]
            (let [{:keys [data-source]} dependencies
                  query (-> {:select :* :from :despesas} (sql/format))
                  despesas (jdbc/execute! (data-source) query {:builder-fn rs/as-unqualified-lower-maps})]
              (assoc context :response (ok despesas))))})

(def routes
  (route/expand-routes
   #{["/hello" :get hello :route-name :hello]
     ["/despesas/:id" :get get-despesa-handler :route-name :get-despesa]
     ["/despesas" :post [(body-params/body-params) post-despesa-handler] :route-name :post-despesa]
     ["/despesas" :get get-despesas-handler :route-name :get-despesas]
     ["/info" :get info-handler :route-name :info]}))

(defn inject-dependencies [dependencies]
  (interceptor/interceptor
   {:name ::inject-dependencies
    :enter (fn [context]
             (assoc context :dependencies dependencies))}))

(defrecord ApiComponent [config memory-state-component data-source]
  component/Lifecycle

  (start [component]
    (log/info "Starting Api component")
    (let [server (-> {::http/join? false ::http/routes routes ::http/type :jetty ::http/port (-> config :server :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat [(inject-dependencies component) content-negotiation-interceptor])
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (log/info "Stopping Api component")
    (when-let [server (:server component)]
      (log/info "Stopping web server")
      (http/stop server))
    (assoc component :server nil)))

(defn new-api-component [config]
  (map->ApiComponent {:config config}))
