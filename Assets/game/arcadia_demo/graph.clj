(ns game.arcadia-demo.graph
  (:require [arcadia.core :refer :all]
            [arcadia.linear :as l])
  (:import
   (UnityEngine GameObject Resources
                Mathf Time)))

(defn clear-scene!
  [go]
  (doseq [c (children go)]
    (destroy c)))

(defn update-wave! [go _]
  (let [x-val (.. go transform position x)]
    (set! (.. go transform position) (l/v3 x-val (Mathf/Sin (* Mathf/PI (+ (.. Time time) x-val))) 7))))

(defn add-point [go posn]
  (let [point-prefab (Resources/Load "Prefabs/Point")
        pnt (GameObject/Instantiate point-prefab)]
    (set! (.. pnt transform position) posn)
    (child+ go pnt)
    (hook+ pnt :update :wave #'update-wave!)))

(defn init! [& _]
  (let [gr (object-named "Graph")
        pnts (for [x-val (range -1 1.1 0.1)]
               [x-val 0 2])]
    (clear-scene! gr)
    (mapv #(add-point gr (apply l/v3 %)) pnts)))


(comment
  (init!)
(let [holder (object-named "Graph")]
  (hook+ holder :start :clock #'init!))
  (Mathf/Sin 1))