(ns alike.core
  (:refer-clojure :exclude [count])
  (:require
   [clojure.data :as data]
   [clojure.string :as str]
   [clojure.test :as test]))

;; docstrings
;; readme & toc
;; release
;; explain tests

(alias 'cc 'clojure.core)


(deftype Missing [])

(defonce MISSING (new Missing))


(defprotocol IMismatch
  (-push [this level]))

(defprotocol IRepr
  (-repr [this]))


(def REPR_LIMIT 70)

(extend-protocol IRepr

  nil
  (-repr [this]
    "nil")

  Missing
  (-repr [this]
    "<missing>")

  java.lang.Object
  (-repr [this]
    (let [-result (pr-str this)]
      (if (> (cc/count -result) REPR_LIMIT)
        (-> -result (subs 0 REPR_LIMIT) (str "..."))
        -result)))

  java.lang.Class
  (-repr [this]
    (.getCanonicalName this))

  clojure.lang.Fn
  (-repr [this]
    (-> this
        .toString
        (str/replace "_PLUS_" "+")
        (str/replace #"@\w+" "")
        (str/replace "_QMARK_" "?")
        (str/replace "_BANG_" "!")
        (str/replace "_GT_" ">")
        (str/replace "_LT_" "<")
        (str/replace "$" "/"))))


(defmulti -explain :-tag)

(defmethod -explain :default [_]
  "The expected value doesn't match the actual value (no details provided)")


(defrecord Mismatch [-expected
                     -actual
                     -tag
                     -path]
  IMismatch
  (-push [_this level]
    (new Mismatch -expected -actual -tag (cons level -path)))

  IRepr
  (-repr [this]
    (with-out-str
      (println (-explain this))
      (printf "  case %s%n" -tag)
      (printf "  path [%s]%n" (str/join " " -path))
      (printf "  expected: %s%n" (-repr -expected))
      (printf "  actual: %s%n" (-repr -actual)))))


(defn mismatch
  ([expected actual]
   (mismatch expected actual :default))

  ([expected actual tag]
   (new Mismatch expected actual tag nil))

  ([expected actual tag level]
   (new Mismatch expected actual tag [level])))


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
      (mismatch expected actual :default)

      :else
      true)))


(let [-tag :object-object]

  (defmethod -match [java.lang.Object java.lang.Object]
    [a b]
    (or (= a b)
        (mismatch a b -tag)))

  (defmethod -explain -tag [_]
    "The expected value =/= actual value"))


(let [-tag :object-nil]

  (defmethod -match [java.lang.Object nil]
    [a b]
    (mismatch a b -tag))

  (defmethod -explain -tag [{:keys [-expected]}]
    (format "Expected is an instance of %s but actual is nil"
            (-> -expected class -repr))))


(let [-tag :nil-object]

  (defmethod -match [nil java.lang.Object]
    [a b]
    (mismatch a b -tag))

  (defmethod -explain -tag [{:keys [-actual]}]
    (format "Expected nil but got an instance of %s"
            (-> -actual class -repr))))


(defmethod -match [nil nil]
  [a b]
  true)


(let [-tag :class-object]

  (defmethod -match [java.lang.Class java.lang.Object]
    [cls obj]
    (or (instance? cls obj)
        (mismatch cls obj -tag)))

  (defmethod -explain -tag [{:keys [^Class -expected
                                    ^Object -actual]}]
    (format "Expected is an instance of %s but got %s"
            (-repr -expected)
            (-> -actual class -repr))))


(let [-tag :class-class]

  (defmethod -match [java.lang.Class java.lang.Class]
    [cls1 clj2]
    (or (= cls1 clj2)
        (mismatch cls1 clj2 -tag)))

  (defmethod -explain -tag [{:keys [^Class -expected
                                    ^Class -actual]}]
    (format "Expected class %s =/= actual class %s"
            (-repr -expected)
            (-repr -actual))))


(let [-tag :func-nil]

  (defmethod -match [clojure.lang.Fn nil]
    [func the-nil]
    (or (func the-nil)
        (mismatch func the-nil :func-nil)))

  (defmethod -explain -tag [{:keys [-expected]}]
    (format "The expected function %s returned a false result for the actual nil value"
            -expected)))

(let [-tag :func-func]

  (defmethod -match [clojure.lang.Fn clojure.lang.Fn]
    [func1 func2]
    (or (= func1 func2)
        (mismatch func1 func2 -tag)))

  (defmethod -explain -tag [{:keys [-expected
                                    -actual]}]
    (format "The expected function %s =/= the actual function %s"
            (-repr -expected)
            (-repr -actual))))


(let [-tag :func-object]

  (defmethod -match [clojure.lang.Fn java.lang.Object]
    [func obj]
    (or (func obj)
        (mismatch func obj -tag)))

  (defmethod -explain -tag [{:keys [-expected]}]
    (format "The expected function %s returned a false result for the actual value"
            (-repr -expected))))


(let [-tag :set-object]

  (defmethod -match [java.util.Set java.lang.Object]
    [set obj]
    (or (contains? set obj)
        (mismatch set obj -tag)))

  (defmethod -explain -tag [_]
    "The expected set doesn't contain the actual object"))


(defmethod -match [java.util.Set java.util.Set]
  [set1 set2]
  (let [[set1-only set2-only _both]
        (data/diff set1 set2)]

    (cond

      (and set1-only set2-only)
      (mismatch set1-only set2-only :set-set-both)

      set1-only
      (mismatch set1-only MISSING :set-set-left)

      set2-only
      (mismatch MISSING set2-only :set-set-right)

      :else
      true)))

(defmethod -explain :set-set-both [_]
  "The expected and the actual sets have different values")

(defmethod -explain :set-set-left [_]
  "The expected set has values missing in the actual set")

(defmethod -explain :set-set-right [_]
  "The expected set misses values presenting in the actual set")


(let [-tag :set-nil]

  (defmethod -match [java.util.Set nil]
    [the-set _]
    (or (contains? the-set nil)
        (mismatch the-set nil -tag)))

  (defmethod -explain -tag [_]
    "The expected set doesn't contain a nil value "))

(let [-tag :regex-string]

  (defmethod -match [java.util.regex.Pattern java.lang.String]
    [re string]
    (or (re-find re string)
        (mismatch re string -tag)))

  (defmethod -explain -tag [_]
    "The expected regex doesn't match the actual string"))


(let [-tag :string-string]

  (defmethod -match [java.lang.String java.lang.String]
    [string1 string2]
    (or (str/includes? string2 string1)
        (mismatch string1 string2 -tag)))

  (defmethod -explain -tag [_]
    "The actual string doesn't include the expected string"))


(let [-tag :map-map]

  (defmethod -match [java.util.Map java.util.Map]
    [m1 m2]
    (reduce-kv
     (fn [acc k v1]
       (if-let [[_ v2] (find m2 k)]
         (let [result (match v1 v2)]
           (if (mismatch? result)
             (reduced (-push result k))
             acc))
         (reduced (mismatch k MISSING -tag))))
     true
     m1))

  (defmethod -explain -tag [{:keys [-expected]}]
    (format "The expected map has a key '%s' which is missing in the actual map"
            (-repr -expected))))


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
              (-push result i)
              (recur (inc i))))

          [true false]
          (let [v1 (.next iter1)]
            (mismatch v1 MISSING :list-right-over i))

          [false true]
          (let [v2 (.next iter2)]
            (mismatch MISSING v2 :list-left-over i)))))))

(defmethod -explain :list-right-over [_]
  "The expected list has more items than the actual one does")

(defmethod -explain :list-left-over [_]
  "The expected list has less items than the actual one does")


;;
;; Arrays
;;

(def ^Class ARRAY_BOOL
  (Class/forName "[Z"))

(def ^Class ARRAY_BYTE
  (Class/forName "[B"))

(def ^Class ARRAY_CHAR
  (Class/forName "[C"))

(def ^Class ARRAY_DOUBLE
  (Class/forName "[D"))

(def ^Class ARRAY_FLOAT
  (Class/forName "[F"))

(def ^Class ARRAY_INT
  (Class/forName "[I"))

(def ^Class ARRAY_SHORT
  (Class/forName "[S"))

(def ^Class ARRAY_LONG
  (Class/forName "[J"))

(def ^Class ARRAY_OBJ
  (Class/forName "[Ljava.lang.Object;"))


(defmethod -match [java.util.List ARRAY_OBJ]
  [list array]
  (match list (vec array)))


(defmethod -match [java.util.List ARRAY_INT]
  [list array]
  (match list (vec array)))


(defmethod -match [java.util.List ARRAY_LONG]
  [list array]
  (match list (vec array)))

;;
;; smart objects
;;


(defrecord Count [-n]
  IRepr
  (-repr [_]
    (format "<count=%s>" -n)))

(defn count [n]
  (new Count n))

(let [-tag :count-string]

  (defmethod -match [Count java.lang.String]
    [c ^String string]
    (or (= (:-n c) (.length string))
        (mismatch c string -tag)))

  (defmethod -explain -tag [_]
    "The actual string length doesn't equal the expected count"))


;;
;; clojure.test extension
;;

(defmethod test/assert-expr 'alike
  [msg [_ expected actual :as form]]
  `(let [result# (match ~expected ~actual)]
     (if (mismatch? result#)
       (let [{-expected# :-expected
              -actual# :-actual}
             result#

             representation#
             (-repr result#)

             message#
             (str ~msg
                  (when ~msg \newline)
                  representation#)]

         (test/do-report {:type :fail
                          :message message#
                          :expected -expected#
                          :actual -actual#}))
       (test/do-report {:type :pass
                        :message ~msg
                        :expected ~expected
                        :actual ~actual}))))
