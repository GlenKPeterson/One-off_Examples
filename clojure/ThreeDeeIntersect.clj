;; Copyright 2014 Glen Peterson
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;    http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

;; You really want a terminal at least 100 chars wide and
;; exactly 46 high to see this properly.
(defn showSphere
      "Displays the intersection of the given sphere (x, y, z) center points
       and radius r against the plane z=0 in somewhat anti-aliased ASCII art"
      [h k l r]
      (println ";;  y|     x=10|       20|       30|       40|       50|       60|       70|       80|       90|")
      (dotimes [y 44]
        (print ";;" (if (< y 10) (str " " y) y))
        (dotimes [x 94]
          (let [z 0
                ;; [(x-h)^2 + (y-k)^2 + (z-l)^2] - r^2
                dSphere (- (+ (Math/pow (- x h) 2.0)
                              (Math/pow (- y k) 2.0)
                              (Math/pow (- z l) 2.0)) 
                              (Math/pow r 2.0))
                av (Math/abs dSphere)
               ]
               (print (cond (< av 0) " "
                            (< av 1) "@"
                            (< av 10) "*"
                            (< av 40) "'"
                            (= 0 (rem x 10)) "|"
                            (= 0 (rem y 10)) "-"
                            :else " "))))
        (println)))

(dotimes [t 61]
         (showSphere (+ 40 (* 20 (Math/sin (* t (/ Math/PI 30))))) 
                     (+ 20 (* 10 (Math/cos (* t (/ Math/PI 20)))))
                     (- t 30) 30)
         (Thread/sleep (if (< t 7) (- 300 (* 30 t)) 150)))
