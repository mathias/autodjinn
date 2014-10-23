(ns autodjinn.sent-counts
  (require [autodjinn.core :refer :all]
           [datomic.api :as d]))

(defn sent-count-hash-map
  [result type]
  {:db/id (d/tempid :db.part/user)
   :sent-count/from (first result)
   :sent-count/to (second result)
   :sent-count/count (last result)
   :sent-count/address-type type})

(def mail-entity-attrs {:to :mail/to
                        :cc :mail/cc
                        :bcc :mail/bcc})

(def sent-count-types {:to :sent-count.address-type/to
                       :cc :sent-count.address-type/cc
                       :bcc :sent-count.address-type/bcc})

(defn txns-for [db type]
  (let [find-pairs (count-address-pairs-q db (get mail-entity-attrs type))]
    (map #(sent-count-hash-map % (get sent-count-types type)) find-pairs)))

(defn create-sent-counts
  []
  (let [db (new-db-val)
        to-txns (txns-for db :to)
        cc-txns (txns-for db :cc)
        bcc-txns (txns-for db :bcc)
        txns (concat to-txns cc-txns bcc-txns)]
    (d/transact db-connection txns)))

(defn sent-counts-to-address
  ([address] (sent-counts-to-address (new-db-val) address))
  ([db address]
     (d/q '[:find ?eid
            :in $ ?address
            :where [?eid :sent-count/to ?address]]
          db
          address)))

(defn sum-sent-counts-to-address
  ([address] (sum-sent-counts-to-address (new-db-val) address))
  ([db address]
     (d/q '[:find (sum ?count)
            :with ?eid
            :in $ ?address
            :where [?eid :sent-count/to ?address]
                   [?eid :sent-count/count ?count]]
          db
          address)))

(defn sent-counts-from-address
  ([address] (sent-counts-from-address (new-db-val) address))
  ([db address]
     (d/q '[:find ?eid
            :in $ ?address
            :where [?eid :sent-count/from ?address]]
          db
          address)))

(defn sum-sent-counts-from-address
  ([address] (sum-sent-counts-from-address (new-db-val) address))
  ([db address]
     (d/q '[:find (sum ?count)
            :with ?eid
            :in $ ?address
            :where [?eid :sent-count/from ?address]
                   [?eid :sent-count/count ?count]]
          db
          address)))

(defn sent-to-address-sums
  ([] (sent-to-address-sums (new-db-val)))
  ([db]
     (d/q '[:find ?address (sum ?count)
            :with ?eid
            :where [?eid :sent-count/to ?address]
                   [?eid :sent-count/count ?count]]
          db)))

(defn most-sent-to-address
  ([] (most-sent-to-address (new-db-val)))
  ([db]
     (let [sums (sent-to-address-sums)
           max-count (d/q '[:find (max ?count)
                            :where [?address ?count]]
                          sums)]
       (d/q '[:find ?address ?max-count
              :in $ ?max-count
              :where [?address ?max-count]]
            sums
            (ffirst max-count)))))
