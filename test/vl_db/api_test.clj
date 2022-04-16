(ns vl-db.api-test
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Tests the complete API set."}
  (:require [vl-db.configure :as c]
            [vl-db.core :as db]
            [clojure.test :refer :all]))

(def conf (c/config  {:usr (System/getenv "CAL_USR")
                      :pwd (System/getenv "CAL_PWD")}))



(deftest server-up-i
  (testing "the server is reachable"
    (is (db/up? conf))))

(def N 2)
(def v (db/get-uuids N conf))

(deftest uuids-i
  (testing "returns a vector of length N"
    (is (= N (count v)))
    (is (= 1 (count (db/get-uuids conf))))))

;; use first v as database name
(def db-name (first v))
  
;; use second v as document name
(def id (second v))

(deftest doc-id-and-name-i
  (testing "is string"
    (is (string? id))
    (is (string? db-name))))

(deftest gen-database-i
  (testing "a database can be generated only once"
    (= (db/put-db (assoc conf :name db-name))
       {:ok true})
    (= (db/put-db (assoc conf :name db-name))
       {:error "clj-http: status 412"})))

(deftest del-database-i
  (testing "a database can be deleted only once"
    (= (db/del-db (assoc conf :name db-name))
       {:ok true})
    (= (db/del-db (assoc conf :name db-name))
       {:error "clj-http: status 404"})))


