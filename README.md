# vl-db

Collection of functions for CouchDB interaction. 

## init conf

```clojure
(def conf (config {:usr "username" :pwd "password"}))

;; or
(def conf (config {:usr (System/getenv "USERNAME) :pwd (System/getenv "PASSWD")}))

```


## Unit tests 

Run in shell with:

```shell
$ clojure -X:test
```

## Generate api docs

```shell
clojure -X:codox
```

### Build uberjar (tools.deps and tools.build)

```shell
clj -T:build clean
clj -T:build jar
```
