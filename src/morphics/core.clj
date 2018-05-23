(ns morphics.core
  (:require [clojure.spec.alpha :as s]
            [mission :as behavior]))

(defn- dispatch-randomly-interpolate
  [behavior-metadata _ _]
  (::behavior/class behavior-metadata))

(defmulti randomly-interpolate
          "Constructs a random value that satisfies `behavior-metadata` and is likely to be between `v1` and `v2`
          in some behavior-class-specific way. `v1` and `v2` must satisfy `behavior-metadata`.

          The intent is that if `v1` and `v2` serve some particular domain-specific purpose,
          then the returned value is likely to serve that purpose. But the behavior class
          is the only hard guarantee."
          dispatch-randomly-interpolate)

(defmethod randomly-interpolate :default [_ v1 v2] (rand-nth [v1 v2]))

(s/fdef randomly-interpolate
        :args (s/cat :behavior-metadata ::behavior/metadata
                     :v1 any?
                     :v2 any?)
        :ret any?
        :fn (fn [{:keys [:behavior-metadata :v1 :v2]}]
              (and (behavior/satisfies? behavior-metadata :v1)
                   (behavior/satisfies? behavior-metadata :v2))))
