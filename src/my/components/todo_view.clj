(ns my.components.todo-view
  (:require [my.components.event-handler :as eh]))

(defn todo-view
  [{:keys [text id done]}]
  {:fx/type :h-box
   :spacing 5
   :padding 5
   :children [{:fx/type :check-box
               :selected done
               :on-selected-changed {:event/type :set-done :id id}}
              {:fx/type :label
               :style {:-fx-text-fill (if done :grey :black)}
               :text text}]})

; (defmethod event-handler ::set-done
;   [event]
;   (swap! *state assoc-in [:by-id (:id event) :done] (:fx/event event)))


