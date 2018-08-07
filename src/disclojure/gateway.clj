(ns disclojure.gateway
  (:require [disclojure.http :as http]
            [manifold.stream :as s]))

;;; Constants
(def url-suffix "/gateway")
(def url-bot-suffix "/gateway/bot")

(def dispatch-opcode 0)
(def identify-opcode 2)

(def identify-library-name "disclojure")
(def ^:dynamic *os-name* (System/getProperty "os.name"))

;;; Getting the gateway endpoint
(defn- create-bot-header
  [bot-token]
  {:headers {"Authorization" (str "Bot " bot-token)}})

(defn get-endpoint
  ([base-url]
   (http/get-json (str base-url url-suffix)))
  ([base-url bot-token]
   (http/get-json (str base-url url-bot-suffix)
                  (create-bot-header bot-token))))

(defn send-payload
  ([json-conn opcode data]
   (s/put! json-conn {:op opcode :d data}))
  ([json-conn data seq t]
   (s/put! json-conn {:op dispatch-opcode :d data :s seq :t t})))

(defn create-shard
  [shard-num total-shards]
  [shard-num total-shards])

(defn create-activity
  [name type options]
  (merge options
         {:name name
          :type type}))

(defn create-presence
  [since game status afk?]
  {:since since
   :game game
   :status status
   :afk afk?})

(defn create-identify
  [token shard presence]
  {:token token
   :properties {:$os *os-name*
                :$browser identify-library-name
                :$device identify-library-name}
   :compress false
   :large_threshold 250
   :shard shard
   :presence presence})

(defn identify
  [json-conn identify-payload]
  (send-payload json-conn identify-opcode identify-payload))
