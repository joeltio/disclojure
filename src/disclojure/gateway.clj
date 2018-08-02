(ns disclojure.gateway
  (:require [disclojure.http :as http]))

(def url-suffix "/gateway")
(def url-bot-suffix "/gateway/bot")

(defn- create-bot-header
  [bot-token]
  {:headers {"Authorization" (str "Bot " bot-token)}})

(defn get-gateway-url
  ([base-url]
   (http/get-json (str base-url url-suffix)))
  ([base-url bot-token]
   (http/get-json (str base-url url-bot-suffix)
                  (create-bot-header bot-token))))
