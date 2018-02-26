# morphics

Morphics is a different approach to constructing software. A morphic software component is constructed at runtime by
assembling a hierarchy of subcomponents. The API for a morphic component is defined by a _mission_. For any given
mission, the morphic runtime knows a set of alternative implementations called _formations_. Each formation defines
a different high-level approach to carrying out the mission.

As a real-world analogy that also provides motivation for the unusual terminology, consider American football.
Each play has a mission. We can imagine these missions in simple terms like "safe short yardage gain", "risky
long yardage gain", "field goal", etc. To achieve any given mission, a team could choose to line up in any of
several formations.

In our football analogy, the choice of formation doesn't pin down every detail of the play. Much is left open to the
situation on the field and the preferences and reactions of the players. Similarly, the top-level formation of a morphic
component only provides an outline of how that component will function. Many details are left to be filled in.

Because it is a software paradigm, morphics uses a regular recursive structure to fill in these details. Each morphic
formation specifies a list of _roles_, where each role has its own mission. A formation must be chosen to implement
each of these missions. A morphic component is constructed at runtime by assembling a hierarchy of formations.

This is fundamentally different from how most software approaches abstraction and decomposition. We are used to a
software component providing an API that specifies exactly what behavior its client can expect, while hiding
implementation details that the client doesn't care about. But we are used to each software component implementing one,
specific, fixed behavior. A morphic formation does implement a specific API: the mission. But it only provides
an abstract, high-level implementation of the mission. It delegates more detailed implementation decisions to its
subordinate roles. This allows programmers, end-users, or even machine learning algorithms to snap together alternative
implementations that achieve a chosen mission in different ways.

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



