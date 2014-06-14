(ns goya.components.keylistener
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [goya.components.canvastools :as canvastools]
            [goya.components.animation :as animation]
            [goya.timemachine :as timemachine]
            [goya.globalcommands :as globalcommands]
            [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [cljs.core.async :refer [put! chan <! alts!]]))


(def ONE-KEY 49)
(def TWO-KEY 50)
(def THREE-KEY 51)
(def FOUR-KEY 52)
(def FIVE-KEY 53)
(def SIX-KEY 54)
(def A-KEY 65)
(def D-KEY 68)
(def E-KEY 69)
(def C-KEY 67)
(def G-KEY 71)
(def Z-KEY 90)
(def X-KEY 88)
(def Q-KEY 81)
(def V-KEY 86)
(def W-KEY 87)
(def O-KEY 79)
(def MINUS-KEY 189)
(def PLUS-KEY 187)
(def LEFT-ARROW-KEY 37)
(def RIGHT-ARROW-KEY 39)
(def BACKSPACE-KEY 8)


(def key-chan (chan))

(defn select-tool [app tool]
  (om/update! app [:tools :paint-tool] tool))

(defn toggle-erase-mode [app]
  (om/transact! app [:erase-mode] not))


(defn handle-key-event [app event]
  (let [keyCode (.-keyCode event)
        metaKey (.-metaKey event)
        shiftKey (.-shiftKey event)
        ctrlKey (.-ctrlKey event)
        handler (cond
                   (= keyCode G-KEY) #(canvastools/toggle-grid app)
                   (= keyCode W-KEY) #(canvastools/zoom-in app)
                   (= keyCode Q-KEY) #(canvastools/zoom-out app)
                   (= keyCode O-KEY) #(animation/toggle-onion-skin app)
                   (= keyCode D-KEY) #(animation/delete-current-frame app)
                   (= keyCode E-KEY) #(toggle-erase-mode app)
                   (= keyCode ONE-KEY) #(select-tool app :brush)
                   (= keyCode TWO-KEY) #(select-tool app :line)
                   (= keyCode THREE-KEY) #(select-tool app :box)
                   (= keyCode FOUR-KEY) #(select-tool app :fill)
                   (= keyCode FIVE-KEY) #(select-tool app :picker)
                   (= keyCode SIX-KEY) #(select-tool app :selection)
                   (= keyCode LEFT-ARROW-KEY) #(animation/prev-frame app)
                   (= keyCode RIGHT-ARROW-KEY) #(animation/next-frame app)
                   (and (= keyCode Z-KEY) (or ctrlKey metaKey) shiftKey)
                     #(timemachine/do-redo)
                   (and (= keyCode Z-KEY) (or ctrlKey metaKey))
                     #(timemachine/do-undo)
                   (and (= keyCode C-KEY) (or ctrlKey metaKey))
                     #(globalcommands/copy)
                   (and (= keyCode V-KEY) (or ctrlKey metaKey))
                     #(globalcommands/paste)
                   (and (= keyCode A-KEY) (or ctrlKey metaKey))
                     #(globalcommands/select-all)
                   (= keyCode C-KEY) #(globalcommands/clear)
                   (= keyCode BACKSPACE-KEY) #(globalcommands/clear)
                   (= keyCode A-KEY) #(animation/add-new-frame app)
                 )]
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
