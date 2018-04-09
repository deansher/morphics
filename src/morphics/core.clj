(ns morphics.core
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [with-test is]]))

(s/def ::team-spec-keyword keyword?)

(s/def ::mission-id (s/keys :req [::team-spec-keyword]))

(s/def ::formation-keyword keyword?)

(s/def ::formation-id (s/keys :req [::formation-keyword]))

(s/def ::resources-spec-keyword keyword?)

(s/def ::resources-id (s/keys :req [::resources-spec-keyword]))

(s/def ::formation-keys (s/keys :req [::formation-id
                                      ::mission-id
                                      ::duty-handlers]
                                :opt [::resources-id]))

(defn valid-formation? [formation] "do some validation")

(s/def ::formation (s/and ::formation-keys
                          valid-formation?))

(defonce ^:private keyword-to-formation-ref (atom {}))

(defn get-formation [keyword] (keyword @keyword-to-formation-ref))

(s/fdef get-formation
        :args (s/cat :keyword keyword?)
        :ret ::formation)

(defn o>
  "Look up key in map to retrieve a function that is applied to rest"
  [map key & rest]
  (apply (get map key) rest))

(s/fdef o> :args (s/& (s/cat :map map?
                             :key keyword?
                             :rest (s/* any?))
                      (fn [{:keys [map key]}] (fn? (get map key)))))

(defn def-formation
  [[formation-keyword team-spec-keyword resources-spec-keyword]
   & {:as duty-handlers}]
  (let [formation
        (conj {
               ::formation-id  {::formation-keyword formation-keyword}
               ::mission-id    {::team-spec-keyword team-spec-keyword}
               ::duty-handlers duty-handlers
               }
              (when resources-spec-keyword
                [::resources-id {::resources-spec-keyword resources-spec-keyword}]))]
    (do
      (swap! keyword-to-formation-ref assoc formation-keyword formation)
      nil)))

(s/fdef def-formation
        :args (s/cat :team-and-resource (s/and vector?
                                               (s/cat :formation-keyword keyword?
                                                      :team-spec-keyword keyword?
                                                      :resource-spec-keyword (s/? keyword?)))
                     :duty-handlers (s/* (s/cat :duty-name keyword?
                                                :duty-handler fn?)))
        :ret nil?)
