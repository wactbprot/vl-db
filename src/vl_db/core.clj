(ns vl-db.core
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Collection of functions for CouchDB CRUD operations. `conf` map last."}
  (:require [clj-http.client :as http]
            [cheshire.core :as che]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import java.io.ByteArrayOutputStream))



(defn- get! 
  "Generic `GET` request with the following opt map:

  ```clojure
  {:basic-auth [usr pwd]
   :content-type content-type}
  ```"
  [url opt]
  (http/get url opt))

(defn- put!
  "Generic `PUT` request with the following opt map:
  
  ```clojure
  {:basic-auth [usr pwd]
   :content-type content-type
   :content-length (count (.getBytes body))
   :body body}
  ```"
  [url opt]
  (http/put url opt))

(defn- delete! 
  "Generic `DELETE` request with the following opt map:

  ```clojure
  {:basic-auth [usr pwd]
   :content-type content-type}
  ```"
  [url opt]
  (http/delete url opt))

(defn- head!
  "Generic `HEAD` request with the following opt map:
  
  ```clojure
  {:basic-auth [usr pwd]
  :content-type content-type}
  ```"
  [url opt]
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
  "Generates a attachment url from the `id, the `f`ilename and the `url`
  param given in the conf map."
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

(defn base-url [{:keys [prot host port  name] :as conf}]
  (assoc conf
         :url (str prot "://" host ":" port "/"name)))

(defn auth-opt [{usr :usr pwd :pwd :as conf}]
  (if (and usr pwd)
    (dissoc
     (assoc-in conf [:opt :basic-auth] [usr pwd]) :usr :pwd)
    conf))

(defn conf [conf]
  (-> {:prot "http"
       :host "localhost"
       :port 5984
       :name "vl_db"}
      auth-opt
      (merge conf)
      base-url ))



;;........................................................................
;; prep ops
;;........................................................................
(defn get-rev [id {opt :opt :as conf}]
  (-> (head! (doc-url id conf) opt)
      res->etag))

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

(defn get-attachment-as-byte-array [{opt :opt :as conf} id filename]
  (-> (get! (attachment-url id filename conf) opt)
      res->map))

(defn del-doc [id {opt :opt :as conf}]
  (-> (delete! (doc-url id (assoc conf
                                  :rev (get-rev conf id))) opt)
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
;; playground
;;........................................................................
(comment
  (get-doc c/conf "foo")
  (get-rev c/conf "000_REPLICATIONS"))
