(ns alike.core
  (:require
   [clojure.string :as str]))


;; add hint/reason message
;; add matching options
;; toString method -> function
;; better repr for functions
;; add test cases
;; add test report (is (alike )


(def MISSING '<missing>)

(defn class-name [obj]
  (some-> obj
          (class)
          (.getCanonicalName)))


(defprotocol IMismatch
  (add-level [this level]))


(deftype Mismatch [-expected
                   -actual
                   -path]
  IMismatch
  (add-level [_this level]
    (new Mismatch -expected -actual (cons level -path)))

  Object
  (toString [_this]
    (with-out-str
      (println "Mismatch")
      (println)
      (printf "  path     [%s]%n" (str/join " " -path))
      (println)
      (printf "  expected %s%n" -expected)
      (when-let [cls (class-name -expected)]
        (printf "    type   %s%n" cls))
      (println)
      (printf "  actual   %s%n" -actual)
      (when-let [cls (class-name -actual)]
        (printf "    type   %s%n" cls)))))


(defn mismatch
  ([expected actual]
   (new Mismatch expected actual nil))

  ([expected actual level]
   (new Mismatch expected actual [level])))


(defn mismatch? [x]
  (instance? Mismatch x))


(defmulti -match
  (fn [a b]
    [(type a) (type b)]))


(defn match [expected actual]
  (let [result (-match expected actual)]

    (cond

      (mismatch? result)
      result

      (not result)
      (mismatch expected actual)

      :else
      result)))


(defmethod -match [java.lang.Object java.lang.Object]
  [a b]
  (or (= a b)
      (mismatch a b)))


(defmethod -match [java.lang.Object nil]
  [a b]
  (mismatch a b))


(defmethod -match [nil java.lang.Object]
  [a b]
  (mismatch a b))


(defmethod -match [nil nil]
  [a b]
  true)


(defmethod -match [clojure.lang.Keyword clojure.lang.Keyword]
  [k1 k2]
  (or (identical? k1 k2)
      (mismatch k1 k2)))


(defmethod -match [java.lang.Class java.lang.Object]
  [cls obj]
  (or (instance? cls obj)
      (mismatch cls obj)))


(defmethod -match [clojure.lang.IFn java.lang.Object]
  [ifn obj]
  (or (ifn obj)
      (mismatch ifn obj)))


(defmethod -match [clojure.lang.IFn clojure.lang.IFn]
  [ifn1 ifn2]
  (or (= ifn1 ifn2)
      (mismatch ifn1 ifn2)))


(defmethod -match [java.util.List java.lang.Object]
  [l obj]
  (mismatch l obj))


(defmethod -match [java.util.Set java.lang.Object]
  [set obj]
  (or (contains? set obj)
      (mismatch set obj)))


(defmethod -match [java.util.Map java.util.Map]
  [m1 m2]
  (reduce-kv
   (fn [acc k v1]
     (if-let [[_ v2] (find m2 k)]
       (let [result (match v1 v2)]
         (if (mismatch? result)
           (reduced (add-level result k))
           acc))
       (reduced (mismatch k MISSING))))
   true
   m1))


(defmethod -match [java.util.List java.util.List]
  [l1 l2]
  (let [iter1 (clojure.lang.RT/iter l1)
        iter2 (clojure.lang.RT/iter l2)]

    (loop [i 0]

      (let [next1? (.hasNext iter1)
            next2? (.hasNext iter2)]

        (case [next1? next2?]

          [false false]
          true

          [true true]
          (let [v1 (.next iter1)
                v2 (.next iter2)
                result (match v1 v2)]
            (if (mismatch? result)
              (add-level result i)
              (recur (inc i))))

          [true false]
          (let [v1 (.next iter1)]
            (mismatch v1 MISSING i))

          [false true]
          (let [v2 (.next iter2)]
            (mismatch MISSING v2 i)))))))


(prefer-method -match
               [java.util.Set java.lang.Object]
               [clojure.lang.IFn clojure.lang.IFn])

(prefer-method -match
               [java.util.Set java.lang.Object]
               [clojure.lang.IFn java.lang.Object])

(prefer-method -match
               [java.util.Map java.util.Map]
               [clojure.lang.IFn java.lang.Object])

(prefer-method -match
               [java.util.List java.util.List]
               [clojure.lang.IFn java.lang.Object])

(prefer-method -match
               [java.util.List java.lang.Object]
               [clojure.lang.IFn java.lang.Object])

(prefer-method -match
               [java.util.List java.util.List]
               [clojure.lang.IFn clojure.lang.IFn])

(prefer-method -match
               [java.util.Map java.util.Map]
               [clojure.lang.IFn clojure.lang.IFn])


(defn -main [& _]
  (println (-match [1 int? 3] [1 :foo 3]))
  #_
  (println (-match [1 2 3 {:bar {:aaa [1 int? 3]}}] [1 2 3 {:foo 1 :bar {:aaa [1 :foo 3]}}]))
  )
