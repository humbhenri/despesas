(ns despesas.html.root
  (:require [hiccup2.core :as h]
            [hiccup.page :as hp]
            [hiccup.element :as he]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]))

(defn ok
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (->
          (h/html {:mode :html}
                  (hp/doctype :html5)
                  [:html {:lang "pt-br"} body])
          (str))})

(defn- layout
  [body]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:content "width=device-width, initial-scale=1.0" :name "viewport"}]
   [:title "Despesas | Home"]
   (hp/include-js
    "https://cdn.tailwindcss.com"
    "https://unpkg.com/htmx.org@1.9.4?plugins=forms")
   [:body
    [:div {:class "container mx-auto mt-10"}
     [:h1 {:class "text-2xl font-bold loading-7 mb-5 text-gray-900"}
      "Bem vindo às suas Despesas"]
     body]]])

(def page-size 10)

(defn get-despesas [data-source]
  (let [query (-> {:select :* :from [[:despesas :d]] :order-by [[:d.created_at :desc]]} (sql/format))
        despesas (-> (jdbc/execute! (data-source) query {:builder-fn rs/as-unqualified-lower-maps}))
        th-class {:scope "col" :class "px-6 py-3 text-left text-xs font-medium uppercase text-gray-500"}]
    [:table {:class "min-w-full divide-y divide-gray-200 border border-gray-200"}
     [:thead {:class "bg-gray-50"}
      [:tr
       [:th th-class "Nome"]
       [:th {:scope "col" :class "px-6 py-3 text-left text-xs font-medium uppercase text-gray-500"} "Valor"]]]
     [:tbody {:class "divide-y divide-gray-200 bg-white"}
      (->> despesas
           (map (fn [{:keys [content val]}]
                  [:tr {:class "hover:bg-gray-50"}
                   [:td {:class "whitespace-nowrap px-6 text-gray-500"} content]
                   [:td {:class "whitespace-nowrap px-6 text-gray-500"} val]])))]]))

(def html-root-handler
  {:name :html-root
   :enter (fn [{:keys [dependencies] :as context}]
            (let [{:keys [data-source]} dependencies
                  response (-> [:div (get-despesas data-source)] (layout) (ok))]
              (assoc context :response response)))})
