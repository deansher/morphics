(ns morphics.examples.pleasantries
  "A toy morphics application that mimics getting-to-know-you pleasantries"
  (:require
    [clojure.core]
    [clojure.spec.alpha :as s]
    [morphics.core :as m :refer [o>]]
    [clojure.set :as set]))

(def party-names
  #{:fred :sue :john :sally})

(def party-vocabulary
  (set/union party-names #{:hello :i :love :me :hi :huh :ok}))

(s/def ::done boolean?)

;; Expectations typically in [-1.0, 1.0]
(s/def ::expectation-greeting double?)
(s/def ::expectation-tell-me-your-name double?)

;; Emotions typically in [-1.0, 1.0]
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
   ::emotion-boredom               0.0
   })

(s/def ::variable-initial-party-state-fields (s/keys :req [::emotion-happiness
                                                           ::emotion-belonging
                                                           ::emotion-irritation
                                                           ::emotion-boredom]))

(s/def ::line (s/every party-vocabulary :kind vector?))

(s/def ::get-initial-state (s/fspec :args empty?
                                    :ret ::party-state))

(s/def ::hear (s/fspec :args (s/cat :state ::party-state
                                    :line ::line)
                       :ret ::party-state))

(s/def ::speak (s/fspec :args (s/cat :state ::party-state)
                        :ret (s/nilable (s/cat :line ::line
                                               :state ::party-state))))

(s/def ::party (s/and ::m/team
                      (s/keys :req [::get-initial-state
                                    ::hear
                                    ::speak])))

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
    (let [[line speaking-party-state] (o> speaking-party ::speak speaking-party-state)
          listening-party-state (o> listening-party ::hear listening-party-state line)]
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
  (score-chat* {::party party1 ::party-state (o> party1 ::get-initial-state)}
               {::party party2 ::party-state (o> party2 ::get-initial-state)}
               true
               max-interactions))

(s/fdef score-chat
        :args (s/cat :party1 ::party
                     :party2 ::party
                     :max-interactions integer?)
        :ret (s/cat :party1-happiness double?
                    :party2-happiness double?))

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
                      (o> speaker ::speak state))))
  )



