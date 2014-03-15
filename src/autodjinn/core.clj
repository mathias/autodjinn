(ns autodjinn.core
  (:require [clojure-mail.core :refer :all]))

(def env
  {:username ""
   :password ""})

(defn get-mail []
  (auth! (:username env) (:password env))
  (gen-store)
  (first (inbox 1)))
