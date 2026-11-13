(ns despesas.components.api
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [io.pedestal.http.content-negotiation :as content-negotation]
            [io.pedestal.http.body-params :as body-params]
            [cheshire.core :as json]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.tools.logging :as log]
            [honey.sql :as sql]
            [clojure.pprint :refer [pprint]]
            [despesas.html.root :refer [html-root-handler view-despesas-items-handler view-despesa-item-handler
                                        edit-despesa-item-handler update-despesa-item-handler]]
            [despesas.model :refer [validate-despesa]]))

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

(defn spy [x]
  (pprint x)
  x)

(def post-despesa-handler
  {:name :post-despesa
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  despesa (validate-despesa (:json-params request))
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

(def generic-error-interceptor
  (interceptor/interceptor
   {:name ::generic-error-interceptor
    :error (fn [context exception]
             (let [message (.getMessage exception)]
               (assoc context :response (response 500 {:message message}))))}))

(defn find-sql-exception [^Throwable t]
  (loop [cause t]
    (cond
      (nil? cause) nil
      (instance? java.sql.SQLException cause) cause
      :else (recur (.getCause cause)))))

(def database-error-interceptor
  (interceptor/interceptor
   {:name ::database-error-interceptor
    :error
    (fn [context exception]
      (let [sql-ex (find-sql-exception exception)]
        (if sql-ex
          (let [sql-state (.getSQLState ^java.sql.SQLException sql-ex)]
            (case sql-state
              "23505" (assoc context :response
                             {:status 409 :body "Registro já cadastrado."})

              "23503" (assoc context :response
                             {:status 409 :body "Registro possui dependências."})

              "23502" (assoc context :response
                             {:status 400 :body "Campo obrigatório não informado."})

              (assoc context :response
                     {:status 500 :body "Erro ao executar operação no banco de dados."})))
          (chain/with-error context exception))))}))

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
     ["/info" :get info-handler :route-name :info]
     ["/" :get html-root-handler :route-name :html-root]
     ["/view/despesas/items" :get view-despesas-items-handler :route-name :view-despesas-items]
     ["/view/despesas/item/:id" :get view-despesa-item-handler :route-name :view-despesa-item]
     ["/view/despesas/item/:id/edit" :get edit-despesa-item-handler  :route-name :edit-despesa-item]
     ["/view/despesas/item/:id/edit" :put [(body-params/body-params) update-despesa-item-handler]  :route-name :update-despesa-item]}))

(defn inject-dependencies [dependencies]
  (interceptor/interceptor
   {:name ::inject-dependencies
    :enter (fn [context]
             (assoc context :dependencies dependencies))}))

(defrecord ApiComponent [config memory-state-component data-source]
  component/Lifecycle

  (start [component]
    (log/info "Starting Api component")
    (let [server (-> {::http/join? false
                      ::http/routes routes
                      ::http/type :jetty
                      ::http/port (-> config :server :port)
                      ::http/secure-headers {:content-security-policy-settings {:object-src "none"}}}
                     (http/default-interceptors)
                     (update ::http/interceptors concat [(inject-dependencies component)
                                                         content-negotiation-interceptor
                                                         database-error-interceptor
                                                         generic-error-interceptor])
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
