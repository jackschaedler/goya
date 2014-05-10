(ns goya.components.canvas
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.canvasdrawing :as canvasdrawing]
            [cljs.core.async :refer [put! chan <! alts!]]))



;; =============================================================================

(defn refresh-pixels [app owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        width (get-in app [:main-app :canvas-width])
        height (get-in app [:main-app :canvas-height])
        pixels (get-in app [:main-app :image-data])
        zoom-factor (if (= dom-node-ref "main-canvas-ref")
                        (get-in app [:zoom-factor]) 2)]
  (canvasdrawing/draw-image-to-canvas pixels canvas width height zoom-factor)))


(defn refresh-grid [app owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        width (get-in app [:main-app :canvas-width])
        height (get-in app [:main-app :canvas-height])
        pixels (get-in app [:main-app :image-data])
        zoom-factor (if (= dom-node-ref "main-canvas-ref")
                        (get-in app [:zoom-factor]) 2)
        grid-on (get-in app [:tools :grid-on])]
  (when grid-on (canvasdrawing/draw-pixel-grid canvas width height zoom-factor))))



;; =============================================================================
;; Should be consolidated into a single component with different parameters

(defn canvas-minimap-component [app owner]
  (reify
    om/IDidUpdate
      (did-update [_ _ _]
         (refresh-pixels app owner "minimap-canvas-ref"))

    om/IDidMount
      (did-mount [_]
         (refresh-pixels app owner "minimap-canvas-ref"))

    om/IRender
      (render [this]
        (let [width (get-in app [:main-app :canvas-width])
              height (get-in app [:main-app :canvas-height])
              default-mini-canvas-zoom 2]
        (omdom/canvas
           #js {:id "minimap-canvas"
                :className "minimap-canvas-elem"
                :width (* default-mini-canvas-zoom width)
                :height (* default-mini-canvas-zoom height)
                :ref "minimap-canvas-ref"})))))


(defn main-canvas-component [app owner]
  (reify
    om/IDidUpdate
      (did-update [_ _ _]
         (refresh-pixels app owner "main-canvas-ref")
         (refresh-grid app owner "main-canvas-ref"))

    om/IDidMount
      (did-mount [_]
         (refresh-pixels app owner "main-canvas-ref")
         (refresh-grid app owner "main-canvas-ref"))

    om/IRender
      (render [this]
        (let [width (get-in app [:main-app :canvas-width])
              height (get-in app [:main-app :canvas-height])
              zoom-factor (get-in app [:zoom-factor])]
        (omdom/canvas
           #js {:id "main-canvas"
                :width (* zoom-factor width)
                :height (* zoom-factor height)
                :className "canvas"
                :ref "main-canvas-ref"})))))
