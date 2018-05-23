(ns morphics.mission
  "Implements \"missions\", which are type-like descriptions of values. Unlike `clojure.spec.alpha`,
  which focuses on describing a value's data shape, missions focus on describing a value's usage and behavior.

  Every clojure value has a \"mission tag\", which is intended to be a good tag for the Clojure hierarchy system
  (see `clojure.core/isa?`). This allows multi-methods to be naturally dispatched on a value's
  mission tag. If we think of a mission as being like a type, then the corresponding mission tag is the
  type with all parameters erased. It captures a mission's category -- its place in the hierarchy -- but no
  further nuance.

  A mission is represented by a map. There are two required keys for this map:

  * `morphics.mission/for-tag` specifies the mission tag for values that implement the mission.
    That is, it is the mission tag that corresponds to the mission.

  * `morphics.mission/tag` has the value `:morphics.mission/mission`.
    This indicates that the mission's own mission is to be a mission.

  Missions may have additional keys that further restrict the range of possible values. The set
  of meaningful keys depends on the mission tag. All additional keys must be optional."
  (:require [com.rpl.specter :refer :all]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::tag (s/with-gen (s/or :ident (s/and ident? namespace)
                               :class (partial instance? Class))
                         #(s/gen #{::test-tag 'mission/test-tag String})))

;; Mission tag for values described by a mission.
(s/def ::for-tag ::tag)

(s/def ::mission (s/and (s/keys :req [::tag ::for-tag])
                        #(-> % ::tag (= ::mission))))

(defprotocol HasMissionTag
  "A Java type that implements a mission tag different than the type.  See `morphics.mission/tag`."
  (mission-tag* [x]
    "Returns the mission tag of `x`, or nil to not provide one."))

(defprotocol HasMission
  "A Java type that can report its own mission."
  (mission* [x]
    "Returns the mission for `x`."))

(s/def ::tag-fn (s/fspec :args (s/cat :m map?)
                         :ret (s/nilable ::tag)))

;; Given a key that may be present in a map, contains a function
;; from the map to a mission tag or nil.
(defonce ^:skip-wiki key->tag-fn (atom {}))

;; Given a key, a set of other keys over which it is preferred, for the purpose
;; of inferring a mission tag.
(defonce ^:skip-wiki key->preferred-over-keys (atom {}))

;; Given a mission tag, a set of known constructor symbols.
(defonce ^:skip-wiki tag->constructor-syms (atom {}))

;; Given a mission tag, a set of functions from mission to spec.
(defonce ^:skip-wiki tag->spec-fns (atom {}))

(defn def-tag-fn
  "For a key that may be present in a map, provide a function `tag-fn` from such a
  map to the map's mission tag.  `tag-fn` may return nil if it is unable to determine
  the mission tag.

  These key functions are used only for maps
  that lack a `:morphics.mission/tag` key (see `morphics.mission/tag`).
  The purpose of these key functions is to allow existing map data shapes
  (to when it is impractical to add a `:morphics.mission/tag` key)
  to participate in mission-tag-based dispatching.

  If just a tag function is provided, it is also treated as the key. This is convenient
  when the map's value for a keyword is the tag."
  ([tag-fn] (def-tag-fn tag-fn tag-fn))
  ([key tag-fn]
   (if (= key ::tag)
     (throw (ex-info (str "cannot override the tag function for " ::tag) {}))
     (setval [ATOM key] tag-fn key->tag-fn))))

(s/fdef def-tag-fn
        :args (s/or :arity-1 (s/cat :tag-fn (s/and ::tag-fn
                                                   (partial not= ::tag)))
                    :arity-2 (s/cat :key any?
                                    :tag-fn (s/and ::tag-fn
                                                   (partial not= ::tag)))))

(defn prefer-key
  "Given two keys that might each be used to infer a mission tag,
  indicate that one should be used in preference to the other."
  [preferred-key over-key]
  (setval [ATOM preferred-key NIL->SET NONE-ELEM] over-key key->preferred-over-keys))

(defn ^:skip-wiki set-preferred-over-keys
  "Internal for testing: set key to be preferred over exactly over-keys"
  [preferred-key over-keys]
  (setval [ATOM preferred-key] over-keys key->preferred-over-keys))

(defn ^:skip-wiki undominated-keys
  "Internal: If one of the keys is preferred over all the others, then return it.
  Else throw an exception."
  [keys]
  (let [kpo         @key->preferred-over-keys
        dominated   (mapcat (partial get kpo) keys)
        undominated (set/difference (set keys)
                                    (set dominated))]
    (if (and (seq keys)
             (empty? undominated))
      (throw (ex-info "There is a `prefer-key` loop among these keys." {:keys keys}))
      undominated)))

(defn ^:skip-wiki infer-map-tag
  "Internal: determine the mission tag of a map."
  [m]
  (let [k->tf          @key->tag-fn
        get-tag        (fn [key]
                         (if-let [tag-fn (get k->tf key)]
                           (tag-fn m)))
        candidate-keys (filter get-tag (keys m))
        candidate-keys (if (> (count candidate-keys) 1)
                         (undominated-keys candidate-keys)
                         candidate-keys)
        candidate-tags (map get-tag candidate-keys)]
    (condp = (count candidate-tags)
      0 nil
      1 (first candidate-tags)
      2 (throw (ex-info "Multiple keys could be used to determine the mission tag for this map.
      Consider using `morphics.mission/prefer-key` to disambiguate."
                        {:map m :candidate-keys candidate-keys})))))

(defn tag [x]
  "Determine the mission tag of an arbitrary value x.
  The following approaches are tried in order:
  * If x implements behavior/HasMissionTag, then try the mission-tag* method.
  * If x is a map, then
    * First look for `:morphics.mission/tag`.
    * Otherwise look for map keys defined by behavior/defclass.
  * If none of these yield a non-nil mission tag, use the value's type."
  (or (when (clojure.core/satisfies? HasMissionTag x)
        (mission-tag* x))
      (when (map? x)
        (if-let [tag (::tag x)]
          tag
          (infer-map-tag x)))
      (type x)))

(s/fdef tag
        :args (s/cat :x any?)
        :ret ::tag)

(defmulti mission
          "Infer a value's mission. At minimum, this returns a mission that has
          the value's mission tag as its `morphics.mission/for-tag`."
          tag)

(s/fdef mission
        :args (s/cat :v any?)
        :ret ::mission)

(defn add-constructor
  "Declare that `constructor-sym` is a constructor for the specified mission `tag`."
  [tag constructor-sym]
  (setval [ATOM tag NIL->SET NONE-ELEM]
          constructor-sym
          tag->constructor-syms))

(s/fdef add-constructor
        :args (s/cat :tag ::tag
                     :constructor-sym symbol?))

(defmulti implements?
          "Inexpensive test of whether `x` implements `mission`."
          (fn [mission _] (::for-tag mission)))

(defmethod implements? :default
  [mission v]
  (isa? (tag v) (::for-tag mission)))

(defn add-spec-fn
  "For the specified mission `tag`, add a function from mission to spec (see `clojure.spec.alpha`).
  See `behavior.core/make-spec`."
  ([tag spec-fn]
   (setval [ATOM tag NIL->SET NONE-ELEM]
           spec-fn
           tag->spec-fns)))

(defn ^:skip-wiki constructor-sym->fn
  "Internal: Look up the function associated with `sym`. Throws an exception if none found."
  [sym]
  (let [v (find-var sym)]
    (if-not v
      (throw (ex-info "constructor symbol does not identify a var" {:symbol sym})))
    (let [f (deref v)]
      (if-not (fn? f)
        (throw (ex-info "value of constructor var is not a function" {:var v :value f})))
      f)))

(defn ^:skip-wiki make-gen-from-constructor
  "Internal: If possible, make a generator from the specified `constructor-sym`.
  Otherwise, throw an exception."
  [constructor-sym]
  (let [constructor (constructor-sym->fn constructor-sym)
        spec (s/get-spec constructor-sym)]
    (if-not spec
      (throw (ex-info "constructor has no spec" {:symbol constructor-sym})))
    (let [args-spec (:args spec)]
      (if-not args-spec
        (throw (ex-info "constructor spec has no args" {:symbol constructor-sym :spec spec})))
      (gen/fmap #(apply constructor %) (s/gen args-spec)))))

(defn ^:skip-wiki gens-from-constructors
  "Internal: Create generators from constructors of the mission `tag`.
  Uses constructors that have function specs that have `:args` constructors.
  If tag is *not* a Java class, then this includes constructors of descendants of `tag`.
  Returns a sequence of constructor symbols."
  [tag]
  (let [tag-and-descendants   (conj (descendants tag) tag)
        find-constructor-syms (fn [tag]
                                (select [ATOM tag] tag->constructor-syms))
        constructor-syms      (mapcat find-constructor-syms tag-and-descendants)]
    (map make-gen-from-constructor constructor-syms)))

(defn mission-specs [mission]
  (let [spec-fns (select [ATOM (::for-tag mission) ALL] tag->spec-fns)]
    (map #(% mission) spec-fns)))

;; https://stackoverflow.com/questions/9273333/in-clojure-how-to-apply-a-macro-to-a-list
(defmacro functionize [macro]
  `(fn [& args#] (eval (cons '~macro args#))))

(defn implements
  "Constructs a spec that verifies the behavior specified for `mission`. At minimum,
  this spec verifies that `morphics.mission/tag` returns a tag that
  `clojure.core/isa?` the one specified by `mission` (as `:morphics.mission/for-tag`).

  If spec functions have been registered for `mission`s `for-tag` or ancestors of it,
  the constructed spec also verifies the specs returned by those functions.

  If at least one constructor has been declared for `mission`s `for-tag` or a descendant
  (see `morphics.mission/add-constructor`), and if that constructor has an fspec with args
  that can be generated, then the spec returned from this function will have a generator that
  chooses and executes a constructor. Note: constructors of descendant tags are *not*
  included if `mission`s `for-tag` is a Java class."
  [mission]
  (let [expected-tag (::for-tag mission)
        result       (fn [x] (= expected-tag (tag x)))

        specs        (mission-specs mission)
        result       (if (seq specs)
                       (apply (functionize s/and) result specs)
                       result)

        gens         (gens-from-constructors expected-tag)
        result       (if (seq gens)
                       (s/with-gen result
                                   (gen/one-of gens))
                       result)]
    result))

(s/fdef implements
        :args (s/cat :mission ::mission)
        :ret (partial satisfies? s/Spec))








