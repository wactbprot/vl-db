(ns vl-db.core-test
  (:require [vl-db.core :as db]
            [clojure.test :refer :all]))

(deftest param-opt-i
  (testing "right place"
    (is (= (db/param-opt {:key "foo"})
           {:opt {:query-params {:key "foo"}}})
        "values")))


