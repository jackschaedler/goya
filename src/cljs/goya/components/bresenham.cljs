(ns goya.components.bresenham)


;; =============================================================================
;; Bresenham line drawing algorithm
;; Stolen from:
;;   http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Clojure

(defn bresenham [x0 y0 x1 y1]
  (let [len-x (js/Math.abs (- x0 x1))
        len-y (js/Math.abs (- y0 y1))
        is-steep (> len-y len-x)]
    (let [[x0 y0 x1 y1] (if is-steep [y0 x0 y1 x1] [x0 y0 x1 y1])]
      (let [[x0 y0 x1 y1] (if (> x0 x1) [x1 y1 x0 y0] [x0 y0 x1 y1])]
        (let [delta-x (- x1 x0)
              delta-y (js/Math.abs (- y0 y1))
              y-step (if (< y0 y1) 1 -1)]
          (loop [x x0
                 y y0
                 error (js/Math.floor delta-x 2)
                 pixels (if is-steep [[y x]] [[x y]])]
            (if (> x x1)
              pixels
              (if (< error delta-y)
                (recur (inc x)
                       (+ y y-step)
                       (+ error (- delta-x delta-y))
                       (if is-steep (conj pixels [y x]) (conj pixels [x y])))
                (recur (inc x)
                       y
                       (- error delta-y)
                       (if is-steep (conj pixels [y x]) (conj pixels [x y]))
                       )))))))))
