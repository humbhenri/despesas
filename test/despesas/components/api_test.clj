(ns despesas.components.api-test
  (:require [clojure.test :refer :all]
            [io.pedestal.http.route :as route]
            [com.stuartsierra.component :as component]
            [despesas.components.api :as api]
            [despesas.core :as core]
            [clj-http.client :as client]
            [clojure.string :as str]))

(def url-for (route/url-for-routes api/routes))

(defn sut->url [sut path]
  (str/join ["http://localhost:" (-> sut :api-component :config :server :port) path]))

(defn get-free-port []
  (with-open [socket (java.net.ServerSocket. 0)]
    (.getLocalPort socket)))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(deftest hello-test
  (with-system
      [sut (core/api-system {:server {:port (get-free-port)}})]
    (is (= {:body "Hello world!" :status 200}
           (-> (client/get (sut->url sut (url-for :hello)))
               (select-keys [:body :status]))))))

(deftest get-despesa-test
  (with-system
    [sut (core/api-system {:server {:port (get-free-port)}})]
    (let [id-1 (random-uuid)
          despesa-1 {:id id-1 :content "pão comprado"}]
      (reset! (-> sut :memory-state-component :state-atom)
              [despesa-1])
      (-> (client/get (sut->url sut (url-for :get-despesa {:path-params {:id id-1}})))
          (select-keys [:body :status])
          (is (= {:body {:content "pão comprado"}
                  :status 200})))
      (testing "Empty body return form random id"
        (-> (client/get (sut->url sut (url-for :get-despesa {:path-params {:id (random-uuid)}})))
            (select-keys [:body :status])
            (is (= {:body ""
                    :status 200})))))))
