(ns goya.components.drawing
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goog.events :as events]
            [goya.guistate :as guistate]
            [clojure.set :as sets]
            [cljs.core.async :refer [put! chan <! alts!]]))


;; =============================================================================
;; HERE BE DRAGONS
;; Needs a serious refactoring. You can tell I'm not speaking idiomatic
;; Clojure yet. Idiotmatic, perhaps.




;; =============================================================================
;; This is a bit of a hack. We hard code a canvas element into the HTML document
;; so we are able to draw the 'preview' of the stroke while the user is moving
;; the mouse quickly, and don't need to constantly re-render and mount a canvas
;; element on each call to render

(def preview-canvas-elem (. js/document (getElementById "preview-canvas")))


;; =============================================================================
;; it seems like it's too slow to use component local state for this. Hence,
;; it's living on its own in this external atom. Needs a second look.

(def visited-pixels (atom []))

(defn visit-pixel [index]
  (swap! visited-pixels conj index))

(defn forget-visited-pixels []
  (reset! visited-pixels []))

(defn assoc-all [v ks value]
  (reduce #(assoc %1 %2 value) v ks))

(defn commit-stroke [app]
  (let [paint-color (get-in @app [:tools :paint-color])
        old-image (get-in @app [:main-app :image-data])
        new-image (assoc-all old-image @visited-pixels paint-color)
        undo-list (get-in @app [:main-app :undo-history])
        paint-tool (get-in @app [:tools :paint-tool])]

    (om/update! app [:main-app :image-data] (vec new-image))

    (when (= paint-tool :pencil)
      (om/transact! app
                    [:main-app :undo-history]
                    #(conj % {:action (str "Painted Stroke") :icon "pencil"})
                    :add-to-undo))
    (when (= paint-tool :box)
      (om/transact! app
                    [:main-app :undo-history]
                    #(conj % {:action (str "Painted Rectangle") :icon "edit"})
                    :add-to-undo))
    (when (= paint-tool :fill)
      (om/transact! app
                    [:main-app :undo-history]
                    #(conj % {:action (str "Flood Filled") :icon "bucket"})
                    :add-to-undo))
    (forget-visited-pixels)))


;; =============================================================================

(defn preview-pix-coords [mouse-x mouse-y zoom-factor]
  (let [pixel-size zoom-factor
        x (quot mouse-x pixel-size)
        y (quot mouse-y pixel-size)]
    [(* x pixel-size) (* y pixel-size)]))

(defn screen-to-doc [x y zoom-factor]
  [(quot x zoom-factor) (quot y zoom-factor)])

(defn flatten-to-index [x y doc-width]
  (+ (* y doc-width) x))

(defn normalize-rect [orig-x orig-y nx ny]
    [(min orig-x (inc nx))
     (min orig-y (inc ny))
     (max orig-x (inc nx))
     (max orig-y (inc ny))])

(defn visit-pixels-for-rect-tool [doc-x doc-y]
  (let [[orig-x orig-y] (get-in @guistate/transient-state [:mouse-down-pos])
        [x y nx ny] (normalize-rect orig-x orig-y doc-x doc-y)]
    (doseq [dx (range x nx)]
      (doseq [dy (range y ny)]
        (visit-pixel (flatten-to-index dx dy 64))))))

(defn unpack-event [event]
  [(.-offsetX event) (.-offsetY event)])


;; =============================================================================
;; Pick color

(defn pick-color [app doc-x doc-y]
  (let [index (flatten-to-index doc-x doc-y 64)
        color (nth (get-in @app [:main-app :image-data]) index)]
    (om/update! app [:tools :paint-color] color)))


;; =============================================================================
;; The Ned Flanders function

(defn neighborinos [idx image-data color]
  (let [left   (dec idx)
        right  (inc idx)
        top    (- idx 64)
        bottom (+ idx 64)
        result [left right top bottom]
        valid-result (filter #(and (>= % 0) (< % 4096)) result)]
    (filter #(= (nth image-data %) color) valid-result)))


;; =============================================================================
;; Credits to:
;; http://stevelosh.com/blog/2012/10/caves-of-clojure-07-1/

(defn flood [idx image-data color]
  (loop [connected #{}
         to-connect #{idx}]
    (if (empty? to-connect)
      connected
      (let [current (first to-connect)
            connected (conj connected current)
            to-connect (disj to-connect current)
            candidates (set (neighborinos current image-data color))
            to-connect (sets/union to-connect (sets/difference candidates connected))]
        (recur connected to-connect)))))


(defn visit-pixels-for-fill-tool [doc-x doc-y image-data]
  (let [[orig-x orig-y] (get-in @guistate/transient-state [:mouse-down-pos])
        idx (flatten-to-index orig-x orig-y 64)
        color (nth image-data idx)
        fill-target (flood idx image-data color)]
    (doseq [i fill-target]
      (visit-pixel i))))


;; =============================================================================

(defn paint-canvas-mouse-pos [app event]
(let [[x y] (unpack-event event)
      zoom-factor (get-in @app [:zoom-factor])
      [doc-x doc-y] (screen-to-doc x y zoom-factor)
      pixel-size zoom-factor
      doc-canvas-width (get-in @app [:main-app :canvas-width])
      doc-canvas-height (get-in @app [:main-app :canvas-height])
      doc-index (flatten-to-index doc-x doc-y doc-canvas-width)
      paint-color (get-in @app [:tools :paint-color])
      paint-tool (get-in @app [:tools :paint-tool])
      preview-context (.getContext preview-canvas-elem "2d")
      [mouse-down-x mouse-down-y] (get-in @guistate/transient-state [:mouse-down-pos])]
  (when (= paint-tool :pencil)
    (set! (.-fillStyle preview-context) paint-color)
    (.fillRect preview-context (* doc-x zoom-factor) (* doc-y zoom-factor) pixel-size pixel-size)
    (visit-pixel doc-index))

  (when (= paint-tool :box)
    (set! (.-width preview-canvas-elem) (.-width preview-canvas-elem))
    (set! (.-fillStyle preview-context) paint-color)

    (let [adjusted-doc-x (inc doc-x)
          adjusted-doc-y (inc doc-y)]
      (.fillRect preview-context
         (* mouse-down-x zoom-factor)
         (* mouse-down-y zoom-factor)
         (* (- adjusted-doc-x mouse-down-x) zoom-factor)
         (* (- adjusted-doc-y mouse-down-y) zoom-factor))))))


;; =============================================================================

(defn canvas-painting-component [app owner]
  (reify
    om/IInitState
      (init-state [_]
        {:mouse-chan (chan)})

    om/IWillMount
    (will-mount [_]
       (let [mouse-chan (om/get-state owner :mouse-chan)]
        (go
          (loop []
            (let [e (<! mouse-chan)
                  event-type (.-type e)
                  [x y] (unpack-event e)
                  zoom-factor (get-in @app [:zoom-factor])
                  [doc-x doc-y] (screen-to-doc x y zoom-factor)
                  paint-tool (get-in @app [:tools :paint-tool])]

                  (when (= event-type "mousedown")
                    (reset! guistate/transient-state
                            (assoc @guistate/transient-state :user-is-drawing true))
                    (reset! guistate/transient-state
                            (assoc @guistate/transient-state :mouse-down-pos [doc-x doc-y]))
                    (paint-canvas-mouse-pos app e))

                  (when (= event-type "mouseup")
                    (reset! guistate/transient-state
                            (assoc @guistate/transient-state :user-is-drawing false))
                    (when (= paint-tool :box)
                      (visit-pixels-for-rect-tool doc-x doc-y))
                    (when (= paint-tool :fill)
                      (visit-pixels-for-fill-tool doc-x doc-y (get-in @app [:main-app :image-data])))
                    (when (= paint-tool :picker)
                      (pick-color app doc-x doc-y))
                    (commit-stroke app))

                  (when (and (= event-type "mousemove") (:user-is-drawing @guistate/transient-state))
                    (paint-canvas-mouse-pos app e))

                  (when (= event-type "mousemove")
                    (reset! guistate/transient-state (assoc @guistate/transient-state :mouse-pos [doc-x doc-y])))

          (recur))))))

    ;; =============================================================================
    ;; I shouldn't have to listen to the events like this, but if I simply setup
    ;; the listeners in the render function, I get bogus events

    om/IDidMount
      (did-mount [_]
        (let [painter-watcher (om/get-node owner "painter-watcher-ref")
              mouse-chan (om/get-state owner :mouse-chan)]
          (events/listen painter-watcher "mousemove" #(put! mouse-chan %))
          (events/listen painter-watcher "mousedown" #(put! mouse-chan %))
          (events/listen painter-watcher "mouseup" #(put! mouse-chan %))))

    om/IRenderState
      (render-state [this {:keys [mouse-chan]}]
        (let [doc-canvas-width (get-in app [:main-app :canvas-width])
              doc-canvas-height (get-in app [:main-app :canvas-height])
              zoom-factor (get-in app [:zoom-factor])
              screen-canvas-width (* doc-canvas-width zoom-factor)
              screen-canvas-height (* doc-canvas-height zoom-factor)]
        (set! (.-width preview-canvas-elem) screen-canvas-width)
        (set! (.-height preview-canvas-elem) screen-canvas-height)
        (omdom/div #js {:id "painter-watcher"
                        :style #js {:width screen-canvas-width
                                    :height screen-canvas-height}
                        :ref "painter-watcher-ref"})))))
