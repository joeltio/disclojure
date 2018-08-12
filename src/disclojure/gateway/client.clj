(ns disclojure.gateway.client
  "Creates a connected client to communicate with Discord API.
   A Client represents 1 websocket connection, or 1 shard connection (see
   Sharding [TODO]). This means that the client receives events from the
   Discord API gateway, such as message creation and member additions. As the
   Client represents only 1 shard conenction, it will only receive events that
   are given to its shard.

   A Client is represented using a hash map of the following values:
   ```
   {:conn conn
    :seq heartbeat-atom
    :event-bus event-bus
    :ready @(s/take! conn)}
   ```

   `conn` is the connection to the websocket manifold stream. The connection
   converts a hash map to string (when `s/put!`ing into the `conn`) and vice
   versa for `s/take!`.

   For example:
   ```
   ;;; Sending data
   ;; You can do this
   (s/put! conn {\"op\": 1 \"d\": nil})
   ;; Instead of
   (s/put! conn \"{\\\"op\\\": 1 \\\"d\\\": nil}\")

   ;;; Receiving data
   (s/take! conn) ;; Returns a hash map
   ```

   `seq` is an atom that contains the current sequence number. This sequence
   number is used to for heartbeating (see [Discord Heartbeating](https://discordapp.com/developers/docs/topics/gateway#heartbeating))
   and for resuming sessions. Generally, you do not have to bother about it as
   it is for internal use.

   `event-bus` is a manifold event bus that is connected to the gateway
   connection (`conn`). The gateway connection will publish to this event bus
   the data received from the gateway. The `topic` will be the opcode (see
   [Discord Opcodes](https://discordapp.com/developers/docs/topics/opcodes-and-status-codes))
   and the `message` will be the data received. For example, if the gateway
   receives the following:
   ```
   {
     \"op\": 1,
     \"d\": 3
   }
   ```
   The `topic` will be `1` and the `message` will be what the gateway has
   received:
   ```
   {
     \"op\": 1,
     \"d\": 3
   }
   ```

   `ready` is the ready payload received after identifying. See the [Discord
   Ready Payload](https://discordapp.com/developers/docs/topics/gateway#ready).

   This namespace provides functions to create a client and use the client to
   subscribe to events."
  (:require [disclojure.gateway :as gateway]
            [disclojure.http :as http]
            [disclojure.gateway.heartbeat :as heartbeat]
            [disclojure.gateway.identify :as identify]
            [manifold.stream :as s]
            [manifold.bus :as b]))

(defn- create-conn
  ([]
   (create-conn {}))
  ([options]
   (http/websocket-json-client @(gateway/get-cached-endpoint options))))

(defn create-client
  [bot-token options]
  (let [conn (create-conn)
        event-bus (b/event-bus)
        heartbeat-atom (atom nil)]
    ;; Link the connection to the event bus
    (s/connect-via conn
                   #(b/publish! event-bus (gateway/get-opcode %) %)
                   (s/stream))
    ;; Start heartbeating
    (heartbeat/start-heartbeat conn event-bus heartbeat-atom)
    ;; Identify client
    (identify/identify conn)
    {:conn conn
     :seq heartbeat-atom
     :event-bus event-bus
     :ready @(s/take! conn)}))
