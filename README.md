# morphics

Morphics is a different approach to constructing software that makes it more malleable and more
evolvable. A morphic software component is a stateless hierarchy of immutable values, with an
interface comprised of pure functions. (More precisely, it is mostly stateless, mostly immutable,
and the functions are mostly pure. We take the perspective that these are good biases, but that ultimately 
the situation on the ground has to drive the decision. In what follows, each "mostly" is presumed but unsaid.)

The interface to a morphic component is defined by the interface to the top-level value of its
hierarchy of values. The functions that implement this interface pin down certain aspects of the 
component's behavior, but they commonly delegate important behavioral decisions to deeper levels 
of the hierarchy by invoking interfaces of nested values.

The difference between this approach and object-oriented programming is conceptually subtle
but makes a large practical difference. A conventional object or module implements one specific
algorithm (although possibly a complex algorithm with evolving behavior, like machine learning). 
It relies on subordinate objects or modules to handle details of that algorithm or to communicate with 
the outside world. The author of the higher-level code thinks of those subordinate objects or modules 
as being constrained by their interfaces to provide specific, well-understood behaviors. A morphic software 
component instead implements a high-level strategy for solving a problem, while delegating meaningful 
decisions about the solution to lower-level components. As a matter of style rather than machinery, 
the interface to a morphic subcomponent specifies mainly the goals rather than the method of achieving them.

The hierarchy of values that comprise a morphic component is constructed at runtime. The
constraints that guide morphic component construction are exposed at runtime in both human-readable
and machine-readable ways, so that morphic components can be assembled at runtime by programmers,
non-programmers, and machine learning algorithms.

When defining the representations, invariants, and interfaces of morphic values, and when
implementing the interfaces, the programmer's goal is to define a space of possible program 
behaviors rather than pinning down one specific behavior.  All of the behaviors in the defined space 
must be sane, in the sense of not causing damaging runtime behavior like infinite loops, 
excessive resource utilization, or data structure corruption. 
The programmer's goal is to make the overall space of behaviors dense in useful
solutions for a particular problem domain. This allows a future programmer, non-programmer, or
machine learning algorithm to construct a specific morphic component hierarchy that solves a problem
at hand. Morphics can be seen as a style of defining DSLs that are more declarative than most.

On average, changing values at higher levels of the hierarchy that forms a morphic component 
must have more impact on the practical behavior of that component than changes deeper in the hierarchy.
When we talk here about "changing values", we mean it in the immutable sense of creating a new
component that has some different values and hence some different behavior.
The phrase "practical behavior" is chosen carefully: this goal of morphic component design 
is deeply entwined with domain-specific outcomes and cannot be pursued or evaluated except in
that context. 

When done well, a set of morphic values, their invariants and interfaces, and the implementations
of those interfaces form a domain-specific language in which syntactically similar programs have, 
on average, similar real-world performance. This is important both for human abstract thinking and 
for machine learning. On the human side, this means that a given morphic hierarchy can be understood 
top-down, with successive refinement as one descends deeper. On the machine learning side, this means
that testing various changes to a high-performing morphic component, with  bias toward changes deep
in the value hierarchy, will likely lead to higher performance. It also means that two high-performing 
morphic components with similar value structure can be meaningfully combined into a new one that has a 
usefully high chance of performing better than either. 

A morphic component's runtime structure follows many of the same patterns as newer object-oriented
languages like Go and Swift that emphasize programming to interfaces, and that deemphasize or
disallow inheritance. The difference between morphics and OO programming is largely a
difference in style, ecosystem, and tooling, rather than a difference in runtime machinery. Even so,
the resulting difference in capability is profound.

## Morphics Implementation in Joy'

The initial implementation of morphics (current vaporware) is based on [Joy Prime](https://github.com/joy-prime/joy-prime) 
(commonly written Joy', pronounced "joy"), a new language that is being created partly for this purpose.
Joy' is mostly Clojure -- it is a lisp that runs on the JVM and embraces immutability by default.
Joy's abstractions for types, classes, interfaces, type hierarchies, and polymorphism are driven partly
by the needs of morphics.

When Joy's type system is used in the morphic style, a class implementation often 
deliberately provides just a high-level strategy for fulfilling the expectations of the class
interfaces. It leaves details to be filled in later by delegating work to interfaces of values 
contained in the class' representation.

## Important Morphic Idioms and Operations

### Constructing a Morphic Component in Code

Invoke a constructor defined by the root value's type class.

### Constructing a Morphic Component in a UI

In the UI implementation, map classes and their `new` map keys to non-programmer-readable
labels and descriptions.

### Constructing a Morphic Component through Machine Learning

### Textual Representation of a Morphic Component

Uses Joy's textual representation of values.

### Creating a Random Morphic Component of a Given Type

### Randomly Mutating a Morphic Component

### Mating Two Morphic Components



