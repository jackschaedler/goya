(ns goya.components.mainmenu
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.appstate :as app]
            [goya.timemachine :as timemachine]
            [goya.canvasdrawing :as canvasdrawing]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn show-progress [progress owner]
  (let [p (.round js/Math (* progress 100))]
    (om/set-state! owner :progress p)))

(defn dump-image []
  (.log js/console (str (get-in @app/app-state [:main-app :image-data]))))

(defn export-image []
  (let [canvas (. js/document (getElementById "main-canvas"))
        download-link (. js/document (getElementById "image-download-link"))]
    (set! (.-href download-link) (.toDataURL canvas))
    (.click download-link)))


(defn download-history-animation [blob owner]
  (let [download-link (. js/document (getElementById "image-download-link"))]
    (om/set-state! owner :is-processing false)
    (set! (.-href download-link) (.createObjectURL js/URL blob))
    (.click download-link)))

(defn export-history-animation [owner]
  (let [render-canvas (. js/document (createElement "canvas"))
        max-width 64
        max-height 64
        zoom-factor 2
        app-history @timemachine/app-history
        gif (js/GIF. #js {:workers 2
                          :quality 10
                          :width (* max-width zoom-factor)
                          :height (* max-height zoom-factor)
                          :workerScript "./gifjs/dist/gif.worker.js"})]
    (om/set-state! owner :is-processing true)
    (om/set-state! owner :progress 0)
    (dotimes [x (count app-history)]
      (let [history-snapshot (nth app-history x)
            width (get-in history-snapshot [:canvas-width])
            height (get-in history-snapshot [:canvas-height])
            image-data (get-in history-snapshot [:image-data])
            context (.getContext render-canvas "2d")]
        (canvasdrawing/draw-image-to-canvas image-data render-canvas width height zoom-factor)
        (.addFrame gif context #js {:copy true :delay 300})))
    (.on gif "finished" #(download-history-animation % owner))
    (.on gif "progress" #(show-progress % owner))
    (.render gif)))


(defn assoc-all [v ks value]
  (reduce #(assoc %1 %2 value) v ks))


(defn create-new-document [app owner]
  (let [paint-color (get-in @app [:tools :paint-color])
        old-image (get-in @app [:main-app :image-data])
        ;; Hack- need to get proper size instead of hard coded number
        new-image (assoc-all old-image (range 4096) paint-color)]
    (om/update! app [:main-app :image-data] new-image :new-document)
    (om/transact! app
                  [:main-app :undo-history]
                  #(conj % {:action (str "Primed Canvas") :icon "doc-inv"}) :add-to-undo)))


;; =============================================================================

(defn menu-entry-component [entry owner]
  (reify
    om/IInitState
      (init-state [_]
        {:is-processing false
         :progress 0})

    om/IRenderState
      (render-state [this {:keys [clickchan]}]
        (omdom/div
          #js {:className "main-menu-item"
               :onClick (fn [e] (put! clickchan {:command (:command @entry)
                                                 :owner owner}))}
          (omdom/i #js {:className (:icon entry)})
          (if
            (om/get-state owner :is-processing)
            (str "Working... "(om/get-state owner :progress) "%")
            (:text entry))))))


(defn menu-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:clickchan (chan)})

    om/IWillMount
    (will-mount [_]
       (let [clickchan (om/get-state owner :clickchan)]
        (go (loop []
          (let [e (<! clickchan)
                command (:command e)
                origin-owner (:owner e)]
            (when (= command :new-doc) (create-new-document app owner))
            (when (= command :export-doc) (export-image) (dump-image))
            (when (= command :export-history-animation) (export-history-animation origin-owner))
          (recur))))))

    om/IRenderState
    (render-state [this {:keys [clickchan]}]
      (omdom/div nil
        (om/build menu-entry-component
          (get-in app [:main-menu-items :new-document])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :export-document])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :export-history-animation])
          {:init-state {:clickchan clickchan}})))))
