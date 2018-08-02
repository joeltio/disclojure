(ns disclojure.http
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [clojure.data.json :as json]))

(defn- http-body->str
  [request]
  (-> @request
      :body
      bs/to-string))

(defn get-json
  [& args]
  (d/future (-> (apply http/get args)
                http-body->str
                json/read-str)))
