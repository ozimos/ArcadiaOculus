(ns arcadia-demo.core
  (:require [arcadia.core :refer :all]
            [arcadia.linear :as l])
  (:import
   (UnityEngine GameObject Quaternion Application Resources Vector3
                QualitySettings)
   (System DateTime)))



;; Don't burn my computer
;; (defn init-fps!
;;   [& _]
;;   (set! (.. QualitySettings vSyncCount) 0)
;;   (set! (.. Application targetFrameRate) 15))

;; (init-fps!)

;; (hook+ (object-named "Main Camera")
;;        :start
;;        :set-fps
;;        #'init-fps!)

(defn clear-scene!
  [go]
  (doseq [c (children go)]
    (destroy c)))

(defn move!
  [go k]
  (let [old-pos (.. go transform position)
        out-of-bounds (< 30 (.. old-pos x))
        ^Float x-incr
        (case k
          :move-fast (if out-of-bounds -20 0.05)
          :move-slow (if out-of-bounds -20 0.01)
          :else (do (log (str "No speed set for" k))
                    0))]
    (set! (.. go transform position)
          (l/v3+ old-pos (l/v3 x-incr 0 0)))))




(defn make-hour-indicator [go face ind angle]
  (let [prefab (Resources/Load "Prefabs/HourIndicator")
        hi (GameObject/Instantiate prefab)
        hi-transform (.. hi transform)]
    (child+ go hi)
    (.RotateAround hi-transform (.. face transform position)
                   (.. Vector3 back) angle)))

(defn make-clock-hands
  ([go pos scl]
   (make-clock-hands go pos scl  (Resources/Load "Materials/Clock Arm")))
  ([go pos scl mat]
   (let [prefab (Resources/Load "Prefabs/HourIndicator")
         clock-hand (GameObject/Instantiate prefab)
         clock-hand-pivot
         (GameObject. "Pivot")
         ch-transform (..  clock-hand transform)]
     (child+ go clock-hand-pivot)
     (child+ clock-hand-pivot clock-hand)
     (with-cmpt clock-hand
       [rd UnityEngine.MeshRenderer]
       (set! (.. rd material) mat))
     (set! (.. ch-transform position) pos)
     (set! (.. ch-transform localScale) scl)
     clock-hand-pivot)))

(comment
  (def holder (object-named "Clock")))


(defmutable ClockState [^float hour ^float minute ^float second])

(def clock-state (->ClockState 0 0 0))

(defn update-clock! [_ _]
  (let [now (.. DateTime Now TimeOfDay)]
    (mut! clock-state :hour (* (float (.TotalHours now)) 30))
    (mut! clock-state :minute (* (float (.TotalMinutes now)) 6))
    (mut! clock-state :second (* (float (.TotalSeconds now)) 6))))

(defn tick2! [go k]
  (let [now (.. DateTime Now TimeOfDay)
        angle (case k
                :hour (* (float (.TotalHours now)) 30)
                :minute (* (float (.TotalMinutes now)) 6)
                :second  (* (float (.TotalSeconds now)) 6)
                0)]
    (set! (.. go transform rotation) (Quaternion/AngleAxis angle (.. Vector3 back)))))

(defn tick! [go k]
  (let [angle (case k
                :hour (.. clock-state hour)
                :minute (.. clock-state minute)
                :second  (.. clock-state second)
                0)]
    (set! (.. go transform rotation) (Quaternion/AngleAxis angle (.. Vector3 back)))))

(defn init!
  [& _]
  (let [holder (object-named "Clock")
        face (create-primitive :cylinder "Face")

        c1 (create-primitive :cube "FastCube")
        c2 (create-primitive :cube "SlowCube")]

    (clear-scene! holder)
    (cmpt- face UnityEngine.CapsuleCollider)

    (child+ holder face)
    (set! (.. face transform localScale)
          (l/v3 10 0.2 10))

    (set! (.. face transform eulerAngles)
          (l/v3 90 0 0))

    (doall (map-indexed #(make-hour-indicator holder face %1 %2) (range 0 360 30)))
    (let [hour-hand
          (make-clock-hands holder (l/v3 0 0.75 -0.25) (l/v3 0.3 2.5 0.1))
          minute-hand
          (make-clock-hands holder (l/v3 0 1 -0.35) (l/v3 0.2 4 0.1))
          second-hand
          (make-clock-hands holder
                            (l/v3 0 1.25 -0.45)
                            (l/v3 0.1 5 0.1)
                            (Resources/Load "Materials/Seconds Arm"))]
      (hook+ face :update :clock #'update-clock!)
      (hook+ hour-hand :update :hour #'tick!)
      (hook+ minute-hand :update :minute #'tick!)
      (hook+ second-hand :update :second #'tick!))
    (child+ holder c1)
    (set! (.. c1 transform position)
          (l/v3 10 1 3))

    (child+ holder c2)
    (set! (.. c2 transform position)
          (l/v3 9 0 -3))

    (hook+ c1 :update :move-fast #'move!)
    (hook+ c2 :update :move-slow #'move!)))

(defn create-hour-indicator-prefab []
  (let [hour-indicator (create-primitive :cube "HourIndicator")]
    (with-cmpt hour-indicator
      [rd UnityEngine.MeshRenderer]
      (set! (.. rd material)
            (Resources/Load "Materials/Hour Indicator")))
    (set! (.. hour-indicator transform localScale)
          (l/v3 0.5 1 0.1))
    (set! (.. hour-indicator transform position)
          (l/v3 0 4 -0.25))))

(comment
  (init!)
  (let [holder (object-named "Clock")]
    (hook+ holder :start :clock #'init!)))
