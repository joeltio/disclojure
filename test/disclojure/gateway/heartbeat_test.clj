(ns disclojure.gateway.heartbeat-test
  (:require [clojure.test :refer :all]
            [disclojure.gateway.heartbeat :as heartbeat]
            [manifold.deferred :as d]
            [manifold.stream :as s]))

(deftest take-interval-test
  (let [conn (s/stream)
        interval 1000]
    (s/put! conn {"d" {"heartbeat_interval" interval}})
    (let [interval-deferred (#'heartbeat/take-interval conn)]
      (is (d/deferred? interval-deferred))
      (is (= @interval-deferred interval)))))
