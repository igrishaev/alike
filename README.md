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
- [Extending](#extending)
- [Other](#other)

<!-- tocstop -->

## Installation

Lein:

~~~clojure
[com.github.igrishaev/alike "0.1.0-SNAPSHOT"]
~~~

Deps.edn

~~~clojure
com.github.igrishaev/alike {:mvn/version "0.1.0-SNAPSHOT"}
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
standard `(is (= ...))` is quite noisy. Let's see at a typical test:

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

## Matching Cases

The `alike` operator accepts two expressions: the expected and the actual
ones. Here is a list of predefined types with their logic:

| Expected | Actual   | Description                                                                                           |
|----------|----------|-------------------------------------------------------------------------------------------------------|
| Object   | Object   | Compare with the standard =                                                                           |
| Object   | nil      | Always fail                                                                                           |
| nil      | Object   | Always fail                                                                                           |
| Class    | Object   | Check if the object is an instance of the class                                                       |
| Class    | Class    | Check if two classes are the same                                                                     |
| function | nil      | Check if `(function nil)` is true                                                                     |
| function | function | Check if two functions are the same                                                                   |
| function | Object   | Check if `(function x)` is true                                                                       |
| Set      | Set      | Check if both sets have the same items                                                                |
| Set      | nil      | Check if the set has nil                                                                              |
| String   | String   | Check ... TODO                                                                                        |
| Pattern  | String   | Check if the string matches the regex pattern (using `re-find`)                                       |
| Map      | Map      | Check if all keys from the expected map present in the actual map, and their values match recursively |
| List     | List     | Check if both lists are of the same length (iterating one by one), and their itesm match recursively  |
| List     | object[] | See above                                                                                             |
| List     | int[]    | See above                                                                                             |
| List     | long[]   | See above                                                                                             |
| Count    | String   | Check if lenfth of the string is equal to `Count.n`                                                   |
| Count    | Counted  | Check if amount of imtes in Counted is equal to `Count.n`                                             |

If you think of some other possible cases, please open an issue or a PR.

## Extending

## Other

~~~
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
Ivan Grishaev, 2026. © UNLICENSE ©
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
~~~
