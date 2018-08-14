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

(def ^:private is-heartbeat?
  (partial gateway/is-opcode? heartbeat-opcode))
(def ^:private is-heartbeat-ack?
  (partial gateway/is-opcode? heartbeat-ack-opcode))

(defn- take-interval
  [conn]
  (d/chain' (s/take! conn) #(get-in % ["d" "heartbeat_interval"])))

(def ^:private heartbeat-ack-stream
  (partial s/filter is-heartbeat-ack?))

(def ^:private heartbeat-stream
  (partial s/filter is-heartbeat?))

(defn- update-heartbeat-atom!
  [heartbeat-atom new-val]
  (swap! heartbeat-atom #(if (nil? %1)
                           %2
                           (max %1 %2)) new-val))

(defn- create-heartbeat-clock
  [thread-pool-size]
  (let [cnt (atom 0)
        name-generator #(str thread-name-prefix (swap! cnt inc))]
    (->> (ex/thread-factory name-generator (deliver (promise) nil))
         (ScheduledThreadPoolExecutor. thread-pool-size)
         t/scheduled-executor->clock)))

(defn- heartbeat
  [conn ack-stream heartbeat-atom]
  ;; Send the heartbeat
  (gateway/send-payload conn heartbeat-opcode @heartbeat-atom)
  ;; Try to receive a heartbeat ack
  (when-not @(s/try-take! ack-stream false heartbeat-timeout false)
    ;; Failed, close connection and throw exception
    (s/close! conn)
    (throw (Exception. "Heartbeat ack timed out or stream take! error"))))

(defn- start-periodic-heartbeat
  [conn ack-stream heartbeat-atom interval]
    ;; A clock is required here as t/every and t/try-take! both use the same
    ;; clock (manifold's *clock*), and the clock is single-threaded. Without an
    ;; additional clock, it will deadlock.
    ;; Oddly enough, t/with-clock seems to only bind to t/every, so only one
    ;; thread is needed for the clock.
  (t/with-clock (create-heartbeat-clock 1)
    (t/every interval
             #(heartbeat conn ack-stream heartbeat-atom))))

(defn- add-heartbeat-responder
  [conn ack-stream heartbeat-stream heartbeat-atom]
  (s/connect-via heartbeat-stream
                 (fn [_] (do (heartbeat conn ack-stream heartbeat-atom)
                             (d/success-deferred true)))
                 conn))

(defn- add-heartbeat-updater
  [conn dispatch-stream heartbeat-atom]
  (s/connect-via dispatch-stream
                 #(do (update-heartbeat-atom! heartbeat-atom (% "s"))
                      (d/success-deferred true))
                 (s/stream)))

#_(defn start-heartbeat
  [conn event-bus heartbeat-atom]
  ;; Add periodic heartbeater
  (start-periodic-heartbeat conn event-bus
                            heartbeat-atom @(take-interval conn))
  ;; Add heartbeat responder
  (add-heartbeat-responder conn event-bus heartbeat-atom)
  ;; Add heartbeat incrementer
  (add-heartbeat-incrementer event-bus heartbeat-atom))
