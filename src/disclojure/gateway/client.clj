(ns disclojure.gateway.client
  (:require [disclojure.gateway :as gateway]
            [disclojure.gateway.heartbeat :as heartbeat]
            [disclojure.gateway.identify :as identify]
            [disclojure.http :as http]
            [manifold.stream :as s]))

(defn- select-values
  [m ks]
  (reduce #(conj %1 (m %2)) [] ks))

(defn- get-endpoint
  [options]
  (-> options
      (get :gateway {})
      (select-values [:version :encoding :compress])
      (#(apply gateway/create-endpoint-params %))
      gateway/get-endpoint-memo))

(defn- get-client
  [options]
  (-> @(get-endpoint options)
      (get "url")
      http/websocket-json-client))

(defn create-client
  "Creates a client to receive the gateway events"
  ([bot-token]
   (create-client bot-token {}))
  ([bot-token options]
   (let [conn @(get-client options)
         seq (heartbeat/start-heartbeat conn)]
     (identify/identify conn (identify/create-identify bot-token options))
     {:conn conn
      :seq seq
      :ready @(s/take! conn)})))
