(ns vl-db.core
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Collection of functions for CouchDB CRUD ops."}
  (:require [clj-http.client :as http]
            [cheshire.core :as che]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.io.ByteArrayOutputStream))

(defn head [url opt]
  (http/head url opt))

(defn doc-url
  "Generates the document `u`rl for the given `id`. Appends the document
  `rev` if provided."
  [id {u :url rev :rev}]
  {:pre  [(string? id)
          (string? u)]}
  (str u "/" id (when rev (str "?rev=" rev))))

(defn view-url
  "Generates a view url from the params given in the conf map."
  [{u :url d :design v :view}]
  {:pre  [(string? u)
          (string? d)
          (string? v)]}
  (str u "/_design/" d "/_view/" v))

(defn attachment-url 
  "Generates a attachment url from the `id, the `f`ilename and the `url` param given in the conf map."
  [id f conf]
  {:pre  [(string? f)]}
  (str (doc-url id (dissoc conf :rev)) "/" f))

(defn res->byte-array 
  "Turns the `response` body into a byte-array." 
  [{body :body}]
  (with-open [xin  (io/input-stream body)
              xout (ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(defn res->map
  "Tries to parse the `res`ponse body into a `map`."
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

(defn get-rev [{opt :opt :as conf} id]
  (-> (head (doc-url conf id) opt)
      res->etag))

(comment
;;........................................................................
;; crud ops
;;........................................................................
(defn get-doc [{opt :opt :as conf} id]
  (result @(http/get (doc-url conf id) opt)))

(defn get-attachment-as-byte-array [{opt :opt :as conf} id filename]
  (-> @(http/get (attachment-url conf id filename) opt)
      :body
      body->byte-array))

(defn del-doc [{opt :opt :as conf} id]
  (result @(http/delete (doc-url (assoc conf :rev (get-rev conf id)) id) opt)))

(defn put-doc [{opt :opt :as conf} {id :_id :as doc}]
  (result @(http/put (doc-url conf id) (assoc opt :body (che/encode doc)))))

;;........................................................................
;; view
;;........................................................................
(defn get-view [{opt :opt :as conf}]
  (:rows (result @(http/get (view-url conf) opt))))
)
;;........................................................................
;; playground
;;........................................................................
(comment
  (get-doc c/conf "foo")
  (get-rev c/conf "000_REPLICATIONS"))
