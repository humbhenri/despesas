(ns despesas.components.api-test
  (:require [clojure.test :refer :all]
            [io.pedestal.http.route :as route]
            [com.stuartsierra.component :as component]
            [despesas.components.api :as api]
            [despesas.utils :refer [with-system]]
            [despesas.core :as core]
            [despesas.config :as config]
            [clj-http.client :as client]
            [clojure.string :as str]
            [cheshire.core :as json]))

(def url-for (route/url-for-routes api/routes))

(defn sut->url [sut path]
  (str/join ["http://localhost:" (-> sut :api-component :config :server :port) path]))

(defn get-free-port []
  (with-open [socket (java.net.ServerSocket. 0)]
    (.getLocalPort socket)))

(defn new-config []
  (let [config (config/read-config)]
    (-> config
        (assoc :db (:db (:test config)))
        (assoc :server {:port (get-free-port)}))))

(deftest hello-test
  (with-system
    [sut (core/api-system (new-config))]
    (is (= {:body "Hello world!" :status 200}
           (-> (client/get (sut->url sut (url-for :hello)))
               (select-keys [:body :status]))))))

(deftest content-negotiation-test
  (with-system
    [sut (core/api-system (new-config))]
    (is (= {:status 406}
           (-> (client/get (sut->url sut (url-for :hello)) {:accept :edn :throw-exceptions false})
               (select-keys [:status]))))))

(deftest get-despesas-test
  (with-system
    [sut (core/api-system (new-config))]
    (let [url (-> (sut->url sut (url-for :get-despesas)))
          response (client/get url)]
      (is (= 200 (:status response))))))

(deftest post-despesa-test
  (with-system
    [sut (core/api-system (new-config))]
    (let [url (-> (sut->url sut (url-for :post-despesa)))
          despesa {:content "test" :val 1}
          response (client/post url {:content-type :json :body (json/encode despesa)})]
      (is (= 201 (:status response)))
      (testing "get-despesa-test"
        (let [decoded (json/decode (:body response))
              id (-> decoded (select-keys ["id"]) (vals) (first))
              get-url (-> (sut->url sut (url-for :get-despesa {:path-params {:id id}})))
              get-response (client/get get-url)]
          (is (= 200 (:status get-response))))))))

(deftest get-despesa-not-found-test
  (with-system
    [sut (core/api-system (new-config))]
    (let [id (random-uuid)
          get-url (->> (url-for :get-despesa {:path-params {:id id}}) (sut->url sut))
          get-response (client/get get-url {:throw-exceptions false})]
      (is (= 404 (:status get-response))))))
