# Clojure Project from Scratch

This repo has a simple "Hello, World" clojure project based on these tools
with a step-by-step guide.
- [Clojure CLI](https://clojure.org/guides/deps_and_cli)
- [tools.build](https://github.com/clojure/tools.build)
- [Babashka](https://babashka.org)
- [Cognitect Labs Test Runner](https://github.com/cognitect-labs/test-runner)
- [nREPL](https://nrepl.org)
- [Rebel](https://github.com/bhauman/rebel-readline)

## Hello, World!

#### Create a minimal `deps.edn`
```
{:paths ["src"]}
```

#### Create a minimal `src/my/app.clj`
```
(ns my.app
  (:gen-class))

(defn -main
  [& args]
  (println "Hello, World!"))
```

#### Execute with clojure CLI
```
$ clj -X my.app/-main
Hello, World!

$ clj -M -m my.app
Hello, World!
```

## Command line args support

#### Update `main.clj`
```
(ns my.app
  (:require [clojure.string :as s])
  (:gen-class))

(defn greet
  [xs]
  (if (empty? xs)
    "Hello, World!" 
    (str "Hello, " (s/join ", " xs) "!")))

(defn -main
  [& args]
  (println (greet args)))
```

#### Execute with clojure CLI
```
$ clj -M -m my.app
Hello, World!

$ clj -M -m my.app Alice Bob
Hello, Alice, Bob!
```

## Babashka as a task runner

#### Create `bb.edn` by simply sym-linking `deps.edn`
`$ ln -s deps.edn bb.edn`

#### Add `:deps` and `:tasks` to deps.edn
```
{:paths ["src"]

 :deps
 {org.clojure/clojure {:mvn/version "1.9.0"}}
 
 :tasks 
 {:init (def MAIN-NS "my.app")

  run
  {:doc "Run main"
   :override-builtin true
   :task (apply clojure "-M -m" MAIN-NS *command-line-args*)}}}
```

#### Execute with Babashka
```
$ bb run
Hello, World!

$ bb run Alice Bob
Hello, Alice, Bob!
```

## Build and run uberjar

#### Add `build.clj`
```
(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'my/app) ;; NOTE: there is /, not . between my and app
(def version "0.0.1")
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean 
  [_]
  (b/delete {:path "target"}))

(defn uber 
  [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :ns-compile '[my.app]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'my.app}))
```

#### Add aliasses to `deps.edn`
```
{:paths ["src"]

 ;; ...
 
 :aliases
 {:build {:deps {io.github.clojure/tools.build {:mvn/version "0.10.9"}}
          :ns-default build}}}
```

#### Build and execute uberjar
```
$ clj -T:build uber
$ java -jar target/app-0.0.1-standalone.jar
Hello, World!

$ java -jar target/app-0.0.1-standalone.jar Alice Bob
Hello, Alice, Bob!
```

#### Add `run-uber` to `build.clj`
```
(ns build
  (:require [clojure.tools.build.api :as b]
            [babashka.process :refer [shell]]))

;; ...

(defn run-uber
  [opts]
  (apply shell "java" "-jar" uber-file (:args opts)))
```

Command line args can be passed like this when directly using `clj`
```
$ clj -T:build run-uber :args '["Alice" "Bob"]'
```

#### Add babashka tasks to `deps.edn`
```
{
 :tasks 
 {uber
  {:doc "Build uberjar"
   :override-builtin true
   :task (clojure "-T:build uber")}
  
  run-uber
  {:doc "Run uberjar"
   :task (let [args *command-line-args*
               opts (if (empty? args) [] [":args" (str args)])]
           (apply clojure "-T:build run-uber" opts))}}

 :aliases
 {:build
  {:deps {io.github.clojure/tools.build {:mvn/version "0.10.9"}
          babashka/process {:mvn/version "0.6.23"}}
   :ns-default build}}}
```

#### Execute with Babashka
```
$ bb uber
$ bb run-uber
Hello, World!

$ bb run-uber Alice Bob
Hello, Alice, Bob!
```

## Tests

#### Add `test/my/app_test.clj`
```
(ns my.app-test
  (:require [clojure.test :refer [deftest testing is]]
            [my.app :refer [greet]))

(deftest greet-tests
  (testing "No args"
    (is (= (greet []) "Hello, World!"))
    (is (= (greet nil) "Hello, World!")))
  (testing "With args"
    (is (= (greet ["Alice" "Bob"]) "Hello, Alice, Bob!"))))
```

#### Update `deps.edn`
```
{:deps
 {org.clojure/core.async {:mvn/version "1.3.610"}}
 
 :tasks 
 {test
  {:doc "Run tests"
   :task (apply clojure "-M:test" *command-line-args*)}}

 :aliases
 {:test
  {:extra-paths ["test"]
   :extra-deps {io.github.cognitect-labs/test-runner {:git/tag "v0.5.1"
                                                      :git/sha "dfb30dd"}}
   :main-opts ["-m" "cognitect.test-runner"]
   :exec-fn cognitect.test-runner.api/test}}}
```

#### Run all tests
```
$ bb test
```

#### Run a single namespace
```
$ bb test -n my.app-test
```

#### Run a single test case
```
$ bb test -v my.app-test/greet-test
```

## REPL / nREPL

#### Rebel REPL

Update `deps.edn` by adding these
```
{
 :task
 {repl
  {:doc "Start Rebel REPL"
   :override-builtin true
   :task (clojure "-T:rebel")}}

 :aliases
 {:rebel
  {:extra-deps {com.bhauman/rebel-readline-nrepl {:mvn/version "0.1.5"}}
   :exec-fn rebel-readline.tool/repl
   :exec-args {}
   :main-opts ["-m" "rebel-readline.main"]}}}
```

Run REPL by
```
$ bb repl
```

#### nREPL Server

Update `deps.edn` by adding these
```
{
 :tasks
 {nrepl-server
  {:doc "Start nREPL server"
   :override-builtin true
   :task (clojure "-M:nrepl -m nrepl.cmdline")}}

 :aliases
 {:nREPL
  {:extra-deps {nrepl/nrepl {:mvn/version "1.3.1"}}}}
```

Start nREPL server by
```
$ bb nrepl-server
```

#### nREPL client using Rebel REPL

Update `deps.edn` by adding these
```
{
 :tasks
 {nrepl
  {:doc "Start nREPL client"
   :task (clojure "-T:nrebel :port" (slurp ".nrepl-port"))}}

 :aliases 
 {:nrebel 
  {:extra-deps {com.bhauman/rebel-readline-nrepl {:mvn/version "0.1.5"}}
   :exec-fn rebel-readline.nrepl/connect
   :exec-args {:background-print false}
   :main-opts ["-m" "rebel-readline.nrepl.main"]}}}
```

Start nREPL client by
```
$ bb nrepl
```
