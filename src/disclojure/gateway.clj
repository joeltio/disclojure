(ns disclojure.gateway
  (:require [disclojure.http :as http]
            [manifold.stream :as s]))

;;; Constants
(def base-url "https://discordapp.com/api")
(def url-suffix "/gateway")
(def url-bot-suffix "/gateway/bot")

(def dispatch-opcode 0)

;;; Getting the gateway endpoint
(defn- create-bot-header
  [bot-token]
  {:headers {"Authorization" (str "Bot " bot-token)}})

(defn get-endpoint
  ([]
   (http/get-json (str base-url url-suffix)))
  ([bot-token]
   (http/get-json (str base-url url-bot-suffix)
                  (create-bot-header bot-token))))

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
