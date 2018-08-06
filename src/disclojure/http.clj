(ns disclojure.http
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clojure.data.json :as json]))

(defn- http-body->str
  [request]
  (d/chain request
           :body
           bs/to-string))

(defn get-json
  [& args]
  (d/chain (apply http/get args)
           http-body->str
           json/read-str))

(defn- connect-via-fn
  [src f dst]
  (s/connect-via src #(s/put! dst (f %)) dst))

(defn- stream->json-stream
  [stream]
  (let [json-incoming-stream (s/stream)
        json-outbound-stream (s/stream)]
    ;; Connect the json streams to the stream
    (connect-via-fn stream json/read-str json-incoming-stream)
    (connect-via-fn json-outbound-stream json/write-str stream)
    ;; Splice the inboud and outbound stream
    (s/splice json-outbound-stream json-incoming-stream)))

(defn websocket-json-client
  [url]
  (d/chain' (http/websocket-client url) stream->json-stream))
