(ns morphics.examples.pleasantries-test
  (:require [clojure.test :refer :all]
            [com.rpl.specter :refer :all]
            [morphics.core :as m :refer (o>)]
            [morphics.examples.pleasantries :as pl]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as orch]))

(orch/instrument)
(s/check-asserts true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A very simple test case of the pleasantries framework.

;; If Sue or Fred hear [:hello], they add their greeting expectation to their happiness.
;; Else they subtract 0.2 from their happiness.
;; In any case, they subtract 1.0 from their greeting expectation. So it goes 1.0, 0.0, -1.0.
(defn- sue-fred-hear [state line]
  (let [update-state (fn [state happiness-delta]
                       (let [new-happiness (-> state ::pl/emotion-happiness (+ happiness-delta))
                             state         (transform ::pl/expectation-greeting
                                                      (fn [eg] (-> eg dec (max -1.0)))
                                                      state)]
                         (assoc state
                           ::pl/emotion-happiness new-happiness
                           ::pl/done (neg? new-happiness))))]
    (if (= line [:hello])
      (update-state state (::pl/expectation-greeting state))
      (update-state state -0.2))))

(s/def sue-fred-hear ::pl/hear)


;; Sue always says [:hello].
(def sue {::pl/get-initial-state (fn [] pl/default-initial-party-state)
          ::pl/hear              sue-fred-hear
          ::pl/speak             (fn [state] [[:hello] state])})

(s/def ::anyone-already-spoke boolean?)

;; Fred is smart enough to say [:hello] if he speaks first, but then always says [:huh].
(def fred {::pl/get-initial-state (fn [] (assoc pl/default-initial-party-state ::anyone-already-spoke false))
           ::pl/hear              (fn [state line]
                                    (assoc (sue-fred-hear state line)
                                      ::anyone-already-spoke true))
           ::pl/speak             (fn [state]
                                    [(if (::anyone-already-spoke state)
                                       [:huh]
                                       [:hello])
                                     (assoc state ::anyone-already-spoke true)])})

(defn- approx= [^Double x ^Double y]
  (< (Math/abs ^Double (- x y)) 1e-4))

;; If Fred goes first, we expect the following chat:
;; Fred says [:hello]. Sue bumps her happiness to 1.0 and drops her expectation-greeting to 0.0.
;; Sue says [:hello]. Fred bumps his happiness to 1.0 and drops his expectation-greeting to 0.0.
;; Fred says [:huh]. Sue drops her happiness to 0.8 and her expectation-greeting to -1.0.
;; Sue says [:hello]. Fred leaves his happiness at 1.0 and drops his expectation-greeting to -1.0.
;; Fred says [:huh]. Sue drops her happiness to 0.6.
;; Sue says [:hello]. Fred drops his happiness to 0.0.
;; Fred says [:huh]. Sue drops her happiness to 0.4.
;; Sue says [:hello]. Fred drops his happiness to -1.0 and we are done.
(deftest fred-sue-chat
  (let [result (pl/score-chat fred sue 20)]
    (let [[fred-happiness sue-happiness] result
          msg (str "result was " result)]
      (is (approx= fred-happiness -1.0) msg)
      (is (approx= sue-happiness 0.4) msg))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise morphics more thoroughly against the pleasantries framework.

(s/assert ::m/formation (m/get-formation ::pl/party-formation-1))


