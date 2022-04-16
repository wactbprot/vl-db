(ns vl-db.configure)


(defn auth-opt [{usr :usr pwd :pwd :as conf}]
  (if (and usr pwd)
    (dissoc (assoc-in conf [:opt :basic-auth] [usr pwd]) :usr :pwd)
    conf))

(defn base-url
  [{:keys [prot host port] :as conf}]
  {:pre  [(string? prot)
          (string? host)
          (number? port)]}
  (assoc conf :url (str prot "://" host ":" port)))

(defn config
  "Returns a config map to bootstrap the `vl-db` configuration.

  NOTE: `clj-http.client` provides an `:as :json` `opt`ion. However,
  it should still be de- and encoded via `cheshire.core` to keep
  easier more transparent control over the result.

  Example:
  ```clojure
  (def conf (config {:usr \"username\"
                     :pwd \"password\"}))
  
  ;; or
  (def conf (config {:usr (System/getenv USERNAME) 
                     :pwd (System/getenv PASSWD)}))
  ```"

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
