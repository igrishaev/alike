(ns alike.test
  (:require
   [alike.core :as alike]
   [clojure.test :as test]))


(defmethod test/assert-expr 'alike
  [msg [_ expected actual :as form]]
  `(let [result# (alike/match ~expected ~actual)]
     (if (alike/mismatch? result#)
       (let [{-expected# :-expected
              -actual# :-actual}
             result#

             representation#
             (alike/-repr result#)

             message#
             (str ~msg
                  (when ~msg \newline)
                  representation#)]

         (test/do-report {:type :fail
                          :message message#
                          :expected -expected#
                          :actual -actual#}))
       (test/do-report {:type :pass :message ~msg
                        :expected '~form :actual :foobar}))))


#_
(defmethod test/report )
