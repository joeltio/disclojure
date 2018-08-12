(ns disclojure.gateway
  (:require [disclojure.http :as http]
            [manifold.stream :as s]
            [manifold.deferred :as d]))

;;; Constants
(def gateway-version 6)
(def gateway-encoding "json")
(def gateway-compress "zlib-stream")

(def base-url "https://discordapp.com/api")
(def gateway-url (str base-url "/gateway"))
(def bot-gateway-url (str base-url "/gateway/bot"))

(def dispatch-opcode 0)

(defn get-opcode
  [payload]
  (payload "op"))

(defn is-dispatch?
  [payload]
  (= (get-opcode payload) dispatch-opcode))

;;; Getting the gateway endpoint
(defn- create-bot-header
  [bot-token]
  {:headers {"Authorization" (str "Bot " bot-token)}})

(defn- create-endpoint-params
  [options]
  (let [version (or (options :version) gateway-version)
        encoding (or (options :encoding) gateway-encoding)
        compress (or (options :compress) gateway-compress)]
    {:query-params {:v version
                    :encoding encoding
                    :compress compress}}))

(defn- get-endpoint
  ([]
   (get-endpoint {}))
  ([options]
   (d/chain' (http/get-json gateway-url
                            (create-endpoint-params options))
             #(% "url"))))

(def get-cached-endpoint (memoize get-endpoint))

(defn get-bot-gateway
  ([bot-token]
   (get-bot-gateway bot-token {}))
  ([bot-token options]
   (http/get-json bot-gateway-url
                  (create-bot-header bot-token)
                  (create-endpoint-params options))))

(defn create-payload
  ([opcode data]
   {:op opcode :d data})
  ([data seq t]
   {:op dispatch-opcode :d data :s seq :t t}))

(defn send-payload
  ([json-conn payload]
   (s/put! json-conn payload))
  ([json-conn opcode data]
   (s/put! json-conn {:op opcode :d data}))
  ([json-conn data seq t]
   (s/put! json-conn {:op dispatch-opcode :d data :s seq :t t})))
