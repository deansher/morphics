(ns morphics.mission-test
  (:require [clojure.test :refer :all]
            [morphics.mission :as mission]
            [orchestra.spec.test :as orch]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound])
  (:import (clojure.lang ExceptionInfo)))

(set! s/*explain-out* expound/printer)

(defrecord Foo [])

(defrecord FooWithMissionTag []
  mission/HasMissionTag
  (mission-tag* [_] ::foo-mission))

(mission/prefer-key ::tag1 ::tag2)

(mission/prefer-key ::tag2 ::tag3)

;; a deliberate loop
(mission/prefer-key ::tag3 ::tag1)

(mission/def-tag-fn ::tag1)

(mission/def-tag-fn ::tag1b)

(mission/def-tag-fn ::tag2 (constantly ::tag2-mission))

(mission/def-tag-fn ::tag3 (constantly Runnable))

(deftest tag-test
  (is (= (type (->Foo)) (mission/tag (->Foo))))
  (is (= ::foo-mission (mission/tag (->FooWithMissionTag))))
  (is (= ::red (mission/tag {::tag1 ::red})))
  (is (= ::red (mission/tag {::tag1 ::red, ::tag2 ::blue})))
  (is (thrown-with-msg? ExceptionInfo #"Consider.*prefer-key"
                        (mission/tag {::tag1 ::red, ::tag1b ::blue})))
  (is (thrown-with-msg? ExceptionInfo #"loop"
                        (mission/tag {::tag1 ::red ::tag2 ::blue ::tag3 ::green}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Use everyone's favorite example as a test case.

(derive ::rectangle ::shape)

(defn rectangle [width height]
  {::mission/tag ::rectangle
   ::width       width
   ::height      height})

(s/fdef rectangle
        :args (s/cat :width double?
                     :height double?)
        :ret (mission/implements ::rectangle))

(mission/add-constructor ::rectangle #'rectangle)

(defmulti area mission/tag)

(defmethod area ::rectangle [r] (* (::width r) (::height r)))

(orch/instrument)
