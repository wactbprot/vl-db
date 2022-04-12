(ns vl-db.core
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Collection of functions for CouchDB CRUD operations. `conf` map last."}
  (:require [clj-http.client :as http]
            [cheshire.core :as che]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.io.ByteArrayOutputStream))



;;........................................................................
;; interface
;;........................................................................
(defn- get!  [url opt] (http/get url opt))
(defn- put!  [url opt] (http/put url opt))
(defn- del! [url opt] (http/delete url opt))
(defn- head! [url opt] (http/head url opt))


;;........................................................................
;; helper funs
;;........................................................................

(defn db-url
  "Generates the database url from [[base-url]] and `:name` and assoc it
  to `conf` under `db-url`"
  [{u :url n :name :as conf}]
  {:pre  [(string? n)]}
  (str u "/" n))

(defn doc-url
  "Generates the document `u`rl for the given `id`. Appends the document
  `rev` if provided."
  [id {rev :rev}]
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
  (when (< s 400)
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

(defn config [conf]
  (-> {:prot "http"
       :host "localhost"
       :port 5984
       :name "vl_db"
       :design "share"
       :view "vl"}
      (merge conf)
      auth-opt
      base-url))



;;........................................................................
;; prep ops
;;........................................................................
(defn get-rev [id {opt :opt :as conf}]
  (try
    (-> (doc-url id conf)
        (head! opt)
        res->etag)
    (catch Exception _)))

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
  "Deletes a document with the `id` from the configured database. Turns
  the result into a map."
  [id {opt :opt :as conf}]
  (-> (doc-url id (assoc conf :rev (get-rev id conf)))
      (del! opt)
      res->map))

(defn put-doc
  "Puts the given `doc`ument to the configured database. Turns
  the result into a map. Renews the document if it already exists."
  [{id :_id :as doc} {opt :opt :as conf}]
  (-> (doc-url id conf)
      (put! (assoc opt :body (che/encode (update-rev doc conf))))
      res->map))

(defn put-db
  "Generates a database."
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
