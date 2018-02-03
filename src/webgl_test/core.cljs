(ns webgl-test.core
  (:require [webgl-test.gl :as gl]
            [webgl-test.util :as u]))

(defn make-shaders [ctx]
  (->> [[:vertex-shader
         "attribute vec3 aVertexPosition;
          attribute vec4 aVertexColor;
          uniform mat4 uMVMatrix;
          uniform mat4 uPMatrix;
          varying vec4 vColor;
          void main(void) {
            gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 2.0);
            vColor = aVertexColor;
          }"]
        [:fragment-shader
         "precision mediump float;
          varying vec4 vColor;
          void main(void) {
            gl_FragColor = vColor;
          }"]]
       (mapv (fn [[type source]]
               (gl/compile-shader ctx type source)))))

(defn make-program [canvas]
  (let [ctx (gl/create-ctx canvas {:alpha true, :antialias false, :premultipliedAlpha false})
        shaders (make-shaders ctx)]
    (-> ctx
        (gl/with-program)
        (gl/add-shaders shaders)
        (gl/link-program)
        (gl/use-program)
        (gl/enable-vertex-attrib-array "aVertexPosition")
        (gl/enable-vertex-attrib-array "aVertexColor")
        (gl/clear-color 0.0 0.0 0.0 1.0)
        (gl/disable :depth-test)
        (gl/enable :blend)
        (gl/blend-func :one :one-minus-src-slpha))))

(defn make-circle-pos-buf [ctx segments]
  (let [d (-> Math/PI (* 2.0) (/ segments))]
    (->> (range segments)
         (mapcat (fn [i] [(Math/cos (* i d))
                          (Math/sin (* i d))
                          0]))
         (gl/create-vertices-array-buffer ctx 3))))

(defn make-circle-color-buf
  ([ctx segments color] (make-circle-color-buf ctx segments color color))
  ([ctx segments color-center color-edge]
   (->> (cycle color-edge)
        (take (* (inc segments) 4))
        (concat color-center)
        (gl/create-vertices-array-buffer ctx 4))))

(def canvas (js/document.getElementById "app"))
(def ctx (make-program canvas))
(def mouse (atom {}))
(def m (js/mat4.create))

#_(defonce mouse-info (add-watch mouse :mouse-info (fn [_ _ prev-state new-state]
                                                     (->> new-state
                                                          (map (fn [[k v]]
                                                                 (when (= v (prev-state k))
                                                                   (if-let [e (-> k name js/document.getElementById)]
                                                                     (set! (.-innerText e) (str v))))))
                                                          (dorun)))))

(defn set-mouse-wha []
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)
        wide? (> width height)
        [aspect-x aspect-y] (if wide?
                              [(/ width height) 1.0]
                              [1.0 (/ height width)])]
    (doto canvas
      (-> .-width (set! width))
      (-> .-height (set! height)))
    (swap! mouse merge {:width width
                        :height height
                        :aspect-x aspect-x
                        :aspect-y aspect-y})))

(defn map-mouse-x [{:keys [aspect-x width]} x]
  (-> (/ x width)
      (- 0.5)
      (* 2.0 aspect-x)))

(defn map-mouse-y [{:keys [aspect-y height]} y]
  (->> (/ y height)
       (- 0.5)
       (* 2.0 aspect-y)))

(def circle-segments 60)
(def circle-pos-buf (make-circle-pos-buf ctx circle-segments))
(def circle-buf-num-items (gl/buffer-num-items circle-pos-buf))

(def circle-color-bufs
  (->> (range 101)
       #_(map (fn [n] (as-> n $
                            (- 100.0 $)
                            (/ $ 100.0)
                            (- $ 0.5)
                            (Math/abs $)
                            (- 0.5 $)
                            (* 2.0 $))))
       (map (fn [n] (as-> n $
                          (- 100.0 $)
                          (/ $ 100.0))))
       (map (fn [a] [0.7 0.1 0.1 a]))
       (map (fn [color] (make-circle-color-buf ctx circle-segments color)))
       (into [])))

(defn circle-color-buf [n]
  (nth circle-color-bufs (-> n (max 0.0) (min 1.0) (* 100.0) (int))))

(defn from-translation [m matrix]
  (js/mat4.fromTranslation m matrix))

(defn from-scaling [m matrix]
  (js/mat4.fromScaling m matrix))

(defn translate [m matrix]
  (js/mat4.translate m m matrix))

(defn scale [m matrix]
  (js/mat4.scale m m matrix))

(defn bind-circle-pos [ctx]
  (-> ctx
      (gl/bind-buffer :array-buffer circle-pos-buf)
      (gl/vertex-attrib-pointer "aVertexPosition" 3 :float false 0 0)))

(defn bind-circle-color [ctx a]
  (-> ctx
      (gl/bind-buffer :array-buffer (circle-color-buf a))
      (gl/vertex-attrib-pointer "aVertexColor" 4 :float false 0 0)))

(defn draw-circle [ctx tr-m]
  (-> ctx
      (gl/uniform-matrix-4fv "uMVMatrix" false tr-m)
      (gl/draw-arrays :line-loop 0 circle-buf-num-items)))

(defn reset-element [e]
  (doto e
    (-> .-alive (set! false))))

(defn reanimate-element [mouse-x mouse-y e]
  (doto e
    (-> .-alive (set! true))
    (-> .-r (set! 0.01))
    (-> .-age (set! 0.0))
    (-> .-x (set! mouse-x))
    (-> .-y (set! mouse-y))))

(defn make-element []
  (-> (js/Object.)
      (doto
        (-> .-r (set! 0.0))
        (-> .-age (set! 0.0))
        (-> .-x (set! 0.0))
        (-> .-y (set! 0.0)))
      (reset-element)))

(defn age-element [e]
  (doto e
    (-> .-age (set! (-> e .-age (+ 0.002))))
    (-> .-r (set! (-> e .-r (+ 0.001))))))

(defn update-element [e]
  (if (-> e .-age (< 1.0))
    (age-element e)
    (reset-element e)))

(defn draw-element [ctx e]
  (-> ctx
      (bind-circle-color (.-age e))
      (draw-circle (-> m
                       (from-translation (u/js-vec (.-x e) (.-y e) 0.0))
                       (scale (u/js-vec (.-r e) (.-r e)))))))

(defn draw-elements [ctx es]
  (doseq [e es]
    (when (-> e .-alive)
      (->> e
           (update-element)
           (draw-element ctx))))
  ctx)

(def elements (-> (repeatedly 1000 make-element)
                  (into [])))

(defn draw-scene [{:keys [width height aspect-x aspect-y] :as ctx}]
  (-> ctx
      (gl/viewport 0 0 width height)
      (gl/uniform-matrix-4fv "uPMatrix" false (from-scaling m (u/js-vec (/ 1.0 aspect-x) (/ 1.0 aspect-y))))
      (bind-circle-pos)
      (draw-elements elements)
      (gl/flush)))

(defn game-loop [_]
  (js/window.requestAnimationFrame game-loop)
  (draw-scene (-> ctx (merge @mouse))))

(defn init []
  (.addEventListener js/window "resize" (fn [_] (set-mouse-wha)))
  (.addEventListener canvas "mouseenter" (fn [_] (swap! mouse assoc :mouse-on true)))
  (.addEventListener canvas "mouseleave" (fn [_] (swap! mouse assoc :mouse-on false)))
  (.addEventListener canvas "mousemove" (fn [e]
                                          (let [mouse @mouse
                                                x (->> e .-offsetX (map-mouse-x mouse))
                                                y (->> e .-offsetY (map-mouse-y mouse))]
                                            (some->> elements
                                                     (remove (fn [element] (-> element .-alive)))
                                                     (first)
                                                     (reanimate-element x y)))))

  (game-loop 0))

(set-mouse-wha)
(defonce _ (init))

(js/console.log "Page ready")
