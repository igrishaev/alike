(defproject com.github.igrishaev/alike "0.1.1-SNAPSHOT"

  :description
  "A simple matching library"

  :url
  "https://github.com/igrishaev/alike"

  :license
  {:name "The Unlicense"
   :url "https://choosealicense.com/licenses/unlicense/"}

  :managed-dependencies
  [[org.clojure/clojure "1.11.1"]]

  :dependencies
  [[org.clojure/clojure :scope "provided"]]

  :release-tasks
  [["vcs" "assert-committed"]
   ["test"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["deploy" "clojars"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure]]}})
