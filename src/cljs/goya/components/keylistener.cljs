(ns goya.components.keylistener
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [goya.components.canvastools :as canvastools]
            [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [cljs.core.async :refer [put! chan <! alts!]]))


(def ONE-KEY 49)
(def TWO-KEY 50)
(def THREE-KEY 51)
(def FOUR-KEY 52)
(def FIVE-KEY 53)
(def SIX-KEY 54)
(def C-KEY 67)
(def G-KEY 71)
(def Z-KEY 90)
(def X-KEY 88)
(def Q-KEY 81)
(def W-KEY 87)
(def MINUS-KEY 189)
(def PLUS-KEY 187)


(def key-chan (chan))

(defn select-tool [app tool]
  (om/update! app [:tools :paint-tool] tool))


(defn handle-key-event [app event]
  (let [keyCode (.-keyCode event)
        metaKey (.-metaKey event)
        shiftKey (.-shiftKey event)
        handler (cond
                   (= keyCode G-KEY) #(canvastools/toggle-grid app)
                   (= keyCode W-KEY) #(canvastools/zoom-in app)
                   (= keyCode Q-KEY) #(canvastools/zoom-out app)
                   (= keyCode ONE-KEY) #(select-tool app :brush)
                   (= keyCode TWO-KEY) #(select-tool app :line)
                   (= keyCode THREE-KEY) #(select-tool app :box)
                   (= keyCode FOUR-KEY) #(select-tool app :fill)
                   (= keyCode FIVE-KEY) #(select-tool app :picker)
                   (= keyCode SIX-KEY) #(select-tool app :selection))]
    (when-not (= handler nil) (handler app))))


(defn key-listener-component [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [keychan (om/get-shared owner :keychan)]
        (go
          (while true
            (let [[v ch] (alts! [keychan])]
              (when (= ch keychan)
                (do (handle-key-event app v))))))))

    om/IRender
    (render [this]
      (omdom/div nil ""))))
