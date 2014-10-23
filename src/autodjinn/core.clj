(ns autodjinn.core
  (:require [nomad :refer [defconfig]]
            [clojure.java.io :as io]
            [datomic.api :as d]))

(defconfig config (io/resource "config/autodjinn-config.edn"))

(def db-uri (get (config) :db-uri))

;; Always try to create the database
(d/create-database db-uri)

(def db-connection (d/connect db-uri))

(defn new-db-val [] (d/db db-connection))

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
    :db/ident :mail/bcc
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
  (d/transact db-connection schema-txn))

(defn create-mail [attrs]
  (d/transact db-connection
              [(merge {:db/id (d/tempid "db.part/user")}
                      attrs)]))

(defn first-entity
  ([datomic-eid-vec] (first-entity (new-db-val) datomic-eid-vec))
  ([db datomic-eid-vec]
     (d/entity db (first datomic-eid-vec))))

(defn count-address-pairs-q
  ([attr] (count-address-pairs-q (new-db-val) attr))
  ([db attr]
     (d/q '[:find ?from ?to (count ?combined)
            :with ?eid
            :where [?eid ?from ?to]
                   [(vector ?from ?to) ?combined]]
          (d/q '[:find ?eid ?from ?to
                 :in $ ?attr
                 :where [?eid :mail/from ?from]
                        [?eid ?attr ?to]]
               db
               attr))))

(defn count-from-to-pairs
  ([] (count-from-to-pairs (new-db-val)))
  ([db] (count-address-pairs-q db :mail/to)))

(defn count-from-cc-pairs
  ([] (count-from-cc-pairs (new-db-val)))
  ([db] (count-address-pairs-q db :mail/cc)))

(defn count-from-bcc-pairs
  ([] (count-from-bcc-pairs (new-db-val)))
  ([db] (count-address-pairs-q db :mail/bcc)))
