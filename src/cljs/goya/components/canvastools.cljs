(ns goya.components.canvastools
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [clojure.string :as string]
            [cljs.core.async :refer [put! chan <! alts!]]))


;; =============================================================================

(defn cursor-pos-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let [[x y] (:mouse-pos app)]
          (omdom/div nil
             (str x ", " y))))))


;; =============================================================================

(defn frame-num-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let [frame (get-in app [:main-app :editing-frame])
              num-frames (count (get-in app [:main-app :animation]))]
          (omdom/div nil
             (str "Frame " (inc frame) "/" num-frames))))))

;; =============================================================================

(defn toggle-grid [app]
  (om/transact! app [:tools :grid-on] not))

(defn grid-toggle-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let [is-active (get-in app [:tools :grid-on])
              class-name (if is-active "grid-enabled" "grid-disabled")]
         (omdom/div #js {:className class-name}
         (omdom/i #js {:className "icon-grid"
                       :onClick #(toggle-grid app)}))))))


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
            (str (get-in app [:zoom-factor]) "x"))))))


(defn string-to-dimensions [string]
  (let [tokens (string/split string " ")
        width (js/parseInt (tokens 0))
        height (js/parseInt (tokens 2))]
    [width height]))


(defn change-canvas-size [app owner]
  (let [canvas-size-chooser (om/get-node owner "canvas-size-chooser")
        selected-option (.-value canvas-size-chooser)
        [width height] (string-to-dimensions selected-option)
        total-num-pixels (* width height)
        new-image-data (vec (take total-num-pixels (repeat "#000000")))
        animation (get-in @app [:main-app :animation])
        new-animation [{:image-data new-image-data}]]
    (om/update! app [:main-app :canvas-width] width)
    (om/update! app [:main-app :canvas-height] height)
    (om/update! app [:main-app :animation] new-animation)
    (om/transact! app
                  [:main-app :undo-history]
                  #(conj % {:action (str "Resized Canvas to " selected-option) :icon "scissors"}) :add-to-undo)))


(defn canvas-dimensions-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let [canvas-width (get-in app [:main-app :canvas-width])
              canvas-height (get-in app [:main-app :canvas-height])
              current-value (str canvas-width " x " canvas-height)]
          (omdom/div
            #js {:className "canvas-info-text"}
            (omdom/span nil "Size:")
            (omdom/select #js {:className "canvas-size-select"
                               :onChange #(change-canvas-size app owner)
                               :value current-value
                               :ref "canvas-size-chooser"}
               (omdom/option #js {:value "64 x 64"} "64 x 64")
               (omdom/option #js {:value "32 x 32"} "32 x 32")
               (omdom/option #js {:value "24 x 24"} "24 x 24")
               (omdom/option #js {:value "16 x 16"} "16 x 16")))))))


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
