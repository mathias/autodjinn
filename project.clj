(defproject autodjinn "0.1.0"
  :description "An email analysis tool"
  :url "https://github.com/mathias/autodjinn"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-pro "0.9.4384"]
                 [clojure-mail "0.1.6"]
                 [jarohen/nomad "0.6.3"]]
  :plugins [[lein-datomic "0.2.0"]]
  :profiles {:dev
             {:datomic {:config "resources/sql-transactor-template.properties"
                        :db-uri "datomic:sql://autodjinn?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic"}}})
