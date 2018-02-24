# morphics

Morphics is a different approach to program construction.

## Conceptual Data Model

The basic morphic runtime component is the _team_. A team's external interface is defined by a
_mission_.

A mission specifies a set of _duties_, which are named functions. The API of each duty is specified
by a _duty spec_. Each mission has a _mission name_.

A team has _resources_, which are named values. The team's resource API is defined by
a _resource spec_, which specifies a name and API for each resource. One special kind of resource is
the _subteam_. A subteam's resource name is considered a _role name_. A subteam's API is defined by
a mission name.

Each of a team's duties is implemented by a _duty handler_, which is a function whose first
parameter is the team's resources, and whose remaining parameters match the duty spec.

The top-level implementation structure of a team is defined by a _formation_, which is identified by a
_formation name_ and specifies the
following:
* mission name
* resource spec
* duty handler for each duty specified in the mission

A team is constructed from a _charter_, which specifies the following:
* formation name
* charter for each subteam role

## Clojure Representation of the Data Model

All keywords and symbols are namespaced.

| Concept | Representation |
| ------ | ------------ |
| team | map from duty name to function |
| duty name | keyword |
| duty spec | function spec |
| mission | map spec where every value is a duty spec |
| mission name | keyword identifier of the mission's map spec |
| resources | map from keyword to any Clojure value (sometimes from role name to team) |
| role name | keyword |
| resource spec | a map where `:data` is a map spec for non-subteam resources and `:roles` is a list of role definitions |
| role definition | a map from role name to mission name |
| formation | a map specified by `morphics.org/formation` |
| formation name | symbol that evaluates to a formation |

## Important Morphic Operations


