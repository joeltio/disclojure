(ns disclojure.gateway.client-test
  "Tests for the gateway client.
   For more information about what the gateway client does, see the namespace
   documentation for the gateway client.

   As the client is a cumulation of other components such as heartbeating and
   identifying (see gateway.heartbeat and gateway.identify), the tests in this
   namespace are black box, meaning, only the inputs and outputs of the client
   are tested.

   This namespace's tests will connect to the Discord API. Hence, it may exceed
   the Discord rate limits (1 identify in 5 seconds, 1000 identifies over 24
   hours)."
  (:require [clojure.test :refer :all]
            [disclojure.test :refer :all]
            [manifold.stream :as s]
            [disclojure.gateway.client :as client]))

(deftest create-conn-test
  (let [conn @(#'client/create-conn)]
    ;; Check that the connection receives the hello payload on connection
    (testing "connection receives hello"
      (let [payload @(s/try-take! conn false 1000 false)]
        (is (contains? payload "d"))))))

(deftest create-client-test
  (let [c (client/create-client (bot-token))]
    ;; The seq should be one; the ready gives the first sequence
    (testing "ready gives the first sequence"
      (is (= @(c :seq) 1)))
    ;; The connections should be a manifold stream
    (testing "connection is manifold stream"
      (is (s/stream? (c :conn))))
    ;; Check for the ready payload by checking if there is a "session_id" in
    ;; the payload (only the ready payload has the session id).
    (testing "ready payload has session id"
      (is (not (nil? (get-in c [:ready "session_id"])))))))
