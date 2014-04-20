(ns goya.components.toolsmenu
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [cljs.core.async :refer [put! chan <! alts!]]))



(defn class-name-for-tool [current-paint-tool tool]
  (if (= current-paint-tool tool)
    (str "tools-menu-item" " " "tools-menu-item-selected")
    "tools-menu-item"))


(defn color-tool-component [app owner]
  (reify
    om/IRender
      (render [this]
        (omdom/div #js {:className "selected-color-indicator"
                        :style #js {:backgroundColor (:paint-color app)}}))))


(defn box-tool-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [toolchan]}]
        (let [current-paint-tool (:paint-tool app)
              class-name (class-name-for-tool current-paint-tool :box)]
        (omdom/div #js {:className class-name
                        :onClick (fn [e] (put! toolchan :box))}
          (omdom/i #js {:className "icon-edit"}))))))


(defn pencil-tool-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [toolchan]}]
        (let [current-paint-tool (:paint-tool app)
              class-name (class-name-for-tool current-paint-tool :pencil)]
        (omdom/div #js {:className class-name
                        :onClick (fn [e] (put! toolchan :pencil))}
          (omdom/i #js {:className "icon-pencil"}))))))


(defn fill-tool-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [toolchan]}]
        (let [current-paint-tool (:paint-tool app)
              class-name (class-name-for-tool current-paint-tool :fill)]
        (omdom/div #js {:className class-name
                        :onClick (fn [e] (put! toolchan :fill))}
          (omdom/i #js {:className "icon-bucket"}))))))


(defn picker-tool-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [toolchan]}]
        (let [current-paint-tool (:paint-tool app)
              class-name (class-name-for-tool current-paint-tool :picker)]
        (omdom/div #js {:className class-name
                        :onClick (fn [e] (put! toolchan :picker))}
          (omdom/i #js {:className "icon-pipette"}))))))


(defn selection-tool-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [toolchan]}]
        (let [current-paint-tool (:paint-tool app)
              class-name (class-name-for-tool current-paint-tool :selection)]
        (omdom/div #js {:className class-name
                        :onClick (fn [e] (put! toolchan :selection))}
          (omdom/i #js {:className "icon-move"}))))))



(defn tools-menu-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:toolchan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [toolchan (om/get-state owner :toolchan)]
        (go (while true
          (let [[v ch] (alts! [toolchan])]
            (when (= ch toolchan)
              (do
                (om/update! app [:paint-tool] v))))))))

    om/IRenderState
    (render-state [this {:keys [toolchan]}]
      (omdom/div #js {:className "tools-menu"}
        (om/build pencil-tool-component app {:init-state {:toolchan toolchan}})
        (om/build box-tool-component app {:init-state {:toolchan toolchan}})
        (om/build fill-tool-component app {:init-state {:toolchan toolchan}})
        (om/build picker-tool-component app {:init-state {:toolchan toolchan}})
        (om/build selection-tool-component app {:init-state {:toolchan toolchan}})
        (om/build color-tool-component app)))))
