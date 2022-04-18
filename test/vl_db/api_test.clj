(ns vl-db.api-test
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Tests the complete API set."}
  (:require [vl-db.configure :as c]
            [vl-db.core :as db]
            [clojure.test :refer :all]))
;; ............................................................
;; to keep all test in the correct order
;; use `test-ns-hook` (see last function in ns) 
;; ............................................................
;; the test-ns-hook defines the order of test
(def conf (c/config  {:name  "testdb"
                      :usr (System/getenv "CAL_USR")
                      :pwd (System/getenv "CAL_PWD")}))
(def docid "testdoc")

(deftest server-up-i
  (testing "the server is reachable"
    (is (db/up? conf))))

(deftest server-up-ii
  (testing "up? don't crash"
    (is (false? (db/up? (assoc conf :host "not-a-host"))))))

(deftest uuids-i
  (testing "returns a vector of length N"
    (let [n 2]
      (is (= n (count (db/get-uuids n conf))))
      (is (= 1 (count (db/get-uuids conf)))))))

(deftest uuids-ii
  (testing "is string"
     (is (string? (:name conf)))))

(deftest gen-database-i
  (testing "a database can be generated only once"
    (is (true? (:ok (db/put-db conf))))
    (is (= (db/put-db conf)
           {:error "clj-http: status 412"}))))

(deftest put-doc-i
  (testing "a document can be generated and updated"
    (is (true? (:ok (db/put-doc {:_id docid :A  1} conf))))
    (is (= 1 (:A (db/get-doc docid conf))))
    (is (true? (:ok (db/put-doc {:_id docid :A  2} conf))))
    (is (= 2 (:A (db/get-doc docid conf))))))

(deftest get-db-i
  (testing "gets info about the test database"
    (is (pos? (:doc_count (db/get-db conf))))))



(deftest put-design-doc-i
  (testing "a design document can be generated"
    (let [ddoc {:_id "_design/test"
                :views {:testview
                        {:map "function (doc) {emit(doc._id, 1);}"}}
                :language "javascript"}]
      (is (true? (:ok (db/put-doc ddoc conf)))))))

(deftest get-view-i
  (testing "a view can be retrieved"
    (is (pos? (count (db/get-view (assoc conf :design "test" :view "testview")))))))

(deftest put-attachment-i
  (testing "a attachment can be uploaded"
    (is (true? (:ok (db/put-attachment-from-filename docid "README.md" conf))))))

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

(comment)
(defn test-ns-hook []
  (server-up-i)
  (uuids-i)
  (uuids-ii)
  (gen-database-i)
  (put-doc-i)
  (get-db-i)
  (put-design-doc-i)
  (put-attachment-i)
  (get-view-i)
  (del-doc-i)
  (del-database-i))
