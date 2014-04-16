(ns goya.canvasdrawing)


;; =============================================================================
;; Blitting routines. Javascript interop is so easy!


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
    (doseq [x (range 0 pixel-count)]
      (let [pix-x (* (mod x width) pixel-size)
            pix-y (* (quot x height) pixel-size)
            color (nth pixels x)]
        (set! (.-fillStyle context) color)
        (.fillRect context pix-x pix-y pixel-size pixel-size)))))


(defn draw-pixel-grid [canvas width height zoom-factor]
  (let [context (.getContext canvas "2d")
        screen-canvas-width (* width zoom-factor)
        screen-canvas-height (* height zoom-factor)
        pixel-size zoom-factor]
      (.save context)
      (.translate context 0.5 0.5)
      (set! (.-strokeStyle context) "rgba(127,127,127,0.4)")
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


