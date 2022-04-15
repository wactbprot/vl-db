(ns vl-db.core
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Collection of functions for CouchDB CRUD operations. 
          `conf` map last."}
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [cheshire.core :as che]
            [clojure.string :as string])
  (:import java.io.ByteArrayOutputStream))



;;........................................................................
;; interface
;;........................................................................
(defn req [f {pool :pool :as opt}]
  (try
    (http/with-connection-pool pool
      (f opt))
    (catch Exception e
      {:error (.getMessage e)})))

(defn get! [url opt] (req #(http/get url %) opt))
(defn put! [url opt] (req #(http/put url %) opt))
(defn del! [url opt] (req #(http/delete url %) opt))
(defn head! [url opt] (req #(http/head url %) opt))


;;........................................................................
;; helper funs
;;........................................................................
(defn db-url
  "Generates the database url from [[base-url]] and `:name` and assoc it
  to `conf` under `db-url`"
  [{u :url n :name :as conf}]
  {:pre  [(string? n)]}
  (str u "/" n))

(defn uuids-url [{u :url}] (str u "/_uuids"))

(defn up-url [{u :url}] (str u "/_up"))

(defn doc-url
  "Generates the document `u`rl for the given `id`. Appends the document
  `rev` if provided."
  [id {rev :rev :as conf}]
  {:pre  [(string? id)]}
  (str (db-url conf) "/" id (when rev (str "?rev=" rev))))

(defn view-url
  "Generates a view url from the params given in the conf map."
  [{u :db-url d :design v :view}]
  {:pre  [(string? u)
          (string? d)
          (string? v)]}
  (str u "/_design/" d "/_view/" v))

(defn attachment-url 
  "Generates a attachment url from the `id, the `f`ilename and the `url`
  param given in the conf map."
  [id f conf]
  {:pre  [(string? f)]}
  (str (doc-url id (dissoc conf :rev)) "/" f))

(defn res->byte-array 
  "Turns the `response` body into a byte-array." 
  [{b :body}]
  (with-open [xi (io/input-stream b)
              xo (ByteArrayOutputStream.)]
    (io/copy xi xo)
    (.toByteArray xo)))

(defn res->map
  "Tries to parse the `res`ponse body. Returns a `map`."
  [{b :body}]
  (try
    (che/parse-string-strict b true )
    (catch Exception e
      {:error (.getMessage e)})))

(defn res->etag
  "Extracts the etag from the `res`ponse."
  [{s :status :as res}]
  (when (and s (< s 400))
    (string/replace (get-in res [:headers :etag]) #"\"" "")))


;;........................................................................
;; config
;;........................................................................
(defn base-url
  [{:keys [prot host port] :as conf}]
  {:pre  [(string? prot)
          (string? host)
          (number? port)]}
  (assoc conf :url (str prot "://" host ":" port)))

(defn auth-opt [{usr :usr pwd :pwd :as conf}]
  (if (and usr pwd)
    (dissoc (assoc-in conf [:opt :basic-auth] [usr pwd]) :usr :pwd)
    conf))

(defn config
  "Returns a config map to bootstrap the `vl-db` configuration.

  NOTE: `clj-http.client` provides an `:as :json` `opt`ion. However,
  it should still be de- and encoded via `cheshire.core` to keep
  easier more transparent control over the result."
  [conf]
  (-> {:prot "http"
       :host "localhost"
       :port 5984
       :name "vl_db"
       :design "share"
       :view "vl"
       :opt {:pool {:threads 1 :default-per-route 1}}}
      (merge conf)
      auth-opt
      base-url))



;;........................................................................
;; prep ops
;;........................................................................
(defn get-rev [id {opt :opt :as conf}]
  (-> (doc-url id conf)
      (head! opt)
       res->etag))

(defn update-rev [{id :_id :as doc} conf]
  (if-let [r (get-rev id conf)]
    (assoc doc :_rev r)
    doc))

(defn assoc-param [k conf]
  (if-let [v (get conf k)]
    (dissoc (assoc-in conf [:opt :query-params] {k v}) k)
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
      (get! opt)
      res->map))

(defn del-doc
  "Deletes a document with the `id` from the configured database. The
  Result is turned into a map."
  [id {opt :opt :as conf}]
  (-> (doc-url id (assoc conf :rev (get-rev id conf)))
      (del! opt)
      res->map))

(defn put-doc
  "Puts the given `doc`ument to the configured database. Turns the
  result into a map. Renews the `:_rev` key id document if it already
  exists."
  [{id :_id :as doc} {opt :opt :as conf}]
  (-> (doc-url id conf)
      (put! (assoc opt :body (che/encode (update-rev doc conf))))
      res->map))

(defn put-db
  "Generates a database with the name given with `conf` key `:name`."
  [{opt :opt :as conf}]
  (-> (db-url conf)
      (put! opt)
      res->map))


;;........................................................................
;; view
;;........................................................................
(defn get-view [{opt :opt :as conf}]
  (-> (view-url conf)
      ;; add params like key, startkey ...
      (get! opt)
      res->map
      :rows))


;;........................................................................
;; uuid
;;........................................................................
(defn get-uuids
  "Requests one or more (depending on `n`) Universally Unique
  Identifiers (UUIDs) from the CouchDB instance. The response is a
  JSON object providing a list of UUIDs."
  ([conf]
   (get-uuids 1 conf))
  ([n {opt :opt :as conf}]
   (-> (uuid-url conf)
       (get! (assoc opt :query-params {:count n}))
       res->map)))


;;........................................................................
;; up
;;........................................................................
(defn up?
  "Confirms that the server is up, running, and ready to respond to
  requests.

  Example:
  ```clojure
  (up? (config {}))
  ```"
  [{opt :opt :as conf}]
   (-> (up-url conf)
       (get! opt)
       res->map
       :status
       (= "ok")))
;;........................................................................
;; attachments
;;........................................................................
(defn get-attachment-as-byte-array [{opt :opt :as conf} id filename]
  (-> (get! (attachment-url id filename conf) opt)
      res->map))

;;........................................................................
;; playground
;;........................................................................
(comment
  (get-doc c/conf "foo")
  (get-rev c/conf "000_REPLICATIONS"))
