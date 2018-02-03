(ns webgl-test.gl
  (:refer-clojure :exclude [flush])
  (:require [clojure.string :as str]))

;;
;; ========================================================
;; gl-matrix:
;; ========================================================
;;

(defn deg->rad [angle]
  (.toRadian js/glMatrix angle))

;;
;; ========================================================
;; WegGL:
;; ========================================================
;;

(defn- prop [gl k]
  (aget gl (-> k (name) (str/upper-case) (str/replace #"-" "_"))))

(defn create-ctx
  ([canvas] (create-ctx canvas nil))
  ([canvas opts]
   (let [width (.-offsetWidth canvas)
         height (.-offsetHeight canvas)
         gl (.getContext canvas "webgl" (->> opts
                                             (merge {:alpha false
                                                     :antialias true})
                                             (clj->js)))]
     (doto canvas
       (-> .-width (set! width))
       (-> .-height (set! height)))
     {:canvas canvas
      :width width
      :height height
      :aspect (double (/ width height))
      :gl gl})))

(defn create-buffer [{:keys [gl]}]
  (.createBuffer gl))

(defn bind-buffer [{:keys [gl] :as ctx} target buffer]
  (.bindBuffer gl (prop gl target) buffer)
  ctx)

(defn buffer-data
  ([ctx target data] (buffer-data ctx target data :static-draw))
  ([{:keys [gl] :as ctx} target data usage]
   (.bufferData gl (prop gl target) data (prop gl usage))
   ctx))

(defn clear-color [{:keys [gl] :as ctx} r g b a]
  (.clearColor gl r g b a)
  ctx)

(defn clear-depth
  ([{:keys [gl] :as ctx}]
   (.clearDepth gl)
   ctx)
  ([{:keys [gl] :as ctx} depth]
   (.clearDepth gl depth)
   ctx))

(defn clear-stencil
  ([{:keys [gl] :as ctx}]
   (.clearStencil gl)
   ctx)
  ([{:keys [gl] :as ctx} stencil]
   (.clearStencil gl stencil)
   ctx))

(defn clear
  ([{:keys [gl] :as ctx} bit]
   (.clear gl (prop gl bit))
   ctx)
  ([{:keys [gl] :as ctx} bit1 bit2]
   (.clear gl (bit-or (prop gl bit1) (prop gl bit2)))
   ctx)
  ([{:keys [gl] :as ctx} bit1 bit2 & more]
   (.clear gl (apply bit-or (prop gl bit1) (prop gl bit2) (map #(prop gl %) more)))
   ctx))

(defn enable
  ([{:keys [gl] :as ctx} k]
   (.enable gl (prop gl k))
   ctx)
  ([{:keys [gl] :as ctx} k1 k2]
   (.enable gl (bit-or (prop gl k1) (prop gl k2)))
   ctx)
  ([{:keys [gl] :as ctx} k1 k2 & more]
   (.enable gl (apply bit-or (prop gl k1) (prop gl k2) (map #(prop gl %) more)))
   ctx))

(defn disable
  ([{:keys [gl] :as ctx} k]
   (.disable gl (prop gl k))
   ctx)
  ([{:keys [gl] :as ctx} k1 k2]
   (.disable gl (bit-or (prop gl k1) (prop gl k2)))
   ctx)
  ([{:keys [gl] :as ctx} k1 k2 & more]
   (.disable gl (apply bit-or (prop gl k1) (prop gl k2) (map #(prop gl %) more)))
   ctx))

(defn blend-func [{:keys [gl] :as ctx} s-factor d-factor]
  (.blendFunc gl (prop gl s-factor) (prop gl d-factor))
  ctx)

(defn compile-shader [{:keys [gl]} type source]
  (let [shader (.createShader gl (prop gl type))]
    (.shaderSource gl shader source)
    (.compileShader gl shader)
    (when-not (.getShaderParameter gl shader (prop gl :compile-status))
      (throw (ex-info (.getShaderInfoLog gl shader) {})))
    shader))

(defn with-program [{:keys [gl] :as ctx}]
  (assoc ctx :program (.createProgram gl)))

(defn add-shader
  ([{:keys [gl program] :as ctx} shader]
   (.attachShader gl program shader)
   ctx)
  ([{:keys [gl program] :as ctx} shader1 shader2]
   (.attachShader gl program shader1)
   (.attachShader gl program shader2)
   ctx)
  ([{:keys [gl program] :as ctx} shader1 shader2 & more]
   (.attachShader gl program shader1)
   (.attachShader gl program shader2)
   (doseq [shader more]
     (.attachShader gl program shader))
   ctx))

(defn add-shaders [{:keys [gl program] :as ctx} shaders]
  (doseq [shader shaders]
    (.attachShader gl program shader))
  ctx)

(defn link-program [{:keys [gl program] :as ctx}]
  (.linkProgram gl program)
  (when-not (.getProgramParameter gl program (prop gl :link-status))
    (throw (ex-info "failed to link program" {})))
  ctx)

(defn use-program [{:keys [gl program] :as ctx}]
  (.useProgram gl program)
  ctx)

(defn viewport [{:keys [gl] :as ctx} x y w h]
  (.viewport gl x y w h)
  ctx)

(defn- get-attrib-loc! [{:keys [gl program]} attrib-name]
  (or (.getAttribLocation gl program attrib-name)
      (throw (ex-info (str "unknown attribute: \"" attrib-name "\"") {}))))

(defn attrib-loc [ctx index-or-name]
  (cond
    (number? index-or-name) index-or-name
    (keyword? index-or-name) (get-attrib-loc! ctx (name index-or-name))
    (string? index-or-name) (get-attrib-loc! ctx index-or-name)))

(defn- get-uniform-loc! [{:keys [gl program]} uniform-name]
  (or (.getUniformLocation gl program uniform-name)
      (throw (ex-info (str "unknown unform: \"" uniform-name "\"") {}))))

(defn uniform-loc [ctx index-or-name]
  (cond
    (number? index-or-name) index-or-name
    (keyword? index-or-name) (get-uniform-loc! ctx (name index-or-name))
    (string? index-or-name) (get-uniform-loc! ctx index-or-name)))

(defn enable-vertex-attrib-array [{:keys [gl] :as ctx} index-or-name]
  (.enableVertexAttribArray gl (attrib-loc ctx index-or-name))
  ctx)

(defn uniform-matrix-2fv [{:keys [gl] :as ctx} location transpose value]
  (.uniformMatrix2fv gl (uniform-loc ctx location) transpose value)
  ctx)

(defn uniform-matrix-3fv [{:keys [gl] :as ctx} location transpose value]
  (.uniformMatrix3fv gl (uniform-loc ctx location) transpose value)
  ctx)

(defn uniform-matrix-4fv [{:keys [gl] :as ctx} location transpose value]
  (.uniformMatrix4fv gl (uniform-loc ctx location) transpose value)
  ctx)

(defn vertex-attrib-pointer [{:keys [gl] :as ctx} attrib size type normalized? stride offset]
  (.vertexAttribPointer gl (attrib-loc ctx attrib) size (prop gl type) normalized? stride offset)
  ctx)

(defn draw-arrays [{:keys [gl] :as ctx} mode first count]
  (.drawArrays gl (prop gl mode) first count)
  ctx)

(defn flush [{:keys [gl] :as ctx}]
  (.flush gl)
  ctx)

;;
;; Utils:
;;

(defn create-vertices-buffer
  ([ctx target vertices] (create-vertices-buffer ctx target nil vertices))
  ([{:keys [gl] :as ctx} target item-size vertices]
   (let [buffer (create-buffer ctx)]
     (bind-buffer ctx target buffer)
     (buffer-data ctx target (-> vertices clj->js js/Float32Array.))
     (when item-size
       (doto buffer
         (aset "item-size" item-size)
         (aset "num-items" (/ (count vertices) item-size))))
     buffer)))

(defn create-vertices-array-buffer
  ([ctx vertices] (create-vertices-buffer ctx :array-buffer vertices))
  ([ctx item-size vertices] (create-vertices-buffer ctx :array-buffer item-size vertices)))

(defn buffer-item-size [buffer]
  (or (aget buffer "item-size")
      (throw (ex-info "buffer does not have item-size" {}))))

(defn buffer-num-items [buffer]
  (or (aget buffer "num-items")
      (throw (ex-info "buffer does not have num-items" {}))))
