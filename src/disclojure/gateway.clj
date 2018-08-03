(ns disclojure.gateway
  (:require [disclojure.http :as http]))

;;; Constants
(def url-suffix "/gateway")
(def url-bot-suffix "/gateway/bot")

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
