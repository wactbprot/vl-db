(ns vl-db.configure)


(defn auth-opt [{usr :usr pwd :pwd :as conf}]
  (if (and usr pwd)
    (dissoc (assoc-in conf [:opt :basic-auth] [usr pwd]) :usr :pwd)
    conf))

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
       :opt {; :debug true
             :pool {:threads 1 :default-per-route 1}}}
      (merge conf)
      auth-opt))
