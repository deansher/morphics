(ns morphics.examples.pleasantries-test
  (:require [clojure.test :refer :all]
            [morphics.core :as m :refer (o>)]
            [morphics.examples.pleasantries :as pl]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as orch]))

(orch/instrument)
(s/check-asserts true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A very simple test case of the pleasantries framework.

(defn- sue-fred-hear [state line]
  (let [update-state (fn [state happiness-delta]
                       (let [new-expectation-greeting (max -1.0 (- (::pl/expectation-greeting state) 1.0))
                             new-happiness (+ (::pl/emotion-happiness state) happiness-delta)]
                         (assoc state ::pl/expectation-greeting new-expectation-greeting
                                      ::pl/emotion-happiness new-happiness
                                      ::pl/done (< new-happiness 0.0))))]
    (if (= line [:hello])
      (update-state state (::pl/expectation-greeting state))
      (update-state state -0.2))))

(s/def sue-fred-hear ::pl/hear)

(def sue {::pl/get-initial-state (fn [] pl/default-initial-party-state)
          ::pl/hear              sue-fred-hear
          ::pl/speak             (fn [state] [[:hello] state])})

(s/def ::anyone-already-spoke boolean?)

(def fred {::pl/get-initial-state (fn [] (assoc pl/default-initial-party-state ::anyone-already-spoke false))
           ::pl/hear              (fn [state line]
                                    (assoc (sue-fred-hear state line)
                                      ::anyone-already-spoke true))
           ::pl/speak             (fn [state]
                                    [(if (::anyone-already-spoke state)
                                       [:huh]
                                       [:hello])
                                     (assoc state ::anyone-already-spoke true)])})

(s/assert (s/tuple double? double?) (pl/score-chat fred sue 20))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise morphics more thoroughly against the pleasantries framework.

(s/assert ::m/formation (m/get-formation ::pl/party-formation-1))

