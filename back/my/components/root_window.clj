(ns my.components.root-window
  (:require [my.components.title-input :refer [title-input]]
            [my.components.todo-view :refer [todo-view]]))

(defn root-window
  [{:keys [title]}]
  {:fx/type :stage
   :showing true
   :title "Hello, World!"
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [{:fx/type :label
                              :text "Window title input"}
                             {:fx/type title-input
                              :title title}
                             {:fx/type todo-view
                              :text "Todo"
                              :id "todo"
                              :done false}]}}})


