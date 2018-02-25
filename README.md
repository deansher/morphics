# morphics

Morphics is a different approach to program construction.

## Conceptual Data Model

Morphic functionality is implemented at runtime by a _team_. A team's external interface is defined by a
_mission_.

A mission specifies a set of _duties_, which are named functions. The API of each duty is specified
by a _duty spec_. Each mission has a _mission name_.

A team has _resources_, which are named values. There are two kind of resources: _data resources_
and _subteams_. The team's data resource API is defined by a _data resource spec_. The subteams are
defined by a list of _role definitions_, which specifies a name and a mission for each role. The data
resource spec and the role definitions together comprise the _resource spec_.

Each of a team's duties is implemented by a _duty handler_, which is a function whose first
parameter is the team's resources, and whose remaining parameters match the duty spec.

The top-level implementation structure of a team is defined by a _formation_, which is identified by a
_formation name_, and which specifies the following:
* mission name
* resource spec
* duty handlers (coverying each duty specified in the mission)

A team is formed at runtime from a _charter_, which specifies how to instantiate a hierarchy
of formations:
* formation name
* data resources
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
| data resources | map map from keyword to any Clojure value |
| resources | data resources merged with a map from role name to subteam |
| resource spec | map with `:morphics/dataResourceSpec`, `:morphics/roleDefinitions` |
| data resource spec | map spec |
| role definitions | a map from role name to mission name |
| role name | keyword |
| formation | map with `:morphics/missionName`, `morphics/resourceSpec`, and `morphic/dutyHandlers`|
| duty handlers | map from duty name to duty handler function |
| formation name | symbol that evaluates to a formation |
| charter | map with `:morphics/formationName`, `:morphics/dataResources`, `:morphics/roleCharters` |

## Important Morphic Operations


