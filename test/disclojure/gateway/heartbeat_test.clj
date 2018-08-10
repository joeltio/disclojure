(ns disclojure.gateway.heartbeat-test
  (:require [clojure.test :refer :all]
            [disclojure.gateway.heartbeat :as heartbeat]
            [manifold.stream :as s]))

(defn heartbeat-payload-test
  [pred normal? ack? other?]
  (testing "heartbeat payload"
    (let [payload {"op" heartbeat/heartbeat-opcode}]
      (is (= (pred payload) normal?))))
  (testing "heartbeat ack payload"
    (let [payload {"op" heartbeat/heartbeat-ack-opcode}]
      (is (= (pred payload) ack?))))
  (testing "other payload"
    (let [payload {"op" 0}]
      (is (= (pred payload) other?)))))

(deftest is-heartbeat?-test
  (heartbeat-payload-test heartbeat/is-heartbeat? true false false))

(deftest is-heartbeat-ack?-test
  (heartbeat-payload-test heartbeat/is-heartbeat-ack? false true false))

(deftest is-heartbeat-related?-test
  (heartbeat-payload-test heartbeat/is-heartbeat-related? true true false))

(deftest periodic-heartbeat-test
  (testing "heartbeat every interval"
    (let [rx (s/stream)
          tx (s/stream)
          conn (s/splice tx rx)]
      ;; Start periodic heartbeat and wait for interval
      (heartbeat/start-periodic-heartbeat conn 1000)
      ;; Try to take the heartbeat
      (is @(s/try-take! tx false 100 false))
      (s/put! rx {"op" 11})
      (Thread/sleep 1000)
      (is @(s/try-take! tx false 100 false))
      (s/close! conn))))
