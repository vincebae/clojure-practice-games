(ns my.app-test
  (:require [clojure.test :refer [deftest testing is]]
            [my.app :refer [greet]]))

(deftest greet-test
  (testing "greet with no args"
    (is (= (greet []) "Hello, World!"))
    (is (= (greet nil) "Hello, World!")))
  (testing "greet with args"
    (is (= (greet ["a" "b" "c"]) "Hello, a, b, c!"))
    (is (= (greet [1 2 3]) "Hello, 1, 2, 3!"))))
