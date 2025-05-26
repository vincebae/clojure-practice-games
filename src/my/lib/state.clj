(ns my.lib.state
  "Utility functions to manage game state
  Add these lines to the main namespace of the game for easier use:

  (ns ... 
    (require [my.lib.state :as st]))
  
  (defonce state (atom {}))

  (defmacro as! [ks v] `(sl/as! state ~ks ~v))
  (defmacro ms! [v] `(st/ms! state ~v))
  (defmacro us! [ks f & args] `(st/us! state ~ks ~f ~@args))
  (defmacro gs [ks] `(st/gs state ~ks))
  ")

(defn as!
  "Convenience function to assoc / assoc-in value to state"
  [state ks v]
  (if (coll? ks)
    (swap! state assoc-in ks v)
    (swap! state assoc ks v)))

(defn ms!
  "Convenience function to merge value to state"
  [state v]
  (swap! state merge v))

(defn us!
  "Convenience function to update / update-in value in state"
  [state ks f & args]
  (if (coll? ks)
    (apply swap! state update-in ks f args)
    (apply swap! state update ks f args)))

(defn gs
  "Convenience function to get / get-in value in state"
  [state ks]
  (if (coll? ks)
    (get-in @state ks)
    (get @state ks)))

