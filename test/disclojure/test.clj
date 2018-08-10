(ns disclojure.test
  "Provides additional utilities for testing."
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [manifold.stream :as s]))

(def ^:private bot-token-path "bot_token.txt")

(defn bot-token
  "Retrieves the bot token from the file defined in bot-token-path"
  []
  (slurp (io/resource bot-token-path)))
