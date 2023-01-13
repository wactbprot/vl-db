(ns libcdb.configure-test
  (:require [libcdb.configure :as c]
            [clojure.test :refer :all]))

(deftest config-i
  (testing "query params kept"
    (is (=  (-> (c/config {:opt {:query-params {:bar "foo"}}})
                :opt
                :query-params
                :bar)
            "foo")
        "values")))
