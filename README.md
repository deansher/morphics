# morphics

Morphics is a different approach to constructing software. A morphic software component is a
stateless hierarchy of pure functions and immutable data. The interface to a morphic component is
defined by a top-level set of pure functions that pin down certain aspects of the component's
behavior, but that may delegate important behavioral choices to deeper levels of the hierarchy.  The
hierarchy of functions and data that comprise a morphic component is constructed at runtime. The
constraints and alternatives that guide morphic component construction are exposed at runtime in
both human-readable and machine-readable ways, so that morphic components can be assembled at
runtime by programmers, non-programmers, and machine learning algorithms.

A morphic component's runtime structure follows many of the same patterns as newer object-oriented
languages like Go and Swift that emphasize programming to interfaces and that deemphasize or
disallow inheritance. The top-level functions of a morphic component are isomorphic to the methods
of an interface in an object-oriented language. The runtime hierarchy of functions and data that
comprise a morphic component is isomorphic to an object with its instance variables, which may
themselves be objects or (in some languages) may be other data structures that contain objects. The
difference between morphics and OO programming is largely a difference in style, ecosystem, and
tooling, rather than a difference in runtime machinery. Even so, the resulting difference in
capability is profound.

The programmer's goal when implementing a morphic component is to implement a space of
possible behaviors, rather than one specific behavior. The goal is for all of the behaviors in
this space to be sane -- in the sense of not causing damaging runtime behavior like infinite
loops, excessive resource utilization, or data structure corruption -- and for the overall space of
behaviors to be dense in useful behaviors. This allows a future programmer, non-programmer,
or machine learning algorithm to pin down a concrete behavior that is desired or that proves
useful.

## Morphic Terminology

The isomorphs between morphics and object-oriented programming create a dilemma for implementing and
describing the morphic framework. If we use familiar terms like "object", "interface", and "method",
we may be easily pulled into old ways of thinking about software. But if we don't use familiar
terms, we may feel as though we are making up new words for old ideas and claiming to have new
ideas. The creator of morphics decided to accept that latter risk, creating new terminology for
morphics that may help us think in new ways.

A morphic software component is called a _team_. A team's API is defined by a _mission_. A team's
top-level approach to carrying out its mission is defined by a _formation_. Sometimes only one
formation is available to carry out a given mission, but often many different formations are
available for the same mission.

A formation can specify exactly how to carry out its mission. Often, however, a formation deliberately
provides just a high-level strategy for carrying out its mission and leaves the details to be filled
in later.

As a real-world analogy that also provides motivation for the unusual terminology, consider American
football. A football team finds itself in different situations that imply different missions. We can
imagine these missions in simple terms like "gain a few yards safely", "gain at least 8 yards",
"kick a field goal", etc. To achieve any given mission, a team could choose to line up in any of
several formations.

In our football analogy, the choice of formation doesn't pin down every detail of the play. Much is
left open to the situation on the field and the preferences and reactions of the players. Similarly,
a morphic team's top-level formation often just provides a high-level strategy for achieving the mission.

The more behavior of a morphic team is further specified by the team's _resources_, which are
arbitrary data including subordinate teams (_subteams_).  By convention, a team's resources are
immutable. Hence, by convention, teams stateless.

This is fundamentally different from how most software approaches abstraction and decomposition. We are used to a
software component providing an API that specifies exactly what behavior its client can expect, while hiding
implementation details that the client doesn't care about. But in traditional software decomposition, each software
component implements one specific, fixed behavior. In morphics, each component still implements a specific API: the
mission. But it only provides an abstract, high-level implementation of the mission. It delegates more detailed
implementation decisions to its subordinate roles. This allows programmers, end-users, or even machine learning
algorithms to snap together alternative implementations that achieve a chosen mission in different
ways.

## Conceptual Data Model

The interface to a morphic component is defined by a _mission_. A mission specifies a set of _duties_, which are named
functions. The API of each duty is specified by a _duty spec_. A mission is identified by a _mission name_.

A mission is implemented by a _team_, which is assembled at runtime from the detailed team description provided
by a _charter_.

The top-level runtime structure of a team is defined by a _formation_, which is identified by a _formation name_ and
which implements a particular mission.

A formation has _resources_, which are named values.  There are two kind of resources: _data resources_ and
_subteams_. Data resources can be arbitrary values. The formation's data resource API is defined by a _data resource
spec_. A formation's subteams are defined by a list of _role definitions_; each role definition specifies the role name
and mission. The data resource spec and the role definitions together comprise the _resource spec_.

A formation implements each of its duties with a _duty handler_, which is a function whose first parameter is the team's
resources, and whose remaining parameters match the duty spec.

To summarize, a formation specifies the following:
* formation name
* mission name
* resource spec
* list of duty handlers

A charter specifies how to instantiate a hierarchy of formations. It specifies the following:
* formation name
* data resources
* name and charter for each subteam role

## Relationship to OOP

## ClojureScript Representation of the Data Model

All keywords and symbols are namespaced.

| Concept | Representation |
| ------ | ------------ |
| team | map from duty name to function |
| duty name | keyword |
| duty spec | function spec |
| mission | map spec where every value is a duty spec |
| mission name | keyword identifier of the mission's map spec |
| data resources | EDN map from keyword to arbitrary value |
| resources | data resources merged with subteam resources |
| subteam resources | a map from role name to subteam |
| resource spec | map with `:morphics/dataResourceSpec`, `:morphics/roleDefinitions` |
| data resource spec | map spec |
| role definitions | a map from role name to mission name |
| role name | keyword |
| formation | map with `:morphics/formationName`, `:morphics/missionName`, `morphics/resourceSpec`, and `morphic/dutyHandlers`|
| duty handlers | map from duty name to duty handler function |
| formation name | symbol that evaluates to a formation |
| charter | EDN map with `:morphics/formationName`, `:morphics/dataResources`, `:morphics/roleCharters` |

A _formation registry_ provides a set of known formations for each mission.

## Important Morphic Operations

### Validating a Charter

Take the expected mission name as input.

Verify that the charter's top-level formation implements the expected mission.

Verify that the charter's top-level data resources conform to the formation's data resource spec.

Verify that the charter specifies exactly the subteam roles in the formation's role definitions.

Recursively validate each subteam charter.

### Assembling a Team from a Charter

Take the expected mission name as input.

Validate the charter against the expected mission.

Recurse to assemble a subteam for each subteam role.

Create the top-level formation's resources by starting with its data resources (from the top-level charter) and adding
an entry for each subteam role.

Generate each duty function (from the top-level mission) by partially applying the corresponding duty handler (from the
top-level formation) to the resources. Combine these duty functions into the map that will represent the team.

### Creating a Random Charter for a Mission

Look up the known formations for the charter's top-level mission in the formation registry. Choose a random formation
from that set.

Generate the top-level formation's data resources by running spec gen on the top-level formation's data resource spec.

Recurse to generate a random charter for each subteam role.

Validate the charter.

### Mating Two Parent Charters

Parent charters can only be mated if they have the same top-level formation.

For each data resource in the top-level formation's data resource spec:
* If a _random interpolation_ is known for that resource's spec, then apply it to the parent's values for that resource.
* Otherwise, randomly choose one of the two parent data resources.

For each subteam role:
* If the parents' subteam charters for that role have the same top-level formation, then recurse to mate the two subteam
charters.
* Otherwise, randomly choose one of the two subteam charters.



