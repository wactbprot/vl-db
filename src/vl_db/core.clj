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

(defn- delete! [url opt] (http/delete url opt))

(defn- head! [url opt] (http/head url opt))


;;........................................................................
;; helper funs
;;........................................................................
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
(defn base-url [{:keys [prot host port  name] :as conf}]
  (assoc conf
         :url (str prot "://" host ":" port "/"name)))

(defn auth-opt [{usr :usr pwd :pwd :as conf}]
  (if (and usr pwd)
    (dissoc (assoc-in conf [:opt :basic-auth] [usr pwd]) :usr :pwd)
    conf))

(defn config [conf]
  (-> {:prot "http"
       :host "localhost"
       :port 5984
       :name "vl_db"}
      auth-opt
      (merge conf)
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
  (if-let [rev (get-rev id conf)]
    (assoc doc :_rev rev)
    doc))


;;........................................................................
;; crud ops
;;........................................................................
(defn get-doc [id {opt :opt :as conf}]
  (-> (get! (doc-url id conf) opt)
      res->map))

(defn del-doc [id {opt :opt :as conf}]
  (-> (delete! (doc-url id (assoc conf
                                  :rev (get-rev id conf))) opt)
      res->map))

(defn put-doc [{id :_id :as doc} {opt :opt :as conf}]
  (-> (put! (doc-url id conf) (assoc opt
                                     :body (che/encode (update-rev doc conf))))
      res->map))


;;........................................................................
;; view
;;........................................................................
(defn get-view [{opt :opt :as conf}]
  (-> (get! (view-url conf) opt)
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
