(ns goya.components.mainmenu
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.appstate :as app]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn dump-image []
  (.log js/console (str (get-in @app/app-state [:main-app :image-data]))))

(defn export-image []
  (let [canvas (. js/document (getElementById "main-canvas"))
        download-link (. js/document (getElementById "image-download-link"))]
    (set! (.-href download-link) (.toDataURL canvas))
    (.click download-link)
    (dump-image)))

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
          (recur))))))

    om/IRenderState
    (render-state [this {:keys [clickchan]}]
      (omdom/div nil
        (om/build menu-entry-component
          (get-in app [:main-menu-items :new-document])
          {:init-state {:clickchan clickchan}})
        (om/build menu-entry-component
          (get-in app [:main-menu-items :export-document])
          {:init-state {:clickchan clickchan}})))))
