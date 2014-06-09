(ns goya.components.palette
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [goog.dom :as dom]
            [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.timemachine :as timemachine]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn class-name-for-entry [current-color color]
  (if (= current-color color)
    (str "palette_entry" " " "palette_entry_selected")
    "palette_entry"))


(defn palette-entry-component [entry owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [selectchan current-color]}]
      (omdom/div
       #js {:className (class-name-for-entry current-color (:color entry))
            :style #js {:backgroundColor (:color entry)}
            :onClick #(put! selectchan (:color @entry))}
       ""))))


(defn palette-adder-component [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [addchan]}]
      (omdom/div
       #js {:className "palette_adder_entry"}
        (omdom/input
         #js {:className "palette_adder_entry_input"
              :id "palette_adder_input"
              :type "color"
              :onKeyDown #(when (= (.-keyCode %) 13)
                            (put! addchan (.-value (dom/getElement "palette_adder_input"))))})
        (omdom/div
         #js {:className "palette_adder_entry_button"
              :onClick #(put! addchan (.-value (dom/getElement "palette_adder_input")))}
         "Add Color")))))

(defn palette-bg-color-component [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [bgchan]}]
      (omdom/div
       #js {:className "background-color-button"
            :onClick #(put! bgchan :set-bg-color)}
       "Set Background"))))


(defn palette-current-colors-component [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [selectchan]}]
      (let [current-color (get-in app [:tools :paint-color])]
        (apply omdom/div #js {:className "palette-colors"}
          (om/build-all palette-entry-component
                        (:palette app)
                        {:init-state {:selectchan selectchan}
                         :state {:current-color current-color}}))))))


(defn set-bg-color [app]
  (om/update! app [:main-app :background-color] (get-in @app [:tools :paint-color])))

(defn set-paint-color [app color]
  (om/update! app [:tools :paint-color] color))

(defn add-color [app color]
  (let [palette (get-in @app [:main-app :palette])
        color-is-new (not (some #(= {:color color} %) palette))]
  (when color-is-new
    (om/transact! app [:main-app :palette] #(conj % {:color color})))
  (set-paint-color app color)
  (when color-is-new
    (om/transact! app
                  [:main-app :undo-history]
                  #(conj % {:action (str "Added Color: " color) :icon "droplet"})
                  :add-to-undo))))


(defn palette-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:addchan (chan)
       :selectchan (chan)
       :bgchan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [addchan (om/get-state owner :addchan)
            selectchan (om/get-state owner :selectchan)
            bgchan (om/get-state owner :bgchan)]
        (go
          (while true
            (let [[v ch] (alts! [addchan selectchan bgchan])]
              (when (= ch addchan)
                (add-color app v))
              (when (= ch selectchan)
                (set-paint-color app v))
              (when (= ch bgchan)
                (set-bg-color app)))))))

    om/IRenderState
    (render-state [this {:keys [addchan selectchan bgchan]}]
      (omdom/div #js {:className "palette"}
        (om/build palette-adder-component app {:init-state {:addchan addchan}})
        (om/build palette-bg-color-component app {:init-state {:bgchan bgchan}})
        (om/build palette-current-colors-component
                  {:palette (get-in app [:main-app :palette])
                   :tools (get-in app [:tools])}
                  {:init-state {:selectchan selectchan}})))))
