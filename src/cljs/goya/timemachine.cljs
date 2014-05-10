(ns goya.timemachine
	(:require [goya.appstate :as app]
            [goya.previewstate :as previewstate]))


;; =============================================================================
;; Credits to David Nolen's Time Travel blog post.

(def app-history (atom [(get-in @app/app-state [:main-app])]))
(def app-future (atom []))


(defn forget-everything []
  (reset! app-future [])
  (reset! app-history []))

;; =============================================================================

(defn update-preview []
  (reset! previewstate/preview-state
          (assoc-in @previewstate/preview-state [:main-app :image-data]
                    (get-in @app/app-state [:main-app :image-data])))
  (reset! previewstate/preview-state
          (assoc-in @previewstate/preview-state [:main-app :canvas-width]
                    (get-in @app/app-state [:main-app :canvas-width])))
  (reset! previewstate/preview-state
          (assoc-in @previewstate/preview-state [:main-app :canvas-height]
                    (get-in @app/app-state [:main-app :canvas-height]))))


(defn show-history-preview [idx]
  (reset! previewstate/preview-state
          (assoc-in @previewstate/preview-state [:main-app :image-data]
                    (get-in (nth @app-history idx) [:image-data])))
  (reset! previewstate/preview-state
          (assoc-in @previewstate/preview-state [:main-app :canvas-width]
                    (get-in (nth @app-history idx) [:canvas-width])))
  (reset! previewstate/preview-state
          (assoc-in @previewstate/preview-state [:main-app :canvas-height]
                    (get-in (nth @app-history idx) [:canvas-height]))))


(add-watch app/app-state :preview-watcher
  (fn [_ _ _ _] (update-preview)))



(defn undo-is-possible []
  (> (count @app-history) 1))

(defn redo-is-possible []
  (> (count @app-future) 0))


(defn push-onto-undo-stack [new-state]
  (let [old-watchable-app-state (last @app-history)]
    (when-not (= old-watchable-app-state new-state)
      (swap! app-history conj new-state))))


(defn do-undo []
  (when (undo-is-possible)
    (swap! app-future conj (last @app-history))
    (swap! app-history pop)
    (reset! app/app-state (assoc-in @app/app-state [:main-app] (last @app-history)))))

(defn do-redo []
  (when (redo-is-possible)
    (reset! app/app-state (assoc-in @app/app-state [:main-app] (last @app-future)))
    (push-onto-undo-stack (last @app-future))
    (swap! app-future pop)))


(defn handle-transaction [tx-data root-cursor]
  (when (= (:tag tx-data) :add-to-undo)
    (reset! app-future [])
    (let [new-state (get-in (:new-state tx-data) [:main-app])]
      (push-onto-undo-stack new-state))))
