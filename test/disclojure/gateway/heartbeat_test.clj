(ns disclojure.gateway.heartbeat-test
  (:require [clojure.test :refer :all]
            [disclojure.gateway.heartbeat :as heartbeat]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [manifold.bus :as b]))

(deftest take-interval-test
  (let [conn (s/stream)
        interval 1000]
    (s/put! conn {"d" {"heartbeat_interval" interval}})
    (let [interval-deferred (#'heartbeat/take-interval conn)]
      (is (d/deferred? interval-deferred))
      (is (= @interval-deferred interval)))))

(deftest periodic-heartbeat-test
  (let [rx (s/stream)
        tx (s/stream)
        conn (s/splice tx rx)
        event-bus (b/event-bus)
        heartbeat-atom (atom nil)
        interval 1000
        response-leeway 100]
    ;; Connect conn to event-bus
    (s/connect-via conn #(b/publish! event-bus (% "op") %) (s/stream))
    ;; Start periodic heartbeat and wait for interval
    (#'heartbeat/start-periodic-heartbeat conn event-bus heartbeat-atom interval)
    (testing "heartbeat every of the given interval"
      ;; Take heartbeat and send back an ack
      (is @(s/try-take! tx false response-leeway false))
      (s/put! rx {"op" 11})
      (Thread/sleep 1000)
      ;; Take heartbeat and send back an ack
      (is @(s/try-take! tx false response-leeway false))
      (s/put! rx {"op" 11})
      (Thread/sleep 1000))
    (testing "connection closes when no ack is received"
      (Thread/sleep (+ heartbeat/heartbeat-timeout response-leeway))
      (is (s/closed? conn)))))
