# libcdb

Library (lib) provides a collection of functions for CouchDB (cdb) interaction. 

## Dependencies

* [clj-http](https://clojars.org/clj-http) 
* [cheshire.core](https://clojars.org/cheshire)

## Dokumentation

* [API](https://a75438.berlin.ptb.de/libcdb/docs/index.html)


## Init conf

```clojure
(def conf (config {:usr "username" :pwd "password"}))

;; or
(def conf (config {:usr (System/getenv "USERNAME") :pwd (System/getenv "PASSWD")}))

```


## Unit tests 

Run in shell with:

```shell
$ clojure -X:test
```

## Code coverage 

Run in shell with:

```shell
$ clojure -X:coverage
```

## Generate api docs

```shell
clojure -X:codox
```

upload:

```shell
scp -r docs/ bock04@a75438://var/www/html/libcdb/
```

### Build uberjar (tools.deps and tools.build)

```shell
clj -T:build clean
clj -T:build jar
```
