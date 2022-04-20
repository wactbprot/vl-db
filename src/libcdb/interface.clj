(ns libcdb.interface
  ^{:author "Thomas Bock <thomas.bock@ptb.de>"
    :doc "Provides a interface to the http client libbrary."}
  (:require [clj-http.client :as http]))

(defn- req! [f {pool :pool :as opt}]
  (try
    (http/with-connection-pool pool
      (f opt))
    (catch Exception e
      {:error (.getMessage e)})))

(defn get! [url opt] (req! #(http/get url %) opt))
(defn put! [url opt] (req! #(http/put url %) opt))
(defn del! [url opt] (req! #(http/delete url %) opt))
(defn head! [url opt] (req! #(http/head url %) opt))
