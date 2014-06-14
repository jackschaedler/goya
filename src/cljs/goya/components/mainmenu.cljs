(ns goya.components.mainmenu
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [goog.events :as events]
            [goog.dom :as dom]
            [cljs.reader :as reader]
            [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.appstate :as app]
            [goya.timemachine :as timemachine]
            [goya.canvasdrawing :as canvasdrawing]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn file-list-to-cljs [js-col]
  (-> (clj->js [])
      (.-slice)
      (.call js-col)
      (js->clj)))

(defn reset-app-state-from-string [document-uri]
  (let [compressed-app-state (aget (.split document-uri ",") 1)
        decompressed-content (.decompressFromBase64 js/LZString compressed-app-state)
        document (reader/read-string decompressed-content)
        dummy-undo-history [{:action "Loaded Saved Project" :icon "upload"}]
        cleaned-document (assoc-in document [:undo-history] dummy-undo-history)
        current-app-state @app/app-state
        new-app-state (assoc-in current-app-state [:main-app] cleaned-document)]
    (reset! app/app-state new-app-state)
    (timemachine/forget-everything)))

(defn handle-file-load [e]
  (let [files (.-files (.-target e))
        files-list (file-list-to-cljs files)
        file (nth files-list 0)
        file-reader (js/FileReader.)]
    (set! (.-onload file-reader)
      #(reset-app-state-from-string (.-result (.-target %))))
    (.readAsDataURL file-reader file)))

(events/listen
  (dom/getElement "fileChooser")
  "change"
  #(handle-file-load %))


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

(defn export-spritesheet []
  (let [render-canvas (. js/document (createElement "canvas"))
        animation (get-in @app/app-state [:main-app :animation])
        frame-width (get-in @app/app-state [:main-app :canvas-width])
        frame-height (get-in @app/app-state [:main-app :canvas-height])
        download-link (. js/document (getElementById "image-download-link"))]
    (canvasdrawing/draw-spritesheet-to-canvas animation render-canvas frame-width frame-height)
    (set! (.-href download-link) (.toDataURL render-canvas))
    (.click download-link)))

(defn save-document []
  (let [download-link (. js/document (getElementById "document-download-link"))
        app-state-to-save (get-in @app/app-state [:main-app])
        document-content (pr-str app-state-to-save)
        compressed-content (.compressToBase64 js/LZString document-content)
        href-content (str "data:application/octet-stream;base64," compressed-content)]
    (set! (.-href download-link) href-content)
    (.click download-link)))


(defn load-document []
  (let [upload-link (. js/document (getElementById "fileChooser"))]
    (.click upload-link)))


(defn download-history-animation [blob owner]
  (let [download-link (. js/document (getElementById "image-download-link"))]
    (om/set-state! owner :is-processing false)
    (set! (.-href download-link) (.createObjectURL js/URL blob))
    (.click download-link)))

;; (defn export-history-animation [owner]
;;   (let [render-canvas (. js/document (createElement "canvas"))
;;         max-width 64
;;         max-height 64
;;         zoom-factor 2
;;         app-history @timemachine/app-history
;;         gif (js/GIF. #js {:workers 2
;;                           :quality 10
;;                           :width (* max-width zoom-factor)
;;                           :height (* max-height zoom-factor)
;;                           :workerScript "./gifjs/dist/gif.worker.js"})]
;;     (om/set-state! owner :is-processing true)
;;     (om/set-state! owner :progress 0)
;;     (dotimes [x (count app-history)]
;;       (let [history-snapshot (nth app-history x)
;;             width (get-in history-snapshot [:canvas-width])
;;             height (get-in history-snapshot [:canvas-height])
;;             image-data (get-in history-snapshot [:image-data])
;;             context (.getContext render-canvas "2d")]
;;         (canvasdrawing/draw-image-to-canvas image-data render-canvas width height zoom-factor)
;;         (.addFrame gif context #js {:copy true :delay 300})))
;;     (.on gif "finished" #(download-history-animation % owner))
;;     (.on gif "progress" #(show-progress % owner))
;;     (.render gif)))


(defn export-animation [owner]
  (let [render-canvas (. js/document (createElement "canvas"))
        width (get-in @app/app-state [:main-app :canvas-width])
        height (get-in @app/app-state [:main-app :canvas-height])
        animation (get-in @app/app-state [:main-app :animation])
        gif (js/GIF. #js {:workers 4
                          :quality 1
                          :width width
                          :height height
                          :workerScript "./gifjs/dist/gif.worker.js"})]
    (om/set-state! owner :is-processing true)
    (om/set-state! owner :progress 0)
    (dotimes [x (count animation)]
      (let [frame (nth animation x)
            image-data (get-in frame [:image-data])
            context (.getContext render-canvas "2d")]
        (canvasdrawing/draw-image-to-canvas image-data render-canvas width height 1)
        (.addFrame gif context #js {:copy true :delay 300})))
    (.on gif "finished" #(download-history-animation % owner))
    (.on gif "progress" #(show-progress % owner))
    (.render gif)))



(defn assoc-all [v ks value]
  (reduce #(assoc %1 %2 value) v ks))


(defn create-new-document [app owner]
  (let [width (get-in @app [:main-app :canvas-width])
        height (get-in @app [:main-app :canvas-height])
        num-pixels (* width height)
        blank-frame {:image-data (vec (take num-pixels (repeat "#000000")))}
        new-animation [blank-frame]]
    (timemachine/forget-everything)
    (om/update! app [:main-app :editing-frame] 0)
    (om/update! app [:main-app :animation] new-animation :new-document)
    (om/update! app [:main-app :undo-history]
                    [{:action (str "Started New Project") :icon "tag"}] :add-to-undo)))


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
                                                 :owner owner})
                          (when (= (:command @entry) :load-doc) (load-document)))}
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
            (when (= command :save-doc) (save-document))
            (when (= command :export-doc) (export-image))
            (when (= command :export-spritesheet) (export-spritesheet))
            (when (= command :export-animation) (export-animation origin-owner))
;;             (when (= command :export-history-animation) (export-history-animation origin-owner))
          (recur))))))

    om/IRenderState
    (render-state [this {:keys [clickchan]}]
      (omdom/div nil
        (om/build menu-entry-component
          (get-in app [:main-menu-items :new-document])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :save-document])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :load-document])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :export-document])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :export-document-spritesheet])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :export-document-animation])
          {:init-state {:clickchan clickchan}})
;;         (om/build menu-entry-component
;;           (get-in app [:main-menu-items :export-history-animation])
;;           {:init-state {:clickchan clickchan}})
        ))))
