(ns webgl-test.util)

(defn js-vec
  ([e0] (doto (js/Array.) (.push e0)))
  ([e0 e1] (doto (js/Array.) (.push e0) (.push e1)))
  ([e0 e1 e2] (doto (js/Array.) (.push e0) (.push e1) (.push e2)))
  ([e0 e1 e2 e3] (doto (js/Array.) (.push e0) (.push e1) (.push e2) (.push e3)))
  ([e0 e1 e2 e3 & elements]
   (let [p (js-vec e0 e1 e2 e3)]
     (doseq [e elements]
       (.push p e))
     p)))