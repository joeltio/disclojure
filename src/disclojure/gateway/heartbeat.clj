(ns disclojure.gateway.heartbeat
  (:require [disclojure.gateway :as gateway]
            [manifold.time :as t]
            [manifold.stream :as s]
            [manifold.deferred :as d]))

;;; Constants
(def heartbeat-opcode 1)
(def heartbeat-ack-opcode 11)
(def heartbeat-timeout 1000)

(defn- take-interval
  [conn]
  (d/chain' (s/take! conn) #(get-in % ["d" "heartbeat_interval"])))

#_(defn start-heartbeat
  [conn event-bus heartbeat-atom]
  ;; Add periodic heartbeater
  (start-periodic-heartbeat conn event-bus
                            heartbeat-atom @(take-interval conn))
  ;; Add heartbeat responder
  (add-heartbeat-responder conn event-bus heartbeat-atom)
  ;; Add heartbeat incrementer
  (add-heartbeat-incrementer event-bus heartbeat-atom))
