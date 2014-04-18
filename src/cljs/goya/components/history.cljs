(ns goya.components.history
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.timemachine :as timemachine]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn history-list-component [undo-history owner]
  (reify
    om/IRender
    (render [this]
      (apply omdom/div #js {:className "undo-list" :transitionName "example"}
        (map-indexed
           (fn [idx history-elem]
             (let [class-name "undo-list-elem"
                   icon-class (str "icon-" (:icon history-elem))
                   undo-history-count (count undo-history)
                   real-idx (dec (- undo-history-count idx))]
               (omdom/li #js {:className class-name
                              :onMouseEnter #(timemachine/show-history-preview real-idx)
                              :onMouseLeave #(timemachine/update-preview)}
                 (omdom/i #js {:className icon-class})
                 (:action history-elem))))
               (reverse undo-history))))))



(defn class-name-for-menu-item [pred]
  (if (pred) "history-menu-elem" "history-menu-elem-disabled"))


(defn undo-button-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let
          [class-name (class-name-for-menu-item timemachine/undo-is-possible)
           text (get-in app [:history-menu-items :undo :text])]
            (omdom/div
               #js {:className class-name :onClick #(timemachine/do-undo)}
               (str " " text))))))


(defn redo-button-component [app owner]
  (reify
    om/IRender
      (render [this]
        (let
          [class-name (class-name-for-menu-item timemachine/redo-is-possible)
           text (get-in app [:history-menu-items :redo :text])]
            (omdom/div
               #js {:className class-name :onClick #(timemachine/do-redo)}
               (str " " text))))))


(defn header-component [app owner]
  (reify
    om/IRender
    (render [this]
      (omdom/div nil
        (om/build redo-button-component app)
        (om/build undo-button-component app)
        (omdom/div #js {:className "history-menu-header-text"}
          (omdom/i #js {:className "icon-back-in-time"})
          "History")))))
