(ns morphics.examples.pleasantries
  "A toy morphics application that mimics getting-to-know-you pleasantries"
  (:require
    [clojure.core]
    [clojure.spec.alpha :as s]
    [morphics.core :as m]
    [clojure.set :as set]))

(def party-names
  #{:fred :sue :john :sally})

(def party-vocabulary
  (set/union party-names #{:hello :i :love :me :hi :huh :ok}))

(s/def ::done boolean?)

;; Expectations in [-1.0, 1.0] by convention, but other values are ok.
(s/def ::expectation-greeting double?)
(s/def ::expectation-tell-me-your-name double?)

;; Emotions in [-1.0, 1.0] by convention, but other values are ok.
(s/def ::emotion-happiness double?)
(s/def ::emotion-belonging double?)
(s/def ::emotion-irritation double?)
(s/def ::emotion-boredom double?)

(s/def ::party-state (s/keys :req [::done
                                   ::expectation-greeting
                                   ::expectation-tell-me-your-name
                                   ::emotion-happiness
                                   ::emotion-belonging
                                   ::emotion-irritation
                                   ::emotion-boredom]))
(def default-initial-party-state
  {
   ::done                          false
   ::expectation-greeting          1.0
   ::expectation-tell-me-your-name -1.0
   ::emotion-happiness             0.0
   ::emotion-belonging             0.0
   ::emotion-irritation            0.0
   ::emotion-boredom               0.0})


(s/def ::variable-initial-party-state-fields (s/keys :req [::emotion-happiness
                                                           ::emotion-belonging
                                                           ::emotion-irritation
                                                           ::emotion-boredom]))

(s/def ::line (s/every party-vocabulary :kind vector?))

(defprotocol Party
  "represents a party in a chat"
  (get-initial-state* [party])
  (hear* [party state line])
  (speak* [party state]))

(s/def ::party (partial satisfies? Party))

(defn get-initial-state
  "return this party's initial state"
  [party]
  (get-initial-state* party))

(s/fdef get-initial-state
        :args (s/cat :party ::party)
        :ret ::party-state)

(defn hear
  "while in the given state, hear the line and return an updated state"
  [party state line]
  (hear* party state line))

(s/fdef hear
        :args (s/cat :party ::party
                     :state ::party-state
                     :line ::line)
        :ret ::party-state)

(defn speak
  "while in the given state, return a line to speak and a new state, or nil if nothing to say"
  [party state]
  (speak* party state))

(s/fdef speak
        :args (s/cat :party ::party
                     :state ::party-state)
        :ret (s/nilable (s/cat :line ::line
                               :state ::party-state)))

(s/def ::party-and-state (s/keys :req [::party ::party-state]))

(defn- flip-if
  "if b, then return the pair in reverse order"
  [b [x y]]
  (if b [y x] [x y]))

(defn score-chat*
  "Conduct a chat between the two parties, giving speaking-party the first chance to speak,
   and continuing for up to max-interactions (where an interaction is one line spoken and heard)
   or until either party's state has ::done of true.

   Return a vector of two doubles indicating the resulting happiness of the two parties."
  [{speaking-party ::party, speaking-party-state ::party-state}
   {listening-party ::party, listening-party-state ::party-state}
   speaker-is-party1
   max-interactions]
  (if (or (<= max-interactions 0)
          (::done speaking-party-state)
          (::done listening-party-state))
    (flip-if (not speaker-is-party1)
             [(::emotion-happiness speaking-party-state)
              (::emotion-happiness listening-party-state)])
    (let [[line speaking-party-state] (speak speaking-party speaking-party-state)
          listening-party-state (hear listening-party listening-party-state line)]
      (recur {::party listening-party ::party-state listening-party-state}
             {::party speaking-party ::party-state speaking-party-state}
             (not speaker-is-party1)
             (dec max-interactions)))))

(s/fdef score-chat*
        :args (s/cat :speaker ::party-and-state
                     :listener ::party-and-state
                     :speaker-is-party1 boolean?
                     :max-interactions integer?)
        :ret (s/cat :party1-happiness double?
                    :party2-happiness double?))

(defn score-chat [party1 party2 max-interactions]
  (score-chat* {::party party1 ::party-state (get-initial-state party1)}
               {::party party2 ::party-state (get-initial-state party2)}
               true
               max-interactions))

(s/fdef score-chat
        :args (s/cat :party1 ::party
                     :party2 ::party
                     :max-interactions integer?)
        :ret (s/cat :party1-happiness double?
                    :party2-happiness double?))

(defprotocol Listener
  (listener-hear* [listener state line]))

(s/def ::listener (partial satisfies? Listener))

(defn listener-hear [listener state line]
  "Process line and return a new state."
  (listener-hear* listener state line))

(s/fdef listener-hear
        :args (s/cat :listener ::listener
                     :state ::party-state
                     :line ::line)
        :ret ::party-state)

(defprotocol Speaker
  (speaker-impulse* [speaker state])
  (speaker-speak* [speaker state]))

(s/def ::speaker (partial satisfies? Speaker))

(defn speaker-impulse [speaker state]
  "Return a positive double representing how strongly this speaker would like to speak"
  (speaker-impulse* speaker state))

(s/fdef speaker-impulse
        :args (s/cat :speaker ::speaker
                     :state ::party-state)
        :ret (s/and double? pos?))

(defn- speaker-reduction-step [state]
  (fn [[highest-impulse highest-impulse-speaker] speaker]
    (let [new-impulse (speaker-impulse speaker state)]
      (if (> new-impulse highest-impulse)
        [new-impulse speaker]
        [highest-impulse highest-impulse-speaker]))))

(s/fdef speaker-reduction-step
        :args (s/cat :state ::party-state)
        :ret (s/fspec :args (s/cat :reduction-state (s/tuple double? ::speaker)
                                   :speaker ::speaker)
                      :ret (s/tuple double? ::speaker)))

(defn speaker-speak [speaker state]
  "while in the given state, return a line to speak and a new state, or nil if nothing to say"
  (speaker-speak* speaker state))

(defrecord ListenersAndSpeakers [initial-state-overrides listeners speakers]
  Party
  (get-initial-state* [_] (merge default-initial-party-state
                                 initial-state-overrides))
  (hear* [_ state line] (let [tell-listener
                              (fn [state listener] (listener-hear listener state line))]
                          (reduce tell-listener state listeners)))
  (speak* [_ state] (let [[_ speaker]
                          (reduce (speaker-reduction-step state)
                                  [Double/NEGATIVE_INFINITY nil]
                                  speakers)]
                      (when speaker
                        (speaker-speak speaker state)))))





