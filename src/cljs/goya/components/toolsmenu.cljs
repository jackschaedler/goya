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


(defn basic-tool-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [toolchan tool-name css-class]}]
        (let [current-paint-tool (:paint-tool app)
              class-name (class-name-for-tool current-paint-tool tool-name)]
        (omdom/div #js {:className class-name
                        :onClick (fn [e] (put! toolchan tool-name))}
          (omdom/i #js {:className css-class}))))))


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
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :brush
                                                         :css-class "icon-brush"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :line
                                                         :css-class "icon-pencil"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :box
                                                         :css-class "icon-edit"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :fill
                                                         :css-class "icon-bucket"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :picker
                                                         :css-class "icon-pipette"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :selection
                                                         :css-class "icon-move"}})))))
