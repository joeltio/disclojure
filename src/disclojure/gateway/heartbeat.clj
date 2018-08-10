(ns disclojure.gateway.heartbeat
  (:require [disclojure.gateway :as gateway]
            [manifold.time :as t]
            [manifold.stream :as s]
            [manifold.deferred :as d]))

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
        (map (create-heartbeat @heartbeat-atom))))

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
    heartbeat-seq))

(defn add-heartbeat-responder
  [json-conn heartbeat-atom]
  (let [responder (s/stream 0 (heartbeat-responder heartbeat-atom))]
    (s/connect json-conn responder)
    (s/connect responder json-conn)))

(defn update-heartbeat-atom!
  [heartbeat-atom val]
  (swap! heartbeat-atom #(if (nil? %1) %2 (max %1 %2)) val))

(defn fake-connect-via
  "Fakes a stream connect-via; the callback will always return a deferred true"
  ([src f dst]
   (fake-connect-via src f dst nil))
  ([src f dst options]
   (s/connect-via src #(do (f %) (d/success-deferred true)) dst options)))

(defn add-heartbeat-incrementer
  [json-conn heartbeat-atom]
  (let [batch-filter (s/filter gateway/is-dispatch? json-conn)
        incrementer (s/stream)]
    ;; Do a fake connect to incrementer to act as a callback
    (fake-connect-via batch-filter
                      #(update-heartbeat-atom! heartbeat-atom (% "s"))
                      incrementer)))

(defn- take-interval
  [json-conn]
  (get-in @(s/take! json-conn) ["d" "heartbeat_interval"]))

(defn start-heartbeat
  "Shorthand function to start heartbeat functions after taking the interval"
  [json-conn]
  (let [interval (take-interval json-conn)
        heartbeat-atom (start-periodic-heartbeat json-conn interval)]
    (add-heartbeat-responder json-conn heartbeat-atom)
    (add-heartbeat-incrementer json-conn heartbeat-atom)
    heartbeat-atom))
