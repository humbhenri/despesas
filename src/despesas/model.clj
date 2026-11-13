(ns despesas.model
  (:require [schema.core :as s]))

(s/defschema Despesa
  {:content s/Str
   :val s/Num})

(defn validate-despesa [despesa]
  (s/validate Despesa despesa))
