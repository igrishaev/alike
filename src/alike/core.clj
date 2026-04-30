(ns alike.core
  (:gen-class))


(deftype Mismatch [^String -message]
  Object
  (toString [this]
    -message))

(defn mismatch [template & args]
  (new Mismatch (apply format template args)))

(defn mismatch? [x]
  (instance? Mismatch x))

(defmulti -match
  (fn [a b]
    [(type a) (type b)]))

(defmethod -match [Object Object]
  [a b]
  (or (= a b)
      (mismatch "(expected %s) =/= (actual %s)" a b)))

(defmethod -match [clojure.lang.Keyword clojure.lang.Keyword]
  [k1 k2]
  (or (identical? k1 k2)
      (mismatch "(expected %s) =/= (actual %s)" k1 k2)))

(defmethod -match [Class Object]
  [cls obj]
  (or (instance? cls obj)
      (mismatch "(expected %s) not instance? (actual %s)" cls obj)))


(defmethod -match [clojure.lang.IFn Object]
  [ifn obj]
  (or (ifn obj)
      (mismatch "(expected %s) is false for (actual %s)" (pr-str ifn) obj)))


(defmethod -match [clojure.lang.IFn clojure.lang.IFn]
  [ifn1 ifn2]
  (or (= ifn1 ifn2)
      (mismatch "(expected %s) =/= (actual %s)" ifn1 ifn2)))

(defmethod -match [java.util.List Object]
  [l obj]
  (mismatch "(expected %s) =/= (actual %s)" l obj))

(defmethod -match [java.util.Set Object]
  [set obj]
  (or (contains? set obj)
      (mismatch "(expected: %s contains %s) (actual: doesn't)"
                set obj)))

(defmethod -match [java.util.Map java.util.Map]
  [m1 m2]
  (reduce-kv
   (fn [acc k v1]
     (if-let [[_ v2] (find m2 k)]
       (let [result (-match v1 v2)]

         (cond

           (mismatch? result)
           (mismatch "%s -> %s"
                     k result)

           (not result)
           (mismatch "%s -> (expected %s) =/= (actual %s)"
                     k v1 v2)

           :else
           true))

       (reduced (mismatch "%s -> (expected: has key) (actual: no key)"
                          k))))
   true
   m1))

(defmethod -match [java.util.List java.util.List]
  [l1 l2]
  (let [iter1 (clojure.lang.RT/iter l1)
        iter2 (clojure.lang.RT/iter l2)]

    (loop [i 0 path []]

      (let [next1? (.hasNext iter1)
            next2? (.hasNext iter2)]

        (case [next1? next2?]

          [false false]
          true

          [true true]
          (let [v1 (.next iter1)
                v2 (.next iter2)
                result (-match v1 v2)]

            (cond

              (mismatch? result)
              (mismatch "%s -> %s"
                        i result)

              (not result)
              (mismatch "%s -> (expected %s) =/= (actual %s)"
                        i v1 v2)

              :else
              (recur (inc i) (conj path i))))

          [true false]
          (mismatch "%s -> (expected %s) (actual %s)"
                    i (.next iter1) "<missing>")

          [false true]
          (mismatch "%s -> (expected %s) (actual %s)"
                    i "<missing>" (.next iter2))


          )
        ))


    )

  )

(prefer-method -match


               [java.util.Set java.lang.Object]
               [clojure.lang.IFn clojure.lang.IFn]

               )

(prefer-method -match

               [java.util.Set java.lang.Object]
               [clojure.lang.IFn java.lang.Object]


               )


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
  (println (-match #{:foo :bar :baz} :bar2))
  #_
  (println (-match [1 2 3 {:bar {:aaa [1 int? 3]}}] [1 2 3 {:foo 1 :bar {:aaa [1 :foo 3]}}]))
  )
