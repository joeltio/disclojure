(ns disclojure.gateway.heartbeat
  "Heartbeats to Discord API to show that the connection is still alive.
   Once connected to a gateway, the client has to start heartbeating (see
   [Discord Heartbeating](https://discordapp.com/developers/docs/topics/gateway#heartbeating))

   There are three main processes for heartbeating:
   1. Heartbeating every interval
   2. Replying to a requested heartbeat
   3. Incrementing heartbeat sequence

   1. Heartbeating every interval
   After the client has connected, it will receive a hello payload (opcode 10):
   ```
   {
     \"op\": 10,
     \"d\": {
      \"heartbeat_interval\": 45000,
      \"_trace\": [\"discord-gateway-prd-1-99\"]
    }
   }
   ```
   The client then has to start sending heartbeats (opcode 1) to the gateway
   every heartbeat interval (which is in milliseconds).

   2. Replying to a requested heartbeat
   The gateway can request for a heartbeat by sending a heartbeat (opcode 1).
   The response should be the same as 1.--a heartbeat.

   3. Incrementing heartbeat sequence
   A heartbeat also consists of a sequence number (which is represented using
   an atom). This sequence number is the largest sequence number received from
   a gateway dispatch (opcode 0). This heartbeat sequence is used in the
   heartbeat payload as well as for resuming events."
  (:require [disclojure.gateway :as gateway]
            [manifold.time :as t]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as b]
            [manifold.executor :as ex])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor]))

;;; Constants
(def heartbeat-opcode 1)
(def heartbeat-ack-opcode 11)
(def heartbeat-timeout 1000)

(def ^:private thread-name-prefix "heartbeat-pool-")

(defn- take-interval
  [conn]
  (d/chain' (s/take! conn) #(get-in % ["d" "heartbeat_interval"])))

(defn- create-heartbeat-clock
  [thread-pool-size]
  (let [cnt (atom 0)
        name-generator #(str thread-name-prefix (swap! cnt inc))]
    (->> (ex/thread-factory name-generator (deliver (promise) nil))
         (ScheduledThreadPoolExecutor. thread-pool-size)
         t/scheduled-executor->clock)))

(defn- heartbeat
  [conn ack-subscriber heartbeat-atom]
  ;; Send the heartbeat
  (gateway/send-payload conn heartbeat-opcode @heartbeat-atom)
  ;; Try to receive a heartbeat ack
  (when-not @(s/try-take! ack-subscriber false heartbeat-timeout false)
    ;; Failed, close connection and throw exception
    (s/close! conn)
    (throw (Exception. "Heartbeat ack timed out or stream take! error"))))

(defn- start-periodic-heartbeat
  [conn event-bus heartbeat-atom interval]
  (let [heartbeat-ack-subscriber (b/subscribe event-bus heartbeat-ack-opcode)
        clock (create-heartbeat-clock 1)]
    ;; A clock is required here as t/every and t/try-take! both use the same
    ;; clock (manifold's *clock*), and the clock is single-threaded. Without an
    ;; additional clock, it will deadlock.
    ;; Oddly enough, t/with-clock seems to only bind to t/every, so only one
    ;; thread is needed for the clock.
    (t/with-clock clock
      (t/every interval
               #(heartbeat conn heartbeat-ack-subscriber heartbeat-atom)))))

#_(defn start-heartbeat
  [conn event-bus heartbeat-atom]
  ;; Add periodic heartbeater
  (start-periodic-heartbeat conn event-bus
                            heartbeat-atom @(take-interval conn))
  ;; Add heartbeat responder
  (add-heartbeat-responder conn event-bus heartbeat-atom)
  ;; Add heartbeat incrementer
  (add-heartbeat-incrementer event-bus heartbeat-atom))
