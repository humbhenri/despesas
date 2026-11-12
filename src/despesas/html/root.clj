(ns despesas.html.root
  (:require [hiccup2.core :as h]
            [hiccup.page :as hp]
            [hiccup.element :as he]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]
            [clojure.tools.logging :as log])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatterBuilder]
           [java.time.temporal ChronoField]
           [java.text NumberFormat]
           [java.util Locale]))

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

(def parser
  (-> (DateTimeFormatterBuilder.)
      (.appendPattern "yyyy-MM-dd HH:mm:ss")
      (.appendFraction ChronoField/NANO_OF_SECOND 0 9 true)
      (.toFormatter)))

(def formatter
  (java.time.format.DateTimeFormatter/ofPattern "dd/MM/yyyy HH:mm"))

(defn format-date [date-str]
  (.format (LocalDateTime/parse date-str parser) formatter))

(defn format-money [value]
  (.format (NumberFormat/getCurrencyInstance
            (Locale/forLanguageTag "pt-BR"))
           (bigdec value)))

(defn get-despesas [page data-source]
  (let [query (-> {:select :*
                   :from [[:despesas :d]]
                   :order-by [[:d.created_at :desc]]
                   :offset (* page page-size)
                   :limit page-size}
                  (sql/format))
        despesas (-> (jdbc/execute! (data-source) query {:builder-fn rs/as-unqualified-lower-maps}))]
    despesas))

(defn loader [next-page]
  [:div.bg-red-100.p10
   {:hx-get (str "/view/despesas/items?page=" next-page)
    :hx-trigger "revealed"
    :hx-target "this"
    :hx-swap "outerHTML"}
   [:span (format "... loading page %s ..." next-page)]])

(defn item-component
  [{:keys [id
           content
           val
           created_at]}]
  [:article.p-6.even:bg-white.odd:bg-slate-100.sm:p-8.hover:scale-105.transition-transform.duration-200.cursor-pointer.text-right
   [:h2.break-all.text-lg.font-medium.sm:text-xl
    [:a.hover:underline
     {:href (str "/view/despesas/item/" id)}
     content]]
   [:p.mt-1.break-all.text-sm.text-gray-700
    (format-money val)]
   [:div.mt-4.text-xs.font-medium.text-gray-500
    (format-date (str created_at))]])

(defn view-despesas [despesas page]
  [:div.shadow-xl
   (map item-component despesas)
   (loader (inc page))])

(def html-root-handler
  {:name :html-root
   :enter (fn [{:keys [dependencies] :as context}]
            (let [initial-page 0
                  {:keys [data-source]} dependencies
                  despesas (get-despesas initial-page data-source)
                  response (-> [:div (view-despesas despesas initial-page)] (layout) (ok))]
              (assoc context :response response)))})

(def view-despesas-items-handler
  {:name :view-despesas-items
   :enter (fn [{:keys [dependencies] :as context}]
            (let [page (-> context :request :query-params :page parse-long)
                  {:keys [data-source]} dependencies
                  despesas (get-despesas page data-source)
                  response (-> (if (seq despesas) (view-despesas despesas page) "") (ok))]
              (log/info "despesas count:" (count despesas))
              (assoc context :response response)))})
