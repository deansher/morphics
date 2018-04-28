# morphics

Morphics is a different approach to constructing software that makes it more malleable and more
evolvable. A morphic software component is a stateless hierarchy of pure functions and immutable
data. The interface to a morphic component is defined by a top-level set of pure functions. The
implementations of these functions pin down certain aspects of the component's behavior, but they
commonly delegate important behavioral decisions to deeper levels of the hierarchy.

The difference between this approach and a conventional component hierarchy is conceptually subtle
but makes a large practical difference. A conventional software component implements one specific
algorithm (although possibly a complex algorithm like machine learning). It relies on subordinate
components to handle details of that algorithm or to communicate with the outside world, but the
author of the higher-level component thinks of those subordinate components as being constrained by
their APIs to provide specific, well-understood behaviors. A morphic software component instead
implements a high-level strategy for solving a problem, while delegating meaningful decisions about
the solution to lower-level components. As a matter of style rather than machinery, the API to a
morphic subcomponent specifies mainly the goals rather than the method of achieving them.

The hierarchy of functions and data that comprise a morphic component is constructed at runtime. The
constraints that guide morphic component construction are exposed at runtime in both human-readable
and machine-readable ways, so that morphic components can be assembled at runtime by programmers,
non-programmers, and machine learning algorithms.

When defining the constraints, APIs, and implementations of morphic components, the programmer's
goal is to define a space of possible program behaviors, rather than pinning down one specific
behavior.  All of the behaviors in this space to be sane -- in the sense of not causing damaging
runtime behavior like infinite loops, excessive resource utilization, or data structure
corruption. The programmer's goal is to make the overall space of behaviors dense in useful
solutions for a particular problem domain. This allows a future programmer, non-programmer, or
machine learning algorithm to construct a specific morphic component hierarchy that solves a problem
at hand. Morphics can be seen as a style of defining DSLs that are more declarative than most, and
that are more usable by both non-programmers and machine learning algorithms.

A morphic component's runtime structure follows many of the same patterns as newer object-oriented
languages like Go and Swift that emphasize programming to interfaces and that deemphasize or
disallow inheritance. The top-level functions of a morphic component are isomorphic to the methods
of an interface in an object-oriented language. The runtime hierarchy of functions and data that
comprise a morphic component is isomorphic to an object with its instance variables, when we observe
that OO instance variables may themselves be objects or (in some languages) may be other data
structures that contain objects. The difference between morphics and OO programming is largely a
difference in style, ecosystem, and tooling, rather than a difference in runtime machinery. Even so,
the resulting difference in capability is profound.

## Morphic Terminology

The isomorphs between morphics and object-oriented programming create a dilemma for implementing and
describing a morphic framework. If we use familiar terms like "object", "interface", and "method",
we may be easily pulled into familiar OO patterns. But if we create new terminology for morphics, it
may seem as though we are making up new words for old ideas and claiming to to have thus invented
something new. We choose to accept that latter risk, creating new terminology for morphics that may
help us think in new ways. So, off we go.

A morphic software component is called a _team_. A team's API is defined by a _mission_. A team's
top-level implementation of its mission is defined by a _formation_. The relationship between
missions and formations is one to many: any number of alternative formations may carry out the same
mission. (Missions and formations are isomorphic to OO interfaces and classes, except without
notions of public instance variables or implementation inheritance.)

A formation can carry out its mission in a traditional, hardcoded way. Often, however, a formation
deliberately provides just a high-level strategy for carrying out its mission and leaves the details
to be filled in later. As a real-world analogy that also provides motivation for the unusual
terminology, consider American football. A football team finds itself in different situations that
imply different tactical missions. We can imagine these missions in simple terms like "gain a few
yards safely", "gain at least 8 yards", "kick a field goal", etc. To achieve any given mission, the
coach can choose from many different formations. The choice of formation doesn't pin down every
detail of the play. Much is left open to the situation on the field and the preferences and
reactions of the players.

The detailed behavior of a morphic team is defined by the team's _resources_, which are arbitrary
data including subordinate teams (_subteams_). A team's resources are intended to be
immutable, so that a team is a stateless. A team may implement pure computation or it may have side
effects such as I/O or changes to global state.

## Conceptual Data Model

A mission (analogous to an OO interface) is identified by a _mission id_.  The mission specifies a
set of _duties_, which are named functions (analogous to OO methods). The API of each duty is
specified by a _duty spec_ (analogous to a method's type). The term "duty" instead of "method" is
intended to emphasize that a duty spec should be framed more in terms of goals than methods of
achieving them.

A mission is implemented by a _team_ (analogous to an OO object with its instance variables). A team
is assembled at runtime from the detailed team description provided by a _charter_ (analogous to
a serialized object).

The top-level runtime structure of a team is defined by a _formation_ (analogous to an OO class),
which is identified by a _formation id_ and which implements a particular mission.  A formation
has _resources_, which are named values (analogous to instance variables).

A formation implements each of its duties with a _duty handler_ (analogous to an OO method
implementation). A duty handler is a function whose first parameter is the team's resources and
whose remaining parameters are those defined in the duy spec.

To summarize, a formation specifies the following:
* formation id
* mission id
* resource spec
* mapping from duty name to duty handler

A charter specifies how to instantiate a hierarchy of formations. It specifies the following:
* formation id
* resources, which commonly include subteams that are specified by subcharters

## Clojure Representation of the Data Model

All keywords and symbols are name-spaced.

| Concept | Representation |
| ------ | ------------ |
| mission | `spec-tools.spec/Spec` |
| mission id | mostly fades away, but corresponds in some cases to a spec keyword or a multi-method hierarchy key |
| team | value that conforms to a mission spec |
| resources | implicitly embedded in the team |
| resources spec | implicitly embedded in the mission spec |
| formation | `spec-tools.spec/Spec` for an arbitrary Clojure value |
| formation id | mostly fades away, but corresponds in some cases to a multi-method hierarchy key |
| charter | a Clojure value that conforms to a mission spec; represented externally as EDN |

## Important Morphic Operations

### Validating a Charter

### Assembling a Team from a Charter

### Creating a Random Charter for a Mission

### Mating Two Parent Charters




