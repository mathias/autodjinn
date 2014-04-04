(ns autodjinn.gmail-ingestion
  (:require [autodjinn.core :refer :all]
            [clojure-mail.core :refer :all]
            [clojure-mail.message :as message :refer [read-message]]
            [clojure.java.io :as io]))

(def mail-config autodjinn.core/config)

(def gmail-username (get (mail-config) :gmail-username))
(def gmail-password (get (mail-config) :gmail-password))

(defn get-sent-date
  "Returns an instant for the date sent"
  [msg]
  (.getSentDate msg))

(defn get-received-date
  "Returns an instant for the date sent"
  [msg]
  (.getReceivedDate msg))

(defn cc-list
  "Returns a sequence of CC-ed recipients"
  [msg]
  (map str
    (.getRecipients msg javax.mail.Message$RecipientType/CC)))

(defn bcc-list
  "Returns a sequence of BCC-ed recipients"
  [msg]
  (map str
    (.getRecipients msg javax.mail.Message$RecipientType/BCC)))

(defn simple-content-type [full-content-type]
  (-> full-content-type
      (clojure.string/split #"[;]")
      (first)
      (clojure.string/lower-case)))

(defn is-content-type? [body requested-type]
  (= (simple-content-type (:content-type body))
     requested-type))

(defn find-body-of-type [bodies type]
  (:body (first (filter #(is-content-type? %1 type) bodies))))

(defn get-text-body [msg]
  (find-body-of-type (message/message-body msg) "text/plain"))

(defn get-html-body [msg]
  (find-body-of-type (message/message-body msg) "text/html"))

(defn remove-angle-brackets
  [string]
  (-> string
      (clojure.string/replace ">" "")
      (clojure.string/replace "<" "")))

(def my-store (gen-store gmail-username gmail-password))

(defn db-attrs-for [msg]
  {:mail/uid (remove-angle-brackets (message/id msg))
   :mail/from (message/from msg)
   :mail/to (message/to msg)
   :mail/cc (cc-list msg)
   :mail/bcc (bcc-list msg)
   :mail/subject (message/subject msg)
   :mail/date-sent (get-sent-date msg)
   :mail/date-received (get-received-date msg)
   :mail/text-body (get-text-body msg)
   :mail/html-body (get-html-body msg)})

(defn ingest-inbox []
  (doseq [msg (inbox my-store)]
    (println (message/subject msg))
    @(create-mail (db-attrs-for msg))))

(defn -main
  "Perform a Gmail ingestion"
  []
  (println "Gmail ingestion starting up")
  (println "Attmpting to update the schema")
  (update-schema)
  (println "Beginning email ingestion")
  (ingest-inbox)
  (println "Done ingesting")
  (System/exit 0))
