(ns goya.guistate)


;; =============================================================================
;; it seems like it's too slow to use component local state for this. Hence,
;; it's living on its own in this external atom. Needs a closer look.

(def transient-state
  (atom
    {:user-is-drawing false
     :mouse-pos [0 0]
     :mouse-down-pos [0 0]
     :last-mouse-pos []}))
