# vl-db

Collection of functions for CouchDB interaction.

## init conf

```clojure
(def c (conf {:usr "username" :pwd "password"}))

;; or
(def c  (conf {:usr (System/getenv "USERNAME) :pwd (System/getenv "PASSWD")}))

```
