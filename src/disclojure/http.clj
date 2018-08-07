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

(defn- stream->json-stream
  [stream]
  (let [json-incoming-stream (s/stream 0 (map json/read-str))
        json-outbound-stream (s/stream 0 (map json/write-str))]
    ;; Connect the json streams to the stream
    (s/connect stream json-incoming-stream)
    (s/connect json-outbound-stream stream)
    ;; Splice the inboud and outbound stream
    (s/splice json-outbound-stream json-incoming-stream)))

(defn websocket-json-client
  [url]
  (d/chain' (http/websocket-client url) stream->json-stream))
