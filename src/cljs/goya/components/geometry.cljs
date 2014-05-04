(ns goya.components.geometry)


;; =============================================================================
;; Some very basic operations and transforms on grids/rectangles

(defn screen-to-doc [x y zoom-factor]
  [(quot x zoom-factor) (quot y zoom-factor)])


(defn flatten-to-index [x y doc-width]
  (+ (* y doc-width) x))

(defn flatten-point-to-index [point doc-width]
  (flatten-to-index (point 0) (point 1) doc-width))


(defn normalize-rect [orig-x orig-y nx ny]
    [(min orig-x (inc nx))
     (min orig-y (inc ny))
     (max orig-x (inc nx))
     (max orig-y (inc ny))])


(defn contains-point [rectangle point]
  (let [[x1 y1 x2 y2] rectangle
        [x y] point]
    (and (>= x x1) (<= x x2) (>= y y1) (<= y y2))))


(defn rect-width [rect]
  (let [[x1 y1 x2 y2] rect] (- x2 x1)))


(defn rect-height [rect]
  (let [[x1 y1 x2 y2] rect] (- y2 y1)))

