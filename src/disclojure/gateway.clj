(ns disclojure.gateway
  (:require [disclojure.http :as http]
            [manifold.stream :as s]))

;;; Constants
(def url-suffix "/gateway")
(def url-bot-suffix "/gateway/bot")

(def heartbeat-opcode 1)

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


(defn heartbeat
  [json-conn seq]
  @(s/put! {"op" heartbeat-opcode "d" seq}))
