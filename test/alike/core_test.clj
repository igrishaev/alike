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
    (/ 3 2) (/ 3 2))

  (are [a b] (alike/mismatch? (alike/match a b))
    1 1.0
    1.0 1
    1 2
    (atom 42) nil
    nil (atom 42)
    (atom 42) +
    "b" "a"
    false true
    false true
    nil 1
    1 nil
    :foo :foo/foo
    'aaa 'bb/aaa
    Integer Long
    + /
    \a \b
    (/ 3 2) 33))


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
    empty? #{})

  (are [a b] (alike/mismatch? (alike/match a b))
    1 int?
    nil nil?
    "test" string?
    {} map?
    [] vector?
    () list?
    #{} set?
    #{} empty?
    ifn? +
    + /))


(deftest test-instance
  (are [a b] (true? (alike/match a b))
    Integer (int 1)
    Long 2
    Double 2.0
    String "test"
    clojure.lang.Counted [1 2 3]
    clojure.lang.IObj {:foo 1})

  (are [a b] (alike/mismatch? (alike/match a b))
    (int 1) Integer
    2 Long
    2.0 Double
    "test" String
    [1 2 3] clojure.lang.Counted
    {:foo 1} clojure.lang.IObj))


(defn java-list [coll]
  (let [list (new java.util.ArrayList)]
    (doseq [item coll]
      (.add list item))
    list))


(defn java-map [mapping]
  (let [jm (new java.util.HashMap)]
    (doseq [[k v] mapping]
      (.put jm k v))
    jm))


(defn java-set [coll]
  (let [hs (new java.util.HashSet)]
    (doseq [item coll]
      (.add hs item))
    hs))


(deftest test-lists
  (are [a b] (true? (alike/match a b))
    [] (java-list [])
    (java-list []) (map identity [])
    [] ()
    () []
    [1 2 3] [1 2 3]
    (list 1) (list 1)
    (cons 1 [2 3 4]) (cons 1 [2 3 4])
    (seq [1 2 3]) (seq [1 2 3])
    [1] '(1)
    '(1) [1]
    [[:foo 1]] (seq {:foo 1})
    (seq {:foo 1}) [[:foo 1]])

  (are [a b] (alike/mismatch? (alike/match a b))
    {} []
    [] {}
    [1] 1
    1 (seq [1 2])
    [1] {:foo 1}
    [1] +
    :foo [1]
    [1] :foo
    [1] Integer
    [1] "a"
    [1] [1 2]
    [1 2] [1]))

(defrecord MyRecord [a b c])

(deftest test-maps
  (are [a b] (true? (alike/match a b))
    (java-map []) {}
    (java-map {:foo 1}) {:foo 1 :aaa 2}
    {:foo 1} (java-map {:foo 1 :aaa 2})
    {} {}
    {} {:a 1}
    {:a 1} {:a 1}
    {:a 1} {:a 1 :b 2}
    {:a [{:b 2}]} {:a [{:b 2}]}
    {:a 1} (new MyRecord 1 2 3)
    {:a 1 :b 2} (new MyRecord 1 2 3)
    (new MyRecord 1 2 3) {:a 1 :b 2 :c 3}
    (new MyRecord 1 2 3) {:a 1 :b 2 :c 3 :d 4})

  (are [a b] (alike/mismatch? (alike/match a b))
    {:foo 1 :aaa nil} (java-map {:foo 1 :aaa 2})
    {:a 1} {:a 2}
    {:a 1 :b nil} {:a 1 :b 2}
    {:foo 1} :foo
    :foo {:foo 1}
    (seq {:a 1}) {:a 1}
    {:a 1} (seq {:a 1})
    (new MyRecord 1 2 3) {:a 1 :b 2}))

(deftest test-sets
  (are [a b] (true? (alike/match a b))
    #{1 2 3} #{1 2 3}
    #{} #{}
    #{1 2 nil} nil
    #{1} (java-set [1])
    (java-set [1]) #{1}
    #{1 2 3} 2)

  (are [a b] (alike/mismatch? (alike/match a b))
    (java-set [1 2 3]) [1 2 3]
    (java-set [1 2 3]) #{4}
    #{1 2 3} #{1 9 3}
    #{1 2 3} :foo))

(deftest test-arrays
  (are [a b] (true? (alike/match a b))
    [1 2 3] (object-array [1 2 3])
    [1 2 3] (int-array [1 2 3])
    [1 2 3] (long-array [1 2 3])))

(deftest test-regex
  (are [a b] (true? (alike/match a b))
    #"\d+" "abc234asdf")

  (are [a b] (alike/mismatch? (alike/match a b))
    #"\d+" "abcasdf"))


(deftest test-count
  (are [a b] (true? (alike/match a b))
    "bar" "foo bar baz"
    (alike/count 3) "abc"
    (alike/count 3) (list 1 2 3)
    (alike/count 0) ()
    (alike/count 0) nil)

  (are [a b] (alike/mismatch? (alike/match a b))
    "bar" "foo BAR baz"
    (alike/count 3) "abcd"
    (alike/count 3) (list 1 2 3 4)
    (alike/count 1) ()
    (alike/count 3) nil))


(deftest test-nested-list
  (let [exp
        {:a {:b {:c [1 2 {:d [2 3 4]} 4 5]}}}

        act
        {:a {:b {:c [1 2 {:d [2 9 4]} 4 5]}}}

        result
        (alike/match exp act)

        path
        [:a :b :c 2 :d 1]]

    (is (= 3 (get-in exp path)))

    (is (= {:-expected 3
            :-actual 9
            :-tag :object-object
            :-path path}
           (into {} result)))

    (is (= "The expected value =/= actual value
  case :object-object
  path [:a :b :c 2 :d 1]
  expected: 3
  actual: 9
"
           (alike/-repr result)))))


(deftest test-repr
  (are [a b] (= (alike/-repr a) b)
    + "clojure.core/+"
    1 "1"
    nil "nil"
    alike/MISSING "<missing>"
    Integer "java.lang.Integer"
    (repeat 100 100500) "(100500 100500 100500 100500 100500 100500 100500 100500 100500 100500..."))


(deftest test-explain

  (are [exp act msg] (= msg (alike/-explain (alike/match exp act)))

    1 2
    "The expected value =/= actual value"

    {:foo 1} {:foo 2}
    "The expected value =/= actual value"

    {:foo 1 :bar 2} {:foo 1}
    "The expected map has the key :bar which is missing in the actual map"

    nil (new Object)
    "Expected nil but got an instance of java.lang.Object"

    java.util.UUID :dunno
    "Expected is an instance of java.util.UUID but got clojure.lang.Keyword"

    (alike/count 3) "abcd"
    "The actual string length doesn't equal to the expected count"

    (alike/count 3) [1 2 3 4]
    "The actual number of items doens't equal to the expected count"

    (alike/count 2) nil
    "The expected count is not zero, but got nil"

    ))
