(ns goya.components.canvastools
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [cljs.core.async :refer [put! chan <! alts!]]))


;; =============================================================================

(defn cursor-pos-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let [[x y] (:mouse-pos app)]
          (omdom/div nil
             (str x ", " y))))))

(defn grid-toggle-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let [is-active (get-in app [:tools :grid-on])
              class-name (if is-active "grid-enabled" "grid-disabled")]
         (omdom/div #js {:className class-name}
         (omdom/i #js {:className "icon-grid"
                       :onClick (fn [e] (om/transact! app [:tools :grid-on] not))}))))))


;; =============================================================================

(defn zoom-in [app]
  (let [current-zoom-factor (:zoom-factor @app)
        new-desired-zoom-factor (inc current-zoom-factor)
        new-zoom-factor (min new-desired-zoom-factor 16)]
       (om/update! app [:zoom-factor] new-zoom-factor)))

(defn zoom-out [app]
  (let [current-zoom-factor (:zoom-factor @app)
        new-desired-zoom-factor (dec current-zoom-factor)
        new-zoom-factor (max new-desired-zoom-factor 2)]
       (om/update! app [:zoom-factor] new-zoom-factor)))



;; =============================================================================

(defn canvas-zoom-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [zoomchan]}]
        (omdom/div nil
          (omdom/div
            #js {:className "canvas-zoom-info"}
            (omdom/i #js {:className "icon-zoom-out"
                          :onClick #(put! zoomchan :zoom-out)})
            (omdom/i #js {:className "icon-zoom-in"
                          :onClick #(put! zoomchan :zoom-in)})
            (str (* 100 (get-in app [:zoom-factor])) "%"))))))


(defn canvas-dimensions-component [app owner]
  (reify
    om/IRender
      (render [this]
        (omdom/div
          #js {:className "canvas-info-text"}
          (str
            "Canvas: "
            (get-in app [:main-app :canvas-width])
            " x "
            (get-in app [:main-app :canvas-height]))))))


(defn canvas-info-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:zoomchan (chan)})

    om/IWillMount
    (will-mount [_]
       (let [zoomchan (om/get-state owner :zoomchan)]
        (go (loop []
          (let [e (<! zoomchan)]
            (when (= e :zoom-out) (zoom-out app))
            (when (= e :zoom-in) (zoom-in app))
          (recur))))))

    om/IRenderState
    (render-state [this {:keys [zoomchan]}]
      (omdom/div nil
        (om/build canvas-dimensions-component app)
        (om/build canvas-zoom-component app {:init-state {:zoomchan zoomchan}})))))
