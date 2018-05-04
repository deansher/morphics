(ns morphics.examples.pleasantries-test
  (:require [clojure.test :refer :all]
            [com.rpl.specter :refer :all]
            [morphics.core :as m]
            [morphics.examples.pleasantries :as pl]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as orch]))

(s/check-asserts true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A very simple test case of the pleasantries framework.
;; Uses raw pl/Party implementations of Fred and Sue as pl/Party.

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

(s/fdef sue-fred-hear
        :args (s/cat :state ::pl/party-state
                     :line ::pl/line)
        :ret ::pl/party-state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sue always says [:hello].

(defn- sue-get-initial-state []
  pl/default-initial-party-state)

(defn sue-speak [state]
  [[:hello] state])

(defrecord Sue []
  pl/Party
  (get-initial-state* [_] (sue-get-initial-state))
  (hear* [_ state line] (sue-fred-hear state line))
  (speak* [_ state] (sue-speak state)))

(def sue (->Sue))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fred is smart enough to say [:hello] if he speaks first, but then always says [:huh].

(s/def ::anyone-already-spoke boolean?)

(defn- fred-get-initial-state []
  (assoc pl/default-initial-party-state ::anyone-already-spoke false))

(defn- fred-hear [state line]
  (assoc (sue-fred-hear state line)
    ::anyone-already-spoke true))

(defn- fred-speak [state]
  [(if (::anyone-already-spoke state)
     [:huh]
     [:hello])
   (assoc state ::anyone-already-spoke true)])

(defrecord Fred []
  pl/Party
  (get-initial-state* [_] (fred-get-initial-state))
  (hear* [_ state line] (fred-hear state line))
  (speak* [_ state] (fred-speak state)))

(def fred (->Fred))

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
(defn- validate-fred-sue-chat [f s]
  (let [result (pl/score-chat f s 20)]
    (let [[f-happiness s-happiness] result
          msg (str "result was " result)]
      (is (approx= f-happiness -1.0) msg)
      (is (approx= s-happiness 0.4) msg))))

(deftest fred-sue-chat
  (validate-fred-sue-chat fred sue))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Another Fred/Sue implementation, this time using ListenersAndSpeakers.

(def fred'
  (pl/->ListenersAndSpeakers
    (fred-get-initial-state)
    [(reify pl/Listener
       (listener-hear* [_ state line] (fred-hear state line)))]
    [;; a decoy to test speaker selection
     (reify pl/Speaker
       (speaker-impulse* [_ _] 0.0)
       (speaker-speak* [_ state] [:oops-fred-decoy1-spoke state]))
     ;; the real implementation
     (reify pl/Speaker
       (speaker-impulse* [_ _] 1.0)
       (speaker-speak* [_ state] (fred-speak state)))
     ;; another decoy to test speaker selection
     (reify pl/Speaker
       (speaker-impulse* [_ _] 0.5)
       (speaker-speak* [_ state] [:oops-fred-decoy2-spoke state]))]))

(def sue'
  (pl/->ListenersAndSpeakers
    (sue-get-initial-state)
    [(reify pl/Listener
       (listener-hear* [_ state line] (sue-fred-hear state line)))]
    [;; a decoy to test speaker selection
     (reify pl/Speaker
       (speaker-impulse* [_ _] 0.0)
       (speaker-speak* [_ state] [:oops-sue-decoy1-spoke state]))
     ;; another decoy to test speaker selection
     (reify pl/Speaker
       (speaker-impulse* [_ _] 0.5)
       (speaker-speak* [_ state] [:oops-sue-decoy2-spoke state]))
     ;; the real implementation
     (reify pl/Speaker
       (speaker-impulse* [_ _] 1.0)
       (speaker-speak* [_ state] (sue-speak state)))]))

(deftest fred'-sue'-chat
  (validate-fred-sue-chat fred' sue'))







