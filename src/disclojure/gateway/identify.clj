(ns disclojure.gateway.identify
  (:require [disclojure.presence :as presence]
            [disclojure.gateway :as gateway]))

;;; Constants
(def library-name "disclojure")
(def identify-opcode 2)
(def ^:dynamic *os-name* (System/getProperty "os.name"))

(def online-presence
  (presence/create-presence nil nil presence/user-status-online false))

(defn create-identify
  [token shard-num total-shards presence]
  {:token token
   :properties {:$os *os-name*
                :$browser library-name
                :$device library-name}
   :compress false
   :large_threshold 250
   :shard [shard-num total-shards]
   :presence presence})

(defn identify
  ([json-conn token shard-num total-shards]
   (identify json-conn token shard-num total-shards online-presence))
  ([json-conn token shard-num total-shards presence]
   (let [identify-payload (create-identify token shard-num
                                           total-shards presence)]
    (gateway/send-payload json-conn identify-opcode identify-payload))))
