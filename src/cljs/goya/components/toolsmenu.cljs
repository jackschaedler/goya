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
                        :style #js {:backgroundColor (get-in app [:tools :paint-color])}}))))


(defn basic-tool-component [app owner]
  (reify
    om/IRenderState
      (render-state [this {:keys [toolchan tool-name css-class supports-erase-mode]}]
        (let [current-paint-tool (get-in app [:tools :paint-tool])
              class-name (class-name-for-tool current-paint-tool tool-name)
              show-erase-indicator (and supports-erase-mode (get-in app [:erase-mode]))]
        (omdom/div #js {:className class-name
                        :onClick (fn [e] (put! toolchan tool-name))}
          (omdom/i #js {:className css-class})
          (omdom/div #js {:className "erase-mode-indicator"}
            (if show-erase-indicator "E" "")))))))


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
                (om/update! app [:tools :paint-tool] v))))))))

    om/IRenderState
    (render-state [this {:keys [toolchan]}]
      (omdom/div #js {:className "tools-menu"}
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :brush
                                                         :supports-erase-mode true
                                                         :css-class "icon-brush"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :line
                                                         :supports-erase-mode true
                                                         :css-class "icon-pencil"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :box
                                                         :supports-erase-mode true
                                                         :css-class "icon-edit"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :fill
                                                         :supports-erase-mode true
                                                         :css-class "icon-bucket"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :picker
                                                         :supports-erase-mode false
                                                         :css-class "icon-pipette"}})
        (om/build basic-tool-component app {:init-state {:toolchan toolchan
                                                         :tool-name :selection
                                                         :supports-erase-mode false
                                                         :css-class "icon-move"}})))))
