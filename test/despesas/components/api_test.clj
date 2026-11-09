(ns despesas.components.api-test
  (:require [clojure.test :refer :all]
            [io.pedestal.http.route :as route]
            [com.stuartsierra.component :as component]
            [despesas.components.api :as api]
            [despesas.core :as core]
            [clj-http.client :as client]))

(def url-for (route/url-for-routes api/routes))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(deftest hello-test
  (with-system
      [sut (core/api-system {:server {:port 8088}})]
    (is (= {:body "Hello world!" :status 200}
           (-> (client/get "http://localhost:8088/hello")
               (select-keys [:body :status]))))))

(deftest get-despesa-test
  (with-system
    [sut (core/api-system {:server {:port 8088}})]
    (let [id-1 (random-uuid)
          despesa-1 {:id id-1 :content "pão comprado"}]
      (reset! (-> sut :memory-state-component :state-atom)
              [despesa-1])
      (-> (client/get (str "http://localhost:8088/despesas/" id-1))
          (select-keys [:body :status])
          (is (= {:body {:content "pão comprado"}
                  :status 200}))))))
