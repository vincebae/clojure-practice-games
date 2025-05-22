(ns my.components.event-handler)

(def *state (atom {:title "App title"}))

; (defmulti event-handler :event/type)
;
; (defmethod event-handler ::title-change
;   [event]
;   (swap! *state assoc :title (:fx/event event)))
;
; (defmethod event-handler ::set-done
;   [event]
;   (swap! *state assoc-in [:by-id (:id event) :done] (:fx/event event)))

(defn event-handler
  [event]
  (println "in event-handler")
  (case (:event/type event)
    :title-change (swap! *state assoc :title (:fx/event event))
    :set-done (swap! *state assoc-in [:by-id (:id event) :done] (:fx/event event))))

(defn update!
  [fn & args]
  (apply swap! *state fn args))
