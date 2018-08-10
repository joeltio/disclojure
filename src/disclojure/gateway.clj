(ns disclojure.gateway
  (:require [disclojure.http :as http]
            [manifold.stream :as s]))

;;; Constants
(def gateway-version 6)
(def gateway-encoding "json")
(def gateway-compress "zlib-stream")

(def base-url "https://discordapp.com/api")
(def url-suffix "/gateway")
(def url-bot-suffix "/gateway/bot")

(def dispatch-opcode 0)

(defn is-dispatch?
  [payload]
  (= (payload "op") dispatch-opcode))

;;; Getting the gateway endpoint
(defn- create-bot-header
  [bot-token]
  {:headers {"Authorization" (str "Bot " bot-token)}})

(defn create-endpoint-params
  ([]
   (create-endpoint-params gateway-version gateway-encoding gateway-compress))
  ([version]
   (create-endpoint-params version gateway-encoding gateway-compress))
  ([version encoding]
   (create-endpoint-params version encoding gateway-compress))
  ([version encoding compress]
   {:query-params {:v version :encoding encoding :compress compress}}))

(defn- get-endpoint
  ([]
   (http/get-json (str base-url url-suffix)
                  (create-endpoint-params)))
  ([endpoint-params]
   (http/get-json (str base-url url-suffix)
                  endpoint-params)))

(def get-endpoint-memo (memoize get-endpoint))

(defn get-bot-endpoint
  ([bot-token]
   (get-bot-endpoint bot-token (create-endpoint-params)))
  ([bot-token endpoint-params]
   (http/get-json (str base-url url-bot-suffix)
                  (create-bot-header bot-token)
                  endpoint-params)))

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
