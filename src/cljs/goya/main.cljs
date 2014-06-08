(ns goya.core
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [goog.dom :as dom]
			      [goog.events :as events]
            [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.appstate :as app]
            [goya.timemachine :as timemachine]
            [goya.guistate :as guistate]
            [goya.previewstate :as previewstate]
            [goya.globalcommands :as globalcommands]
            [goya.components.animation :as animation]
            [goya.components.mainmenu :as mainmenu]
            [goya.components.toolsmenu :as toolsmenu]
            [goya.components.canvastools :as canvastools]
            [goya.components.palette :as palette]
            [goya.components.history :as history]
            [goya.components.canvas :as goyacanvas]
            [goya.components.drawing :as drawing]
            [goya.components.keylistener :as keylistener]
            [goya.canvasdrawing :as canvasdrawing]
            [cljs.core.async :refer [put! chan <! alts!]]))


(.log js/console "Welcome to Goya, the clojurescript pixel-art studio")

(enable-console-print!)

(defn tx-listener [tx-data root-cursor]
  (timemachine/handle-transaction tx-data root-cursor))


;; =============================================================================
;; This got out of hand before I got the hang of OM. Subsequent version will
;; place everything in a master component, so the app will ideally have one root


(om/root
  (fn [app owner]
    (omdom/h1 #js {:className "app-title"}
      (:title app)
      (omdom/h6 #js {:className "app-subtitle"}
        (str (:subtitle app) " / " (:version app)))))
  app/app-state
  {:path [:info]
   :target (. js/document (getElementById "title"))
   :tx-listen #(tx-listener % %)})


(om/root
  mainmenu/menu-component
  app/app-state
  {:target (. js/document (getElementById "mainMenu"))})


(om/root
  palette/palette-component
  app/app-state
  {:target (. js/document (getElementById "palette"))})


(om/root
  history/header-component
  app/app-state
  {:target (. js/document (getElementById "timeMachineHeader"))})


(om/root
  history/history-list-component
  app/app-state
  {:target (. js/document (getElementById "undoHistory"))
   :path [:main-app :undo-history]})


(om/root
  toolsmenu/tools-menu-component
  app/app-state
  {:target (. js/document (getElementById "tools-menu"))
   :path [:tools]})


(om/root
  canvastools/cursor-pos-component
  guistate/transient-state
  {:target (. js/document (getElementById "cursor-pos-indicator"))})

(om/root
  canvastools/frame-num-component
  previewstate/preview-state
  {:target (. js/document (getElementById "frame-num-indicator"))})


(om/root
  canvastools/grid-toggle-component
  app/app-state
  {:target (. js/document (getElementById "grid-toggle"))})


(om/root
  canvastools/canvas-info-component
  app/app-state
  {:target (. js/document (getElementById "canvas-info"))})


(om/root
  goyacanvas/canvas-minimap-component
  previewstate/preview-state
  {:target (. js/document (getElementById "minimap-canvas"))})


(om/root
  goyacanvas/main-canvas-component
  app/app-state
  {:target (. js/document (getElementById "canvas-wrapper"))})


(om/root
  drawing/canvas-painting-component
  app/app-state
  {:target (. js/document (getElementById "canvas-watcher"))
   :shared {:command-chan globalcommands/command-chan}})


(om/root
  animation/animation-controls-component
  app/app-state
  {:target (. js/document (getElementById "animation-controls"))})


(om/root
  keylistener/key-listener-component
  app/app-state
  {:target (. js/document (getElementById "keylistener"))
   :shared {:keychan keylistener/key-chan}})

(events/listen js/document "keydown" #(put! keylistener/key-chan %))
