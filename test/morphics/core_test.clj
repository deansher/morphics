(ns morphics.core-test
  (:require [clojure.test :refer :all]
            [morphics.core :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [orchestra.spec.test :as orch]
            [morphics.core :as m]))

(orch/instrument)

(s/def ::inc (s/fspec :args (s/cat :x int?)
                      :ret int?))

(s/def ::dec (s/fspec :args (s/cat :x int?)
                      :ret int?))

(s/def ::inc-dec (s/and ::m/team
                        (s/keys :req [::inc ::dec])))

(m/def-formation
  [::inc-dec-one ::inc-dec]
  ::inc inc
  ::dec dec)
