(ns my.app-test
  (:require [clojure.test :refer [deftest testing is]]))

(deftest basic-test
  (testing "true is true"
    (is (= true true))))
