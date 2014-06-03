(ns goya.components.animation
  (:require-macros [cljs.core.async.macros :refer [go]])
	(:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [goya.timemachine :as timemachine]
            [cljs.core.async :refer [put! chan <! alts!]]))


(defn toggle-onion-skin [app]
  (let [current-state (get-in @app [:onion-skin])
        new-state (if (= current-state :both)
                       :off
                       :both)]
    (om/update! app [:onion-skin] new-state)))



(defn delete-frame [app frame]
  (let [animation (get-in @app [:main-app :animation])
        num-frames (count animation)
        second-subvec-start (min (inc frame) num-frames)
        new-animation (vec (concat (subvec animation 0 frame)
                                   (subvec animation second-subvec-start num-frames)))]
    (om/update! app [:main-app :editing-frame] (max 0 (dec frame)))
    (om/update! app [:main-app :animation] new-animation)
    (om/transact! app
                  [:main-app :undo-history]
                  #(conj % {:action (str "Deleted Frame") :icon "minus-squared"})
                  :add-to-undo)))


(defn delete-current-frame [app]
  (let [frame-count (count (get-in @app [:main-app :animation]))]
    (when (> frame-count 1)
      (delete-frame app (get-in @app [:main-app :editing-frame])))))


(defn add-new-frame [app]
  (let [animation (get-in @app [:main-app :animation])
        current-frame (get-in @app [:main-app :editing-frame])
        frame (nth animation current-frame)]
    (om/transact! app [:main-app :animation] #(conj % frame))
    (om/update! app [:main-app :editing-frame] (count animation))
    (om/transact! app
                  [:main-app :undo-history]
                  #(conj % {:action (str "Added Frame") :icon "plus-squared"})
                  :add-to-undo)))

(defn next-frame [app]
  (let [animation (get-in @app [:main-app :animation])
        max-frame (dec (count animation))
        current-frame (get-in @app [:main-app :editing-frame])
        new-frame (min max-frame (inc current-frame))]
    (om/update! app [:main-app :editing-frame] new-frame)
    (timemachine/update-preview)))

(defn prev-frame [app]
  (let [current-frame (get-in @app [:main-app :editing-frame])
        new-frame (max 0 (dec current-frame))]
    (om/update! app [:main-app :editing-frame] new-frame)
    (timemachine/update-preview)))

(defn set-current-frame [app frame]
  (om/update! app [:main-app :editing-frame] frame)
  (timemachine/update-preview))


(defn class-name-for-frame [current-frame onion-skinning frame]
  (cond
    (= current-frame frame) (str "animation-frame" " " "animation-frame-selected")
    (and (= (dec current-frame) frame)
         (= onion-skinning :both)) (str "animation-frame" " " "animation-frame-onion-skin")
    (and (= (inc current-frame) frame)
         (= onion-skinning :both)) (str "animation-frame" " " "animation-frame-onion-skin")
    :else "animation-frame"))


(defn frame-component [entry owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [selectchan current-frame onion-skinning index]}]
      (omdom/div
       #js {:className (class-name-for-frame current-frame onion-skinning index)
            :onClick #(put! selectchan index)}
       (omdom/div #js {:className "frame-number"} (str (inc index)))))))


(defn frame-list-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:selectchan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [selectchan (om/get-state owner :selectchan)]
        (go
          (while true
            (let [[v ch] (alts! [selectchan])]
              (when (= ch selectchan)
                (set-current-frame app v)))))))

    om/IRenderState
    (render-state [this {:keys [selectchan]}]
      (let [current-frame (get-in app [:main-app :editing-frame])
            onion-skinning (get-in app [:onion-skin])
            frames (get-in app [:main-app :animation])]
        (apply omdom/div #js {:className "animation-frames"}
          (map-indexed
             (fn [idx frame]
               (om/build frame-component
                         frame
                         {:init-state {:selectchan selectchan}
                          :state {:current-frame current-frame
                                  :onion-skinning onion-skinning
                                  :index idx}}))
             frames))))))


(defn class-name-for-onion-skin-icon [onion-skinning]
  (cond
    (= onion-skinning :both) (str "animation-icon" " " "animation-icon-selected")
    :else "animation-icon"))


(defn animation-controls-component [app owner]
  (reify
    om/IRender
    (render [this]
      (omdom/div #js {:className "animation-tools"}
        (om/build frame-list-component app)
        (omdom/div #js {:className "animation-tools-bar"}
          (omdom/div #js {:className "animation-info"}
                     (let [total-frames (count (get-in app [:main-app :animation]))
                       current-frame (inc (get-in app [:main-app :editing-frame]))]
                       (str "Frame: " current-frame "/" total-frames)))
          (omdom/div #js {:className (class-name-for-onion-skin-icon (get-in app [:onion-skin]))
                          :style #js {:float "right"}
                          :onClick #(toggle-onion-skin app)}
                     (omdom/i #js {:className "icon-layers"}))
          (omdom/div #js {:className "animation-icon"
                          :style #js {:float "right"
                                      :font-size "14px"
                                      :padding-top "4px"}
                          :onClick #(delete-current-frame app)}
                     (omdom/i #js {:className "icon-minus-squared"}))
          (omdom/div #js {:className "animation-icon"
                          :style #js {:float "right"
                                      :font-size "14px"
                                      :padding-top "4px"}
                          :onClick #(add-new-frame app)}
                     (omdom/i #js {:className "icon-plus-squared"})))))))
