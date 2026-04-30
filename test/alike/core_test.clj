(ns alike.core-test
  (:require
   [clojure.test :refer [are is deftest testing]]
   [alike.core :as alike]))

(deftest test-equality
  (are [a b] (true? (alike/match a b))
    1 1
    "a" "a"
    true true
    false false
    nil nil
    :foo :foo
    'aaa 'aaa
    Integer Integer
    + +
    \a \a
    (/ 3 2) (/ 3 2)))

;; 1 1.0
    ;; 1.0 1

(deftest test-functions
  (are [a b] (true? (alike/match a b))
    int? 1
    float? 1.0
    nil? nil
    string? "test"
    map? {}
    vector? []
    list? ()
    set? #{}
    empty? #{}))

;; ifn? +

(deftest test-instance
  (are [a b] (true? (alike/match a b))
    Integer (int 1)
    Long 2
    Double 2.0
    String "test"
    )




  )

;; sets
;; arrays
