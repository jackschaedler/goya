(ns goya.components.canvas
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.canvasdrawing :as canvasdrawing]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn get-current-pixels [app]
  (let [current-frame (get-in app [:main-app :editing-frame])
        animation (get-in app [:main-app :animation])
        frame (nth animation current-frame)
        pixels (:image-data frame)]
    pixels))

(defn set-current-pixels [app pixels]
  (let [animation (get-in @app [:main-app :animation])
        new-frame {:image-data pixels}
        current-frame (get-in @app [:main-app :editing-frame])
        new-animation (assoc animation current-frame new-frame)]
    (om/update! app [:main-app :animation] new-animation)))


;; =============================================================================

(defn refresh-pixels [app owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        background-color (get-in app [:main-app :background-color])
        width (get-in app [:main-app :canvas-width])
        height (get-in app [:main-app :canvas-height])
        pixels (get-current-pixels app)
        zoom-factor (if (= dom-node-ref "main-canvas-ref")
                        (get-in app [:zoom-factor]) 2)]
  (canvasdrawing/draw-image-to-canvas pixels canvas width height zoom-factor background-color)))


(defn refresh-onionskin [app owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        width (get-in app [:main-app :canvas-width])
        height (get-in app [:main-app :canvas-height])
        zoom-factor (if (= dom-node-ref "main-canvas-ref")
                        (get-in app [:zoom-factor]) 2)
        animation (get-in app [:main-app :animation])
        max-frame (dec (count animation))
        current-frame (get-in app [:main-app :editing-frame])
        previous-frame-index (max (dec current-frame) 0)
        previous-frame (nth animation previous-frame-index)
        previous-pixels (:image-data previous-frame)
        next-frame-index (min (inc current-frame) max-frame)
        next-frame (nth animation next-frame-index)
        next-pixels (:image-data next-frame)
        onion-skin-pref (:onion-skin app)
        show-prev-skin (or (= onion-skin-pref :prev) (= onion-skin-pref :both))
        show-next-skin (or (= onion-skin-pref :next) (= onion-skin-pref :both))]
  (when show-prev-skin
    (canvasdrawing/draw-onionskin-to-canvas previous-pixels canvas width height zoom-factor "rgba(255, 50, 80, 0.25)"))
  (when show-next-skin
    (canvasdrawing/draw-onionskin-to-canvas next-pixels canvas width height zoom-factor "rgba(255, 50, 80, 0.25)"))))


(defn refresh-grid [app owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        width (get-in app [:main-app :canvas-width])
        height (get-in app [:main-app :canvas-height])
        pixels (get-current-pixels app)
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
         (refresh-onionskin app owner "main-canvas-ref")
         (refresh-grid app owner "main-canvas-ref"))

    om/IDidMount
      (did-mount [_]
         (refresh-pixels app owner "main-canvas-ref")
         (refresh-onionskin app owner "main-canvas-ref")
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
