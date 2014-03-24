(ns autodjinn.core
  (:require [clojure-mail.core :refer :all]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]))

(defconfig mail-config (io/resource "config/mail-config.edn"))

(def gmail-username (get (mail-config) :gmail-username))
(def gmail-password (get (mail-config) :gmail-password))

(defn auth-ok? []
  (= com.sun.mail.imap.IMAPSSLStore
     (class (gmail-store gmail-username gmail-password))))

(defn download-inbox [limit]
  (with-store (gmail-store gmail-username gmail-password)
    (doseq [msg (inbox limit)]
      (println (str "Saving: " (:subject msg))))))
