# Alike

"Alike" is a library to match values in tests. For example:

~~~clojure
(is (alike {:some [:data [{:foo 1}]]}
           (get-result item-id)))
~~~

It has no dependencies, pretty simple and extendable.

## Table of Contents

<!-- toc -->

- [Installation](#installation)
- [Rationale](#rationale)
- [Basic Usage](#basic-usage)
- [Output](#output)
- [Matching Cases](#matching-cases)
- [Special objects](#special-objects)
- [Extending](#extending)
- [Other](#other)

<!-- tocstop -->

## Installation

Lein:

~~~clojure
[com.github.igrishaev/alike "0.1.0"]
~~~

Deps.edn

~~~clojure
com.github.igrishaev/alike {:mvn/version "0.1.0"}
~~~

## Rationale

"Alike" is my attempt to make a matching library. There are many of them these
days, namely:

- [matcher-combinators](https://github.com/nubank/matcher-combinators)
- [matchete](https://github.com/xapix-io/matchete)
- [expectations](https://github.com/clojure-expectations/clojure-test)
- [matcho](https://github.com/HealthSamurai/matcho)

and more. So we've got another one. Why?

The project tries to reach the following three points:

- be as simple as possible. There is just one core principle that manages all
  cases;
- it should be extendable so anyone can define their own matching rules;
- the library should clearly explain what didn't match what, and where exactly,
  and why. The output must be narrow and precise even if data structures are
  huge.

Below, we'll check if these requirements have been met.

## Basic Usage

Import the library in a certain test namespace:

~~~clojure
(ns org.project.some-ns-test
  (:require
    [alike.core :as alike]
    [clojure.test :refer [is deftest]]))
~~~

Replace all `(is (= ...))` expressions with `(is (alike ...))` so test look like
this:

~~~clojure
(deftest test-some-case
  (let [...]
    (is (alike {:expected [:data]}
               (get-actual-data ...)))))
~~~

## Output

Let's consider the following case. You have a function you'd like to test:

~~~clojure
(defn func-to-check [item-id]
  {:response
   {:data
    [{:user-id (random-uuid)
      :title "Item 1"
      :tags ["foo" "bar"]}
     {:user-id (random-uuid)
      :title "Item 2"
      :tags ["test" "hello"]}
     {:user-id (random-uuid)
      :title "Item 3"
      :tags ["some" "tag"]}]}})
~~~

As this function returns random data (UUID, for example), it would be impossible
to blindly use `(is (= ...))`. But even if those user-id fields were static, the
standard `(is (= ...))` would be noisy. Let's check out a typical test:

~~~clojure
(deftest test-some-case
  (is (= {:response
          {:data
           [{:tags ["foo" "bar"]
             :title "Item 1"
             :user-id #uuid "da659703-54d9-49d6-a2b7-c03933c20c5c"
            {:tags ["test" "hello"]
             :title "Item 2"
             :user-id #uuid "e54cf28c-8deb-47ff-8392-7bf90d46aa54"}
            {:tags ["some" "tag"]
             :title "Item 3"
             :user-id #uuid "8a554169-5d0b-4881-8150-7b127a6c04e4"}]}}
         (func-to-check 1))))
~~~

and its output when something is not right:

~~~text
lein test :only alike.core-test/test-some-case

FAIL in (test-some-case) (core_test.clj:326)
expected: (= {:response {:data [{:tags ["foo" "bar"], :title "Item 1",
 :user-id #uuid "da659703-54d9-49d6-a2b7-c03933c20c5c"} {:tags ["test"
 "hello"], :title "Item 2", :user-id #uuid "e54cf28c-8deb-47ff-8392-
  7bf90d46aa54"} {:tags ["some" "tag"], :title "Item 3", :user-id
  #uuid "8a554169-5d0b-4881-8150-7b127a6c04e4"}]}} (func-to-check 1))
  actual: (not (= {:response {:data [{:tags ["foo" "bar"], :title
  "Item 1", :user-id #uuid "da659703-54d9-49d6-a2b7-c03933c20c5c"}
  {:tags ["test" "hello"], :title "Item 2", :user-id #uuid
  "e54cf28c-8deb-47ff-8392-7bf90d46aa54"} {:tags ["some" "tag"],
  :title "Item 3", :user-id #uuid "8a554169-5d0b-4881-8150-7b127a6c04e4"}]}}
  {:response {:data [{:user-id #uuid "6db06e88-1726-4025-ac09-5cc045fc705d",
  :title "Item 1", :tags ["foo" "bar"]} {:user-id #uuid
  "94c53f98-1d47-4ee4-9381-ab357707ce47", :title "Item 2", :tags
  ["test" "hello"]} {:user-id #uuid "49fdb03e-c7b1-4332-a8db-c73190c311e2",
  :title "Item 3", :tags ["some" "tag"]}]}}))
~~~

Absolutely unreadable and no chance to spot the difference! Now we use `alike`:

~~~clojure
(deftest test-some-case
  (is (alike {:response
              {:data
               [{:tags ["foo" "bar"]
                 :title "Item 1"
                 :user-id java.util.UUID}
                {:tags ["test" "hello"]
                 :title "Item 2"
                 :user-id java.util.UUID}
                {:tags ["some" "tag"]
                 :title "Item 3"
                 :user-id java.util.UUID}]}}
             (func-to-check 1))))
~~~

It passes with no issues. Let's imagine we did a mistake and some of the items
has a wrong tag:

~~~clojure
(deftest test-some-case
  (is (alike {:response
              {:data
               [{:tags ["foo" "bar"]
                 :title "Item 1"
                 :user-id java.util.UUID}
                {:tags ["test" "hello"]
                 :title "Item 2"
                 :user-id java.util.UUID}
                {:tags ["some" "dunno"] ;; here!
                 :title "Item 3"
                 :user-id java.util.UUID}]}}
             (func-to-check 1))))
~~~

During the test run, you'll get the following report:

~~~text
lein test :only alike.core-test/test-some-case

FAIL in (test-some-case) (core_test.clj:341)
The expected value =/= actual value
  case :object-object
  path [:response :data 2 :tags 1]
  expected: 123
  actual: "tag"
~~~

The output won't throw a dump of Clojure data on you. Instead, it shows only the
deepest case and the path. Above, the cause of mismatch lurks on the
`:response`&rarr;`:data`&rarr;`2`&rarr;`:tags`&rarr;`1` level. On the left
(expected) side we have 123 and on the right (actual side) there is a string
"tag".

This is the primary point of Alike: report only the deepest difference and never
dump the whole data.

The library provides clear error messages for all known cases. For example, this
is what you'll get when matching two deeply nested data structures:

~~~clojure
(println
  (alike/-repr
    (alike/match {:foo {:bar [1 2 {:data {:user-id java.util.UUID}} 4 5]}}
                 {:foo {:bar [1 2 {:data {:user-id 42}} 4 5]}})))
~~~

~~~text
Expected is an instance of java.util.UUID but got java.lang.Long
  case :class-object
  path [:foo :bar 2 :data :user-id]
  expected: java.util.UUID
  actual: 42
~~~

## Matching Cases

The `alike` operator accepts two expressions: the expected and the actual
ones. Here is a list of predefined types with their logic:

| Expected  | Actual   | Description                                                                                           |
|-----------|----------|-------------------------------------------------------------------------------------------------------|
| Object    | Object   | Compare with the standard =                                                                           |
| Object    | nil      | Always fail                                                                                           |
| nil       | Object   | Always fail                                                                                           |
| Class     | Object   | Check if the object is an instance of the class                                                       |
| Class     | Class    | Check if two classes are the same                                                                     |
| Fn        | nil      | Check if `(function nil)` is true                                                                     |
| Fn        | Fn       | Check if two functions are the same                                                                   |
| Fn        | Object   | Check if `(function x)` is true                                                                       |
| Set       | Set      | Check if both sets have the same items                                                                |
| Set       | nil      | Check if the set has nil                                                                              |
| Pattern   | String   | Check if the string matches the regex pattern (using `re-find`)                                       |
| Map       | Map      | Check if all keys from the expected map present in the actual map, and their values match recursively |
| List      | List     | Check if both lists are of the same length (iterating one by one), and their item match recursively   |
| List      | object[] | See above                                                                                             |
| List      | int[]    | See above                                                                                             |
| List      | long[]   | See above                                                                                             |
| Count     | String   | Check if length of the string is equal to `Count.n`                                                   |
| Count     | Counted  | Check if amount of items in Counted is equal to `Count.n`                                             |
| Substring | String   | Check if the actual string includes a substring (via `clojure.string/includes?`)                      |

If you think of some other possible cases, please open an issue or a PR. Or just
let me know, and I'll add them.

## Special objects

Objects like `Count` or `Substring` are provided by Alike and have constructor
functions named after them:

~~~clojure
(alike/match (alike/count 3) "abc")
true

(alike/match (alike/count 3) [1 2 3])
true

(alike/match (alike/substring "error") "there was an error while...")
true
~~~

## Extending

That's quite easy to define your own matching rules. Say, the expected data has
text strings like "2026-03-25", but the actual data stores them as `LocalDate`
instances. You'd like these two values to match:

~~~clojure
(alike.core/match "2026-03-25" (java.time.LocalDate/parse "2026-03-25"))
~~~

This won't work and will return a `Mismatch` object storing the debug data:

~~~clojure
{:-expected "2026-03-25",
 :-actual #object[java.time.LocalDate 0x6cebab15 "2026-03-25"],
 :-tag :object-object,
 :-path nil}
~~~

Extend the `-match` multimethod as follows:

~~~clojure
(defmethod alike.core/-match [String java.time.LocalDate]
  [string local-date]
  (or (= string (str local-date))
      (alike.core/mismatch string local-date :string-local-date)))
~~~

The `-match` multimethod should return any of these:
- a false/nil value without any details; such response gets wrapped into a
  general `Mismatch` object.
- a custom `Mismatch` object using the `mismatch` constructor function;
- any other true value.

Now the matching will do:

~~~clojure
(alike.core/match "2026-03-25" (java.time.LocalDate/parse "2026-03-25"))
true
~~~

Alike allows to define a custom error message for each case. There is the
`-explain` multimethod which accepts the `Mismatch` object and dispatches it by
the tag field. Let's provide our own error message for dates:

~~~clojure
(defmethod alike.core/-explain :string-local-date [mismatch]
  (let [{:keys [-expected ;; fields available
                -actual
                -path
                -tag]}
        mismatch]
    (format "The actual string date %s doesn't match %s"
            -expected
            -actual)))
~~~

Let's check it out:

~~~clojure
(println
  (alike.core/-explain
    (alike.core/match "2026-03-25"
                      (java.time.LocalDate/parse "2026-03-20"))))

;; The actual string date 2026-03-25 doesn't match 2026-03-20
~~~

You'll get this error message during the tests.

## Other

~~~
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
Ivan Grishaev, 2026. © UNLICENSE ©
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
~~~
