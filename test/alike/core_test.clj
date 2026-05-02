(ns alike.core-test
  (:require
   [alike.test]
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
    ;; :foo {:foo 1}
    {:a 1} (new MyRecord 1 2 3)
    {:a 1 :b 2} (new MyRecord 1 2 3)
    (new MyRecord 1 2 3) {:a 1 :b 2 :c 3}
    (new MyRecord 1 2 3) {:a 1 :b 2 :c 3 :d 4})

  (are [a b] (alike/mismatch? (alike/match a b))
    {:foo 1 :aaa nil} (java-map {:foo 1 :aaa 2})
    {:a 1} {:a 2}
    {:a 1 :b nil} {:a 1 :b 2}
    {:foo 1} :foo
    (seq {:a 1}) {:a 1}
    {:a 1} (seq {:a 1})
    (new MyRecord 1 2 3) {:a 1 :b 2}))

(deftest test-sets
  (are [a b] (true? (alike/match a b))
    #{1 2 3} #{1 2 3}
    #{} #{}
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
    [1 2 3] (long-array [1 2 3])
    ))


(defn get-data []
  {:foo {:bar [1 2 {:aaa nil} 4 5 ]}})


(def STRESS-DATA
  {:nil                   nil
   :true                  true
   :false                 false
   :false-boxed (Boolean. false)

   :char      \ಬ
   :str-short "ಬಾ ಇಲ್ಲಿ ಸಂಭವಿಸ"
   :str-long  (reduce str (range 1024))
   :kw        :keyword
   :kw-ns     ::keyword
   :sym       'foo
   :sym-ns    'foo/bar
   :kw-long   (keyword (reduce str "_" (range 128)) (reduce str "_" (range 128)))
   :sym-long  (symbol  (reduce str "_" (range 128)) (reduce str "_" (range 128)))

   :byte      (byte   16)
   :short     (short  42)
   :integer   (int    3)
   :long      (long   3)
   :float     (float  3.1415926535897932384626433832795)
   :double    (double 3.1415926535897932384626433832795)
   :bigdec    (bigdec 3.1415926535897932384626433832795)
   :bigint    (bigint  31415926535897932384626433832795)
   :ratio     22/7

   :list      (list 1 2 3 4 5 (list 6 7 8 (list 9 10 (list) ())))
   :vector    [1 2 3 4 5 [6 7 8 [9 10 [[]]]]]
   :subvec    (subvec [1 2 3 4 5 6 7 8] 2 8)
   :map       {:a 1 :b 2 :c 3 :d {:e 4 :f {:g 5 :h 6 :i 7 :j {{} {}}}}}
   :map-entry (clojure.lang.MapEntry/create "key" "val")
   :set       #{1 2 3 4 5 #{6 7 8 #{9 10 #{#{}}}}}
   :meta      (with-meta {:a :A} {:metakey :metaval})
   :nested    [#{{1 [:a :b] 2 [:c :d] 3 [:e :f]} [#{{[] ()}}] #{:a :b}}
               #{{1 [:a :b] 2 [:c :d] 3 [:e :f]} [#{{[] ()}}] #{:a :b}}
               [1 [1 2 [1 2 3 [1 2 3 4 [1 2 3 4 5 "ಬಾ ಇಲ್ಲಿ ಸಂಭವಿಸ"] {} #{} [] ()]]]]]

   :regex          #"^(https?:)?//(www\?|\?)?"
   :sorted-set     (sorted-set 1 2 3 4 5)
   :sorted-map     (sorted-map :b 2 :a 1 :d 4 :c 3)
   :lazy-seq-empty (map identity ())
   :lazy-seq       (repeatedly 64 #(do nil))
   :queue          (into clojure.lang.PersistentQueue/EMPTY [:a :b :c :d :e :f :g])
   :queue-empty          clojure.lang.PersistentQueue/EMPTY

   :uuid       (java.util.UUID. 7232453380187312026 -7067939076204274491)
   :uri        (java.net.URI. "https://clojure.org")
   :bytes      (byte-array   [(byte 1) (byte 2) (byte 3)])
   :objects    (object-array [1 "two" {:data "data"}])

   :util-date (java.util.Date. 1577884455500)
   :sql-date  (java.sql.Date.  1577884455500)
   :instant   (java.time.Instant/parse "2020-01-01T13:14:15.50Z")
   :duration  (java.time.Duration/ofSeconds 100 100)
   :period    (java.time.Period/of 1 1 1)

   :throwable (Throwable. "Msg")
   :exception (Exception. "Msg")
   :ex-info   (ex-info    "Msg" {:data "data"})

   :many-longs    (vec (repeatedly 512         #(rand-nth (range 10))))
   :many-doubles  (vec (repeatedly 512 #(double (rand-nth (range 10)))))
   :many-strings  (vec (repeatedly 512         #(rand-nth ["foo" "bar" "baz" "qux"])))
   :many-keywords (vec (repeatedly 512
                                   #(keyword
                                     (rand-nth ["foo" "bar" "baz" "qux" nil])
                                     (rand-nth ["foo" "bar" "baz" "qux"    ]))))})


(deftest test-foo

  (is (alike (assoc STRESS-DATA :lazy-seq 1)
             STRESS-DATA
             ))

  #_
  (is (alike {:foo {:bar [1 2 {:aaa 42} 4 5 ]}}
             (get-data)

             )
      "foo bar baz"
      )
  )



;; arrays
;; check representation

;; add hint/reason message
;; add matching options
;; toString method -> function
;; better repr for functions
;; add test report (is (alike )
;; any-of, none-of, count, other helpers?
;; starts-with, ends-with, contains? regex?
;; regex string

;; mismatch: metter fn representation
;; mismatch: missing repr
;; set contains nil
;; move test.ns to the core
