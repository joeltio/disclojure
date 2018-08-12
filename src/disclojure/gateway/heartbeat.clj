(ns disclojure.gateway.heartbeat
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
