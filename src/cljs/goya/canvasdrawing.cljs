(ns goya.canvasdrawing)


;; =============================================================================
;; Blitting routines. Javascript interop is so easy!

(defn draw-pixel [x y size color context]
  (set! (.-fillStyle context) color)
  (.fillRect context x y size size))


(defn draw-image-to-canvas [pixels canvas width height zoom-factor]
  (let [context (.getContext canvas "2d")
        screen-canvas-width (* width zoom-factor)
        screen-canvas-height (* height zoom-factor)
        pixel-size zoom-factor
        pixel-count (count pixels)]
    (set! (.-width canvas) screen-canvas-width)
    (set! (.-height canvas) screen-canvas-height)
    (set! (.-fillStyle context) "#ff0000")
    (.fillRect context 0 0 screen-canvas-width screen-canvas-height)
    (dotimes [x pixel-count]
      (let [pix-x (* (mod x width) pixel-size)
            pix-y (* (quot x height) pixel-size)
            color (nth pixels x)]
        (when
          (not= color "#T")
          (draw-pixel pix-x pix-y pixel-size color context))))))


(defn draw-spritesheet-to-canvas [animation canvas frame-width frame-height]
  (let [context (.getContext canvas "2d")
        frame-count (count animation)
        canvas-width (* frame-width frame-count)
        canvas-height frame-height]
    (set! (.-width canvas) canvas-width)
    (set! (.-height canvas) canvas-height)
    (dotimes [x frame-count]
      (let [frame (nth animation x)
            pixels (:image-data frame)]
        (dotimes [i (count pixels)]
          (let [x-offset (* x frame-width)
                pix-x (+ (mod i frame-width) x-offset)
                pix-y (quot i frame-height)
                color (nth pixels i)]
            (when
              (not= color "#T")
              (draw-pixel pix-x pix-y 1 color context))))))))


(defn draw-onionskin-to-canvas [pixels canvas width height zoom-factor user-color]
  (let [context (.getContext canvas "2d")
        screen-canvas-width (* width zoom-factor)
        screen-canvas-height (* height zoom-factor)
        pixel-size zoom-factor
        pixel-count (count pixels)]
    (.save context)
    (set! (.-globalAlpha context) 0.2)
    (dotimes [x pixel-count]
      (let [pix-x (* (mod x width) pixel-size)
            pix-y (* (quot x height) pixel-size)
            color (nth pixels x)]
        (when
          (not= color "#T")
          (draw-pixel pix-x pix-y pixel-size color context))))
    (.restore context)))


(defn draw-pixel-grid [canvas width height zoom-factor]
  (let [context (.getContext canvas "2d")
        screen-canvas-width (* width zoom-factor)
        screen-canvas-height (* height zoom-factor)
        pixel-size zoom-factor]
      (.save context)
      (.translate context 0.5 0.5)
      (set! (.-strokeStyle context) "rgba(60,50,40,0.4)")
      (set! (.-lineWidth context) 1)
      (dotimes [y height]
        (let [pix-y (* y pixel-size)]
          (doto context
            (.beginPath)
            (.moveTo 0 pix-y)
            (.lineTo screen-canvas-width pix-y)
            (.stroke))))
      (dotimes [x height]
        (let [pix-x (* x pixel-size)]
          (doto context
            (.beginPath)
            (.moveTo pix-x 0)
            (.lineTo pix-x screen-canvas-height)
            (.stroke))))
      (.restore context)))


