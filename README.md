# vl-db

Collection of functions for CouchDB interaction.

## init conf

```clojure
(conf {:usr "username" :pwd "password"})
;; or
(conf {:usr (System/getenv "USERNAME) :pwd (System/getenv "PASSWD")})

```
