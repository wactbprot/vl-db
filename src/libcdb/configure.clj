(ns libcdb.configure)



(defn gen-opt
  "Defines a set of connection option defaults. Merges it into
  `conf`."
  [{opt :opt :as conf}]
  (let [defaults {; :debug true
                  :query-params {}
                  :pool {:threads 1 :default-per-route 1}}]
    (assoc conf :opt (merge defaults opt))))

(defn auth-opt
  "Assoc credentials as `basic-auth` to the connection options `:opt`
  into the `conf` map. Dissoc `:usr` and `:pwd` afterwards for
  security reasons."
  [{usr :usr pwd :pwd :as conf}]
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
                     :pwd \"password\"
                     :name \"db-name\"}))
  
  ;; or
  (def conf (config {:usr (System/getenv USERNAME) 
                     :pwd (System/getenv PASSWD)
                     :name \"db-name\"}))
  ```"

  [{opt :opt :as conf}]
  (-> {:prot "http"
       :host "localhost"
       :port 5984}
      (merge conf)
      gen-opt
      auth-opt))
