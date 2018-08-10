(ns disclojure.gateway.client
  (:require [disclojure.gateway :as gateway]
            [disclojure.http :as http]))

(defn- create-conn
  ([]
   (create-conn {}))
  ([options]
   (http/websocket-json-client @(gateway/get-cached-endpoint options))))
