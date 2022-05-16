(ns libcdb.core
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Collection of functions for CouchDB CRUD operations.
          `conf` map last."}
  (:require [clojure.java.io :as io]
            [cheshire.core :as che]
            [clojure.string :as string]
            [libcdb.configure :as c]
            [libcdb.interface :as http])
  (:import java.io.ByteArrayOutputStream
           java.nio.file.Paths
           java.nio.file.Files))

(defn config [conf] (c/config conf))

;;........................................................................
;; generate urls
;;........................................................................
(defn base-url
  [{:keys [prot host port] :as conf}]
  {:pre  [(string? prot)
          (string? host)
          (number? port)]}
  (str prot "://" host ":" port))

(defn db-url
  "Generates the database url from [[base-url]] and `:name` and assoc it
  to `conf` under `db-url`"
  [{n :name :as conf}]
  {:pre  [(string? n)]}
  (str (base-url conf) "/" n))

(defn uuids-url [conf] (str (base-url conf) "/_uuids"))

(defn up-url [conf] (str (base-url conf) "/_up"))

(defn doc-url
  "Generates the document url for the given `id`. Appends the document
  `rev` if provided."
  [id {rev :rev :as conf}]
  {:pre  [(string? id)]}
  (str (db-url conf) "/" id (when rev (str "?rev=" rev))))

(defn view-url
  "Generates a view url from the params given in the conf map."
  [{d :design v :view :as conf}]
  {:pre  [(string? d)
          (string? v)]}
  (str (db-url conf) "/_design/" d "/_view/" v))

(defn attachment-url
  "Generates a attachment url from the `id, the `f`ilename and the `url`
  param given in the conf map."
  [id f conf]
  {:pre  [(string? f)]}
  (str (doc-url id (dissoc conf :rev)) "/" f))


;;........................................................................
;; helper functions
;;........................................................................
(defn res->byte-array
  "Turns the `res`ponse body into a byte-array.

  NOTE: The clj-http library provides a `{:as :byte-array}` option
  which is active at the [[get-attachment-as-byte-array]]
  function. `res->byte-array` is therefore not needed for attachment
  retrieval."
  [{body :body}]
  (with-open [xi (io/input-stream body)
              xo (ByteArrayOutputStream.)]
    (io/copy xi xo)
    (.toByteArray xo)))

(defn filename->byte-array
  "Reads a file with the name `filename` into a byte-array"
  [filename]
  (try
    (-> filename
        (Paths/get (into-array [""]))
        (Files/readAllBytes))
  (catch Exception e
    {:error (.getMessage e)})))

(defn res->map
  "Tries to parse the `res`ponse body. Returns a `map`."
  [{body :body :as res}]
  (if body
    (try
      (che/parse-string-strict body true )
      (catch Exception e
        {:error (.getMessage e)}))
    res))

(defn res->etag
  "Extracts the `etag` from the `res`ponse.

  NOTE: The Couchdb-Etag is the revision `:_rev` of a document."
  [{s :status :as res}]
  (when (and s (< s 400))
    (string/replace (-> res :headers :etag) #"\"" "")))


;;........................................................................
;; prep ops
;;........................................................................
(defn get-rev
  "Returns the revision of a document with the id `id` by executionof a
  `HEAD` request."
  [id {opt :opt :as conf}]
  (-> (doc-url id conf)
      (http/head! opt)
       res->etag))

(defn update-rev
  "Updates the revision of the given `doc`ument."
  [{id :_id :as doc} conf]
  (if-let [r (get-rev id conf)]
    (assoc doc :_rev r)
    doc))


;;........................................................................
;; query params
;;........................................................................
(defn assoc-param [k conf]
  (if-let [v (get conf k)]
    (dissoc (assoc-in conf [:opt :query-params k] (che/encode v)) k)
    conf))

(defn param-opt [conf]
  (->> conf
       (assoc-param :key)
       (assoc-param :startkey)
       (assoc-param :endkey)))


;;........................................................................
;; crud ops
;;........................................................................
(defn get-doc
  "Gets a document with the `id` from the configured database. Turns
  the result into a map."
  [id {opt :opt :as conf}]
  (-> (doc-url id conf)
      (http/get! opt)
      res->map))

(defn del-doc
  "Deletes a document with the `id` from the configured database. The
  Result is turned into a map."
  [id {opt :opt :as conf}]
  (-> (doc-url id (assoc conf :rev (get-rev id conf)))
      (http/del! opt)
      res->map))

(defn put-doc
  "Puts the given `doc`ument to the configured database. Turns the
  result into a map. Renews the `:_rev` key id document if it already
  exists."
  [{id :_id :as doc} {opt :opt :as conf}]
  (-> (doc-url id conf)
      (http/put! (assoc opt :body (che/encode (update-rev doc conf))))
      res->map))

(defn put-db
  "Generates a database with the name given with `conf` key `:name`."
  [{opt :opt :as conf}]
  (-> (db-url conf)
      (http/put! opt)
      res->map))

(defn get-db
  "Get a info map about the database with the name given with `conf` key
  `:name`. Info map entries are documented
  [here](https://docs.couchdb.org/en/3.2.0/json-structure.html#couchdb-database-information-object)."
  [{opt :opt :as conf}]
  (-> (db-url conf)
      (http/get! opt)
      res->map))

(defn del-db
  "Generates a database with the name given with `conf` key `:name`."
  [{opt :opt :as conf}]
  (-> (db-url conf)
      (http/del! opt)
      res->map))


;;........................................................................
;; view
;;........................................................................
(defn get-view
  "Gets the database index described with `:design` and `:view`. The
  result set can be reduced by `:key`, `:startkey` and `:endkey`
  included in the `conf` map."
  [conf]
  (-> (view-url conf)
      (http/get! (:opt (param-opt conf)))
      res->map
      :rows))


;;........................................................................
;; uuid
;;........................................................................
(defn get-uuids
  "Requests `n` Universally Unique Identifiers (UUIDs) from the CouchDB
  instance. The response is a JSON object providing a list of UUIDs."
  ([conf]
   (get-uuids 1 conf))
  ([n {opt :opt :as conf}]
   (-> (uuids-url conf)
       (http/get! (assoc opt :query-params {:count n}))
       res->map
       :uuids)))


;;........................................................................
;; up
;;........................................................................
(defn up?
  "Predicate function that confirms that the server is up, running, and
  ready to respond to requests.

  Example:
  ```clojure
  (up? (config {}))
  ```"
  [{opt :opt :as conf}]
   (-> (up-url conf)
       (http/get! opt)
       res->map
       :status
       (= "ok")))


;;........................................................................
;; attachments
;;........................................................................
(defn get-attachment
  "Gets a attachment with `filename` from the document with the
  identifier `id`."
  [id filename {opt :opt :as conf}]
  (-> (attachment-url id filename conf)
      (http/get! opt)
      :body))

(defn get-attachment-as-byte-array
  "Gets a attachment with `filename` from the document with the
  identifier `id` and  turns the body into a byte-array."
  [id filename conf]
  (get-attachment id filename (assoc-in conf [:opt :as] :byte-array)))

(defn put-attachment
  "Puts the `body` as an attachment with the name `filename` to the
  document with the id `id`. The `body`should be a `byte-array`"
  [id filename body {opt :opt :as conf}]
  (-> (attachment-url id filename conf)
      (http/put! (assoc opt
                        :headers {:if-match (get-rev id conf)}
                        :body body))
      res->map))

(defn put-attachment-by-filename
  "Reads the file with the name `filename` and turns it into a
  byte-array. Calls [[put-attachment]] with the result as body."
  [id filename {opt :opt :as conf}]
  (put-attachment id filename  (filename->byte-array filename) conf))
