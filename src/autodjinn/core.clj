(ns autodjinn.core
  (:require [clojure-mail.core :refer :all]
            [clojure-mail.message :as message :refer [read-message]]
            [nomad :refer [defconfig]]
            [clojure.java.io :as io]
            [datomic.api :as d]))

(defconfig mail-config (io/resource "config/autodjinn-config.edn"))

(def gmail-username (get (mail-config) :gmail-username))
(def gmail-password (get (mail-config) :gmail-password))

(def db-uri (get (mail-config) :db-uri))

;; Always try to create the database
(d/create-database db-uri)

(def connection (d/connect db-uri))


(defn db [] (d/db connection))

(def schema-txn
  [{:db/id #db/id[:db.part/db]
    :db/ident :mail/uid
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/index true
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/to
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/fulltext true
    :db/index true
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/cc
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/fulltext true
    :db/index true
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/from
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/fulltext true
    :db/index true
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/subject
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/fulltext true
    :db/index true
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/date-sent
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/date-received
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/text-body
    :db/valueType :db.type/string
    :db/fulltext true
    :db/index true
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :mail/html-body
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])

(defn update-schema []
  (d/transact connection schema-txn))

(defn cc-list
  "Returns a sequence of CC-ed receivers"
  [m]
  (map str
    (.getRecipients m javax.mail.Message$RecipientType/CC)))

(defn get-sent-date
  "Returns an instant for the date sent"
  [msg]
  (.getSentDate msg))

(defn get-received-date
  "Returns an instant for the date sent"
  [msg]
  (.getReceivedDate msg))

(defn simple-content-type [full-content-type]
  (-> full-content-type
      (clojure.string/split #"[;]")
      (first)
      (clojure.string/lower-case)))

(defn is-content-type? [body requested-type]
  (= (simple-content-type (:content-type body))
     requested-type))

(defn get-text-body [msg]
  (let [contents (message/message-body msg)]
    (:body (first (filter #(is-content-type? %1 "text/plain") contents)))))

(defn get-html-body [msg]
  (let [contents (message/message-body msg)]
    (:body (first (filter #(is-content-type? %1 "text/html") contents)))))


(defn remove-angle-brackets
  [string]
  (-> string
      (clojure.string/replace ">" "")
      (clojure.string/replace "<" "")))


(def my-store (gen-store gmail-username gmail-password))

(defn ingest-inbox []
  (doseq [msg (inbox my-store)]
    (println (message/subject msg))
    @(d/transact connection [{:db/id (d/tempid "db.part/user")
                              :mail/uid (remove-angle-brackets (message/id msg))
                              :mail/from (message/from msg)
                              :mail/to (message/to msg)
                              :mail/cc (cc-list msg)
                              :mail/subject (message/subject msg)
                              :mail/date-sent (get-sent-date msg)
                              :mail/date-received (get-received-date msg)
                              :mail/text-body (get-text-body msg)
                              :mail/html-body (get-html-body msg)}])))
