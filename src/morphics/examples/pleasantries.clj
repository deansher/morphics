(ns morphics.examples.pleasantries
  "A toy morphics application that mimics getting-to-know-you pleasantries"
  (:require 
   [clojure.core]
   [clojure.spec.alpha :as s]
   [morphics.core :refer [oo]]))

(s/def ::done boolean?)

(s/def ::happiness double?)

(s/def ::party-state (s/keys :req [::done
                                   ::happiness]))
  
(s/def ::initial-state (s/fspec :args empty?
                                :ret ::party-state))

(s/def ::line (s/* keyword?))

(s/def ::opening-line (s/fspec :args (s/cat :state ::party-state)
                               :ret (s/cat :line ::line
                                           :new-state ::party-state)))

(s/def ::reply (s/fspec :args (s/cat :state ::party-state 
                                     :line ::line)
                        :ret (s/cat :line ::line
                                    :new-state ::party-state)))

(s/def ::party (s/keys :req [::initial-state 
                             ::opening-line 
                             ::reply]))

(defn- score-chat*
  [[party1 state1]
   [party2 state2]
   max-interactions   
   is-first-round
   previous-party2-line]
  (if (or (<= max-interactions 0)
          (::done state1)
          (::done state2))
    [(::happiness state1) 
     (::happiness state2)]
    (let [[party1-line state1] (if is-first-round
                                 (oo party1 ::opening-line state1)
                                 (oo party1 ::reply state1 previous-party2-line))
          [party2-line state2] (oo party2 ::reply state2 party1-line)]
      (recur [party1 state1]
             [party2 state2]
             (- max-interactions 1)
             false
             party2-line))))

(s/fdef score-chat*
        :args (s/cat :party1-info (s/cat :party1 ::party, :state1 ::party-state)
                     :party2-info (s/cat :party2 ::party, :state2 ::party-state)
                     :max-interactions integer?
                     :is-first-round boolean?
                     :previous-party2-line ::line)
        :ret (s/cat :party1-happiness double?
                    :party2-happiness double?))

(defn- score-chat
  "Give the two parties a chance to chat.
   Return a vector of two doubles indicating the resulting happiness of the two parties."
  [party1 party2 max-interactions]
  (score-chat* [party1 (oo party1 ::initial-state)]
               [party2 (oo party2 ::initial-state)]
               max-interactions
               true
               nil))

(s/fdef score-chat
        :args (s/cat :party1 ::party 
                     :party2 ::party 
                     :max-interactions integer?)
        :ret (s/cat :party1-happiness double?
                    :party2-happiness double?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; example instances

(def sue {::initial-state (fn [] {::done false, ::happiness 1.0})
          ::opening-line (fn [state] [[:hello] state])
          ::reply (fn [state line] [[:i :dont :know] state])})

(defn fred-sadder-reply [line state sadness]
  (let [new-state (update state ::happiness #(- % sadness))]
    (if (>= 0.0 (::happiness new-state))
      [line new-state]
      [[:go :away] (assoc new-state :done true)])))

(def fred {::initial-state (fn [] {::done false, ::happiness 0.0, ::is-first-line true })
           ::opening-line (fn [state] [[:hey] state])
           ::reply (fn [state line] 
                     (let [new-state (assoc state ::is-first-line false)]
                       (if (= line [:hello])
                         (if (::is-first-line state)
                           [[:hello :to :you] (update new-state ::happiness #(+ % 1.0))]
                           (fred-sadder-reply nil new-state 0.2))
                         (fred-sadder-reply (concat [:why] line) new-state 0.1))))})
