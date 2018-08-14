(ns disclojure.gateway.heartbeat-test
  (:require [clojure.test :refer :all]
            [disclojure.gateway.heartbeat :as heartbeat]
            [disclojure.gateway :as gateway]
            [manifold.deferred :as d]
            [manifold.executor :as ex]
            [manifold.stream :as s]))

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
        heartbeat-atom (atom nil)
        heartbeat-ack-stream (#'heartbeat/heartbeat-ack-stream conn)
        interval 1000
        response-leeway 100]
    ;; Start periodic heartbeat and wait for interval
    (#'heartbeat/start-periodic-heartbeat conn heartbeat-ack-stream
                                          heartbeat-atom interval)
    (testing "heartbeat every of the given interval"
      ;; Take heartbeat and send back an ack
      (is @(s/try-take! tx false response-leeway false))
      (s/put! rx {"op" heartbeat/heartbeat-ack-opcode})
      (Thread/sleep 1000)
      ;; Take heartbeat and send back an ack
      (is @(s/try-take! tx false response-leeway false))
      (s/put! rx {"op" heartbeat/heartbeat-ack-opcode})
      (Thread/sleep 1000))
    (testing "connection closes when no ack is received"
      (Thread/sleep (+ heartbeat/heartbeat-timeout response-leeway))
      (is (s/closed? conn)))))

(deftest heartbeat-responder-test
  (let [rx (s/stream)
        tx (s/stream)
        conn (s/splice tx rx)
        heartbeat-atom (atom nil)
        executor (ex/fixed-thread-executor 1)
        heartbeat-stream (s/onto executor (#'heartbeat/heartbeat-stream conn))
        heartbeat-ack-stream (#'heartbeat/heartbeat-ack-stream conn)
        response-leeway 100]
    ;; Add heartbeat responder
    (#'heartbeat/add-heartbeat-responder conn heartbeat-ack-stream
                                         heartbeat-stream heartbeat-atom)
    ;; Request for heartbeat
    (testing "reply to heartbeat request"
      (s/put! rx {"op" heartbeat/heartbeat-opcode})
      (is (= @(s/try-take! tx false response-leeway false)
              {:op 1 :d nil}))
      (s/put! rx {"op" heartbeat/heartbeat-ack-opcode}))
    ;; Send something irrelevant
    (testing "do not reply to non-heartbeat requests"
      (s/put! rx {"op" heartbeat/heartbeat-ack-opcode})
      (is (not @(s/try-take! tx false response-leeway false)))
      ;; Clear the payload so that the stream can continue receiving
      (s/take! heartbeat-ack-stream))
    ;; Change heartbeat atom and test
    (testing "reply with updated heartbeat atom"
      (swap! heartbeat-atom #(if (nil? %) 1 (inc %)))
      (s/put! rx {"op" heartbeat/heartbeat-opcode})
      (is (= @(s/try-take! tx false response-leeway false)
             {:op heartbeat/heartbeat-opcode :d @heartbeat-atom}))
      (s/put! rx {"op" heartbeat/heartbeat-ack-opcode}))))

(deftest heartbeat-updater-test
  (let [rx (s/stream)
        tx (s/stream)
        conn (s/splice tx rx)
        heartbeat-atom (atom nil)
        dispatch-stream (gateway/dispatch-stream conn)
        response-leeway 100]
    ;; Add heartbeat incrementer
    (#'heartbeat/add-heartbeat-updater conn dispatch-stream heartbeat-atom)
    (testing "sending dispatch increments atom"
      (s/put! rx {"op" gateway/dispatch-opcode "s" 0})
      (Thread/sleep response-leeway)
      (is (= @heartbeat-atom 0)))
    (testing "incrementer takes largest sequence number"
      (s/put! rx {"op" gateway/dispatch-opcode "s" 1})
      (Thread/sleep response-leeway)
      (is (= @heartbeat-atom 1))
      (s/put! rx {"op" gateway/dispatch-opcode "s" 0})
      (Thread/sleep response-leeway)
      (is (= @heartbeat-atom 1)))))
