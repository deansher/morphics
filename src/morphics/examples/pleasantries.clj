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
  (get-initial-state* [party] "return this party's initial state")
  (hear* [party state line] "while in the given state, hear the line and return an updated state")
  (speak* [party state] "while in the given state, return a line to speak and a new state, or else nil"))

(s/def ::party #(satisfies? Party %))

(defn get-initial-state
  "Instrumentable wrapper for get-initial-state*"
  [party]
  (get-initial-state* party))

(s/fdef get-initial-state
        :args (s/cat :party ::party)
        :ret ::party-state)

(defn hear
  "Instrumentable wrapper for hear*"
  [party state line]
  (hear* party state line))

(s/fdef hear
        :args (s/cat :party ::party
                     :state ::party-state
                     :line ::line)
        :ret ::party-state)

(defn speak
  "Instrumentable wrapper for speak*"
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

#_(

    (s/def ::listener (s/and ::team
                             (s/keys :req [::hear])))

    (s/def ::listeners (s/* ::listener))

    (s/def ::speaking-impulse (s/fspec :args (s/cat :state ::party-state)
                                       :ret (s/double-in :min 0.0 :NaN? false)))

    (s/def ::speaker (s/and ::team
                            (s/keys :req [::speaking-impulse
                                          ::speak])))

    (s/def ::speakers (s/* ::speaker))

    (s/def ::party-formation-1-resources
      (s/keys :req [::variable-initial-party-state-fields
                    ::listeners
                    ::speakers]))

    (defn- speaker-reduction-step [state]
      (fn [[highest-impulse highest-impulse-speaker] speaker]
        (let [new-impulse (o> speaker ::speaking-impulse state)]
          (if (> new-impulse highest-impulse)
            [new-impulse speaker]
            [highest-impulse highest-impulse-speaker]))))

    (s/fdef speaker-reduction-step
            :args (s/cat :state ::party-state)
            :ret (s/fspec :args (s/cat :reduction-state (s/tuple double? ::speaker)
                                       :speaker ::speaker)
                          :ret (s/tuple double? ::speaker)))

    (m/def-formation
      [::party-formation-1 ::party ::party-formation-1-resources]
      ::get-initial-state
      (fn [res] (merge default-initial-party-state
                       (::variable-initial-party-state-fields res)))
      ::hear
      (fn [res state line] (let [tell-listener
                                 (fn [state listener] (o> listener ::hear state line))]
                             (reduce tell-listener state (::listeners res))))
      ::speak
      (fn [res state] (let [[_ speaker]
                            (reduce (speaker-reduction-step state)
                                    [Double/NEGATIVE_INFINITY nil]
                                    (::speakers res))]
                        (when speaker
                          (o> speaker ::speak state)))))

    )




