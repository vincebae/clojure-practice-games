(ns my.components.title-input
  (:require [my.components.event-handler :refer [event-handler *state]]))

(defn title-input
  [{:keys [title]}]
  {:fx/type :text-field
   :on-text-changed {:event/type :title-change}
   :text title})

; (defmethod event-handler ::title-change
;   [event]
;   (swap! *state assoc :title (:fx/event event)))


