(ns goya.components.palette
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [goog.dom :as dom]
            [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.timemachine :as timemachine]
            [cljs.core.async :refer [put! chan <! alts!]]))



(defn palette-entry-component [entry owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [selectchan]}]
      (omdom/div
       #js {:className "palette_entry"
            :style #js {:backgroundColor (:color entry)}
            :onClick (fn [e] (put! selectchan (:color @entry)))}
       (:text entry)))))


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
              :onKeyDown (fn [e]
                           (when (= (.-keyCode e) 13)
                             (put! addchan (.-value (dom/getElement "palette_adder_input")))))})
        (omdom/div
         #js {:className "palette_adder_entry_button"
              :onClick (fn [e]
                         (put! addchan (.-value (dom/getElement "palette_adder_input"))))}
         "Add Color")))))


(defn palette-current-colors-component [palette owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [selectchan]}]
      (apply omdom/div #js {:className "palette-colors"}
        (om/build-all palette-entry-component palette {:init-state {:selectchan selectchan}})))))



(defn set-paint-color [app color]
  (om/update! app [:tools :paint-color] color))

(defn add-color [app color]
  (om/transact! app [:main-app :palette] #(conj % {:color color}))
  (set-paint-color app color)
  (om/transact! app
                [:main-app :undo-history]
                #(conj % {:action (str "Added Color: " color) :icon "droplet"})
                :add-to-undo))


(defn palette-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:addchan (chan)
       :selectchan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [addchan (om/get-state owner :addchan)
            selectchan (om/get-state owner :selectchan)]
        (go
          (while true
            (let [[v ch] (alts! [addchan selectchan])]
              (when (= ch addchan)
                (add-color app v))
              (when (= ch selectchan)
                (set-paint-color app v)))))))

    om/IRenderState
    (render-state [this {:keys [addchan selectchan]}]
      (omdom/div #js {:className "palette"}
        (om/build palette-adder-component app {:init-state {:addchan addchan}})
        (om/build palette-current-colors-component (get-in app [:main-app :palette])
                  {:init-state {:selectchan selectchan}})))))
