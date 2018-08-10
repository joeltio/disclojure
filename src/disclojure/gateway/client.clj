(ns disclojure.gateway.client
  (:require [disclojure.gateway :as gateway]
            [disclojure.gateway.heartbeat :as heartbeat]
            [disclojure.gateway.identify :as identify]
            [disclojure.http :as http]
            [manifold.stream :as s]))

(defn- create-conn
  ([]
   (create-conn {}))
  ([options]
   (http/websocket-json-client @(gateway/get-cached-endpoint options))))
