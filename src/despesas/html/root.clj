(ns despesas.html.root
  (:require [hiccup2.core :as h]
            [hiccup.page :as hp]
            [hiccup.element :as he]
            [clojure.string :as str]))

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

(def despesas [
               {:content "ovos" :val 11.5}
               {:content "ovos" :val 11.5}
               {:content "ovos" :val 11.5}
               {:content "ovos" :val 11.5}
               {:content "ovos" :val 11.5}
               {:content "carne" :val 91.5}
               {:content "internet" :val 19.5}
               {:content "internet" :val 19.5}
               {:content "internet" :val 19.5}
               {:content "internet" :val 19.5}
               {:content "internet" :val 19.5}
               ])

(defn get-despesas []
  [:table {:class "min-w-full divide-y divide-gray-200 border border-gray-200"}
   [:thead {:class "bg-gray-50"}
    [:tr
     [:th {:scope "col" :class "px-6 py-3 text-left text-xs font-medium uppercase text-gray-500"} "Nome"]
     [:th {:scope "col" :class "px-6 py-3 text-left text-xs font-medium uppercase text-gray-500"} "Valor"]
     ]]
   [:tbody {:class "divide-y divide-gray-200 bg-white"}
    (->> despesas
         (map (fn [{:keys [content val]}]
                [:tr {:class "hover:bg-gray-50"}
                 [:td {:class "whitespace-nowrap px-6 text-gray-500"} content]
                 [:td {:class "whitespace-nowrap px-6 text-gray-500"} val]])))]])

(def html-root-handler
  {:name :html-root
   :enter (fn [{:keys [dependencies] :as context}]
            (let [response (-> [:div (get-despesas)] (layout) (ok))]
              (assoc context :response response)))})
