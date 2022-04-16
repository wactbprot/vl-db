(ns vl-db.api-test
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Tests the complete API set."}
  (:require [vl-db.configure :as c]
            [vl-db.core :as db]
            [clojure.test :refer :all]))

(def conf (c/config  {:usr (System/getenv "CAL_USR")
                      :pwd (System/getenv "CAL_PWD")}))

(deftest a-server-up-i
  (testing "the server is reachable"
    (is (db/up? conf))))

(def N 2)
(def v (db/get-uuids N conf))

(deftest b-uuids-i
  (testing "returns a vector of length N"
    (is (= N (count v)))
    (is (= 1 (count (db/get-uuids conf))))))

;; use first v as database name
;; assoc db-name to conf
(def conf (assoc conf :name (str "testdb")))
(def docid "testdoc")

(deftest c-uuids-ii
  (testing "is string"
     (is (string? (:name conf)))))

(deftest d-gen-database-i
  (testing "a database can be generated only once"
    (is (true? (:ok (db/put-db conf))))
    (is (= (db/put-db conf)
           {:error "clj-http: status 412"}))))

(deftest e-put-doc-i
  (testing "a document can be generated and updated"
    (is (true? (:ok (db/put-doc {:_id docid :A {:a 1}} conf))))
    (is (true? (:ok (db/put-doc {:_id docid :A {:a 2}} conf))))
    (is (true? (:ok (db/put-doc {:_id docid :A {:a 3}} conf))))
    (is (true? (:ok (db/put-doc {:_id docid :A {:a 4}} conf))))))

(deftest del-doc-i
  (testing "a document can be deleted only once"
    (is (true? (:ok (db/del-doc docid conf))))
    (is (= (db/del-doc docid conf)
           {:error "clj-http: status 404"}))))

(deftest del-database-i
  (testing "a database can be deleted only once"
   (is (= (db/del-db conf)
       {:ok true}))
   (is (= (db/del-db conf)
       {:error "clj-http: status 404"}))))


