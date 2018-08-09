(ns disclojure.gateway.identify
  (:require [disclojure.presence :as presence]
            [disclojure.gateway :as gateway]))

;;; Constants
(def library-name "disclojure")
(def identify-opcode 2)
(def ^:dynamic *os-name* (System/getProperty "os.name"))

(def online-presence
  (presence/create-presence nil nil presence/user-status-online false))

(defn- all-or-none
  [m ks]
  (or (not-any? m ks)
      (every? m ks)))

(defn create-identify
  "Creates the identify payload
   Options taken are: `:shard-num`, `:total-shards` and `:initial-presence`.
   `:shard-num` and `:total-shards` must coexist."
  ([token options]
   {:pre [(all-or-none options [:shard-num :total-shards])]}
   (let [presence (or (options :initial-presence) online-presence)]
     (if (every? options [:shard-num :total-shards])
       (create-identify token (options :shard-num) (options :total-shards) presence)
       (create-identify token 0 1 presence))))
  ([token shard-num total-shards presence]
   {:token token
    :properties {:$os *os-name*
                 :$browser library-name
                 :$device library-name}
    :compress false
    :large_threshold 250
    :shard [shard-num total-shards]
    :presence presence}))

(defn identify
  [json-conn identify-payload]
  (gateway/send-payload json-conn identify-opcode identify-payload))
