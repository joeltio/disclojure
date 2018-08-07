(ns disclojure.presence)

;;; Constants
;; Activity types
(def activity-playing-id 0)
(def activity-streaming-id 1)
(def activity-listening-id 2)

;; User status types
(def user-status-online "online")
(def user-status-dnd "dnd")
(def user-status-idle "idle")
(def user-status-invis "invisible")
(def user-status-offline "offline")

;;; Creating activity
(defn create-activity
  ([name type]
   (create-activity name type nil))
  ([name type options]
   (merge {:name name
           :type type}
          options)))

(defn create-presence
  [since activity status afk?]
  {:since since
   :game activity
   :status status
   :afk afk?})
