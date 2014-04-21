(ns goya.components.mainmenu
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.appstate :as app]
            [goya.timemachine :as timemachine]
            [goya.canvasdrawing :as canvasdrawing]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn dump-image []
  (.log js/console (str (get-in @app/app-state [:main-app :image-data]))))

(defn export-image []
  (let [canvas (. js/document (getElementById "main-canvas"))
        download-link (. js/document (getElementById "image-download-link"))]
    (set! (.-href download-link) (.toDataURL canvas))
    (.click download-link)
    (dump-image)))


(defn download-history-animation [blob]
  (let [download-link (. js/document (getElementById "image-download-link"))]
    (println "done!")
    (set! (.-href download-link) (.createObjectURL js/URL blob))
    (.click download-link)))

(defn export-history-animation []
  (let [render-canvas (. js/document (createElement "canvas"))
        width (get-in @app/app-state [:main-app :canvas-width])
        height (get-in @app/app-state [:main-app :canvas-height])
        zoom-factor (get-in @app/app-state [:zoom-factor])
        app-history @timemachine/app-history
        gif (js/GIF. #js {:workers 2
                          :quality 10
                          :width (* width zoom-factor)
                          :height (* height zoom-factor)
                          :workerScript "./gifjs/dist/gif.worker.js"})]
    (dotimes [x (count app-history)]
      (let [history-snapshot (nth app-history x)
            image-data (get-in history-snapshot [:image-data])
            context (.getContext render-canvas "2d")]
        (canvasdrawing/draw-image-to-canvas image-data render-canvas width height zoom-factor)
        (.addFrame gif context #js {:copy true :delay 300})))
    (.on gif "finished" #(download-history-animation %))
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
    om/IRenderState
      (render-state [this {:keys [clickchan]}]
        (omdom/div
          #js {:className "main-menu-item"
               :onClick (fn [e] (put! clickchan (:command @entry)))}
          (omdom/i #js {:className (:icon entry)})
          (:text entry)))))


(defn menu-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:clickchan (chan)})

    om/IWillMount
    (will-mount [_]
       (let [clickchan (om/get-state owner :clickchan)]
        (go (loop []
          (let [e (<! clickchan)]
            (when (= e :new-doc) (create-new-document app owner))
            (when (= e :export-doc) (export-image) (dump-image))
            (when (= e :export-history-animation) (export-history-animation))
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
