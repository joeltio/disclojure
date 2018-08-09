(ns disclojure.gateway.heartbeat
  (:require [disclojure.gateway :as gateway]
            [manifold.time :as t]
            [manifold.stream :as s]))

;;; Constants
(def heartbeat-opcode 1)
(def heartbeat-timeout 1000)

(def ^:private create-heartbeat
  (partial gateway/create-payload heartbeat-opcode))

(defn is-heartbeat?
  [payload]
  (= (payload "op") heartbeat-opcode))

(defn- heartbeat-responder
  [heartbeat-atom]
  (comp (filter is-heartbeat?)
        (map (create-heartbeat @(heartbeat-atom)))))

(defn- heartbeat
  [json-conn seq]
  ;; Send heartbeat
  (gateway/send-payload json-conn heartbeat-opcode seq)
  ;; Receive heartbeat
  (when-not (s/try-take! json-conn false heartbeat-timeout false)
    ;; Failed, close connection and throw exception
    (s/close! json-conn)
    (throw (Exception. "Heartbeat ack timed out or stream take! error"))))

(defn start-periodic-heartbeat
  [json-conn interval]
  (let [heartbeat-seq (atom nil)]
    (t/every interval #(heartbeat json-conn @heartbeat-seq))
    atom))

(defn add-heartbeat-responder
  [json-conn heartbeat-atom]
  (let [responder (s/stream 0 heartbeat-responder)]
    (s/connect json-conn responder)
    (s/connect responder json-conn)))

(defn start-heartbeat
  "Shorthand function to start heartbeat functions after taking the interval"
  [json-conn]
  (->> (@(s/take json-conn) "heartbeat_interval")
       (start-heartbeat json-conn)
       (add-heartbeat-responder json-conn)))
