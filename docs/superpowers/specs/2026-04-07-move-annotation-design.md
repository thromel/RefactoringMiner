# Move Annotation Design

Date: 2026-04-07

## Goal

Add a broad `Move Annotation` refactoring that captures an annotation being relocated from one declaration to another, instead of reporting the change only as separate add/remove annotation refactorings.

The immediate motivating case is GitHub issue `#1054`, where the same `@WithResource(...)` annotation is moved from a test method to its enclosing class in Spring Boot commit `249620246f07f691c0dc79f2e219246b5f3a3340`.

## Problem

Annotation refactorings are currently detected only within a single declaration kind:

- method annotations in `UMLOperationDiff`
- class annotations in `UMLAbstractClassDiff`
- attribute annotations in `UMLAttributeDiff`
- parameter and variable annotations in `UMLParameterDiff` and variable analysis

Because of that separation, an annotation that moves across declaration kinds is decomposed into:

- one remove refactoring on the source declaration
- one add refactoring on the target declaration

This loses the higher-level intent that the same annotation was moved.

## Scope

### In scope

- Add a new public refactoring type: `Move Annotation`
- Detect moves across declaration kinds inside the same changed class diff
- Support at least these source/target declaration categories:
  - class
  - method
  - attribute / enum constant
  - parameter
  - local variable
- Suppress consumed add/remove annotation refactorings when they are represented by `Move Annotation`
- Add regression coverage for the Spring Boot example

### Out of scope for first implementation

- Cross-class annotation moves caused by larger structural refactorings
- Cross-file matching outside a common class diff
- Fuzzy matching of non-equivalent annotations
- Language-specific special cases beyond the generic Java behavior already supported by the current annotation model

## Refactoring Model

Introduce a new `RefactoringType` entry:

- `MOVE_ANNOTATION("Move Annotation", "Move Annotation (.+) from (.+) to (.+) in class (.+)")`

Add a `MoveAnnotationRefactoring` class that stores:

- `annotationBefore`
- `annotationAfter`
- source declaration descriptor
- target declaration descriptor
- owning class name before/after

The descriptor must be generic enough to describe moves such as:

- method -> class
- class -> method
- method -> parameter
- attribute -> method

`toString()` should describe both declaration kinds explicitly instead of pretending the annotation stayed on the same kind of element.

Example description:

- `Move Annotation @WithResource(...) from method package hazelcastUp() : void to class org.springframework.boot.actuate.hazelcast.HazelcastHealthIndicatorTests in class org.springframework.boot.actuate.hazelcast.HazelcastHealthIndicatorTests`

## Detection Strategy

Use a post-processing detector over already-computed annotation diffs inside a class diff.

### Why this approach

- It is broad enough to pair annotations across declaration kinds
- It avoids a large AST-matching rewrite
- It can reuse the existing annotation equality logic in `UMLAnnotation`
- It keeps container-local diffing intact and adds a higher-level pairing phase on top

### Detection pipeline

1. Collect candidate removed annotations from all relevant source declarations in a class diff.
2. Collect candidate added annotations from all relevant target declarations in the same class diff.
3. Pair candidates by semantic equality:
   - first prefer `annotationBefore.equals(annotationAfter)`
   - optionally fall back to same type name plus equivalent payload if equality is too strict in a later iteration
4. Rank multiple possible targets for the same annotation using a stable preference order:
   - same declaration name if applicable
   - same class
   - smallest declaration-kind distance
   - smallest source/target line distance
5. Emit one `MoveAnnotationRefactoring` per accepted pair.
6. Mark the underlying add/remove annotation refactorings as consumed so they are not emitted separately.

### Initial matching constraints

To keep the first version deterministic:

- source and target must belong to the same `UMLAbstractClassDiff`
- annotations must be identical according to current `UMLAnnotation.equals`
- each removed annotation can match at most one added annotation
- each added annotation can match at most one removed annotation

## Integration Points

Add a new annotation-move synthesis step after declaration-local refactorings are available for a class diff.

Probable integration shape:

- gather class-level annotation adds/removes from `UMLAbstractClassDiff`
- gather method-level annotation adds/removes from each `UMLOperationDiff`
- gather attribute-level annotation adds/removes from attribute diffs
- gather parameter / variable annotation adds/removes from parameter diffs and variable-related refactoring lists where feasible
- run one coalescing pass that:
  - creates `MoveAnnotationRefactoring`
  - filters out the consumed `Add*Annotation` / `Remove*Annotation` refactorings

This pass should live near refactoring aggregation, not inside a single declaration diff, because the move spans multiple declaration kinds.

## Consumption Rules

When a move is emitted:

- remove the corresponding `Add*Annotation` refactoring from the result set
- remove the corresponding `Remove*Annotation` refactoring from the result set

Do not consume:

- `Modify*Annotation` refactorings
- add/remove pairs whose annotations are not semantically equal
- ambiguous pairs where deterministic selection fails

In ambiguous cases, keep the existing add/remove output rather than guessing.

## AST / Location Expectations

The first implementation does not require a new AST mapping concept.

`MoveAnnotationRefactoring.leftSide()` and `rightSide()` should expose:

- the source/target annotation locations
- the source/target declaration locations

That is sufficient for public API output and tests, even if the matcher layer still treats the nodes as unrelated.

## Testing Plan

### Regression test

Add a failing test first for Spring Boot commit `249620246f07f691c0dc79f2e219246b5f3a3340` asserting:

- `Move Annotation` is present
- the moved annotation is `@WithResource(...)`
- source declaration is `hazelcastUp()`
- target declaration is `HazelcastHealthIndicatorTests`
- the old decomposed pair is not reported for that same annotation

### Unit-style test

Add at least one focused test that exercises the coalescing logic without depending on the full oracle dataset. A synthetic before/after pair is acceptable if it produces a stable class diff and avoids network dependence.

### Regression protection

Run the targeted test suite for:

- the new move-annotation test
- at least one existing annotation-related test suite
- at least one test-specific refactoring suite to confirm no obvious regression in `Extract Fixture` / `Assert Throws` behavior

## Risks

### Ambiguous matches

If the same annotation appears multiple times on different declarations, naive pairing may choose the wrong target.

Mitigation:

- require exact annotation equality
- use deterministic ranking
- leave ambiguous cases decomposed as add/remove

### Hidden coupling with existing refactoring aggregation

Current annotation refactorings are emitted from multiple classes. Coalescing too early or too late could miss candidates or duplicate work.

Mitigation:

- implement the move synthesis in one aggregation point with clear ownership over final emitted refactorings

### Variable / parameter coverage complexity

Local variable annotation refactorings may be produced through a different path than class/method/attribute annotation diffs.

Mitigation:

- get class/method/attribute working first in the shared mechanism
- extend to parameter/variable once the aggregation point is confirmed
- keep the public refactoring generic from day one so coverage can expand without changing the API

## Recommended Delivery Order

1. Add `RefactoringType.MOVE_ANNOTATION`
2. Add `MoveAnnotationRefactoring`
3. Add the failing Spring Boot regression test
4. Implement class+method cross-kind pairing and add/remove suppression
5. Extend to attribute, parameter, and variable sources if the aggregation point supports them cleanly
6. Update supported-refactoring documentation

## Open Decisions Resolved

- This feature should be broad, not test-specific only
- First implementation should still be conservative: same-class-diff, exact-match pairing only
- Existing add/remove refactorings should be suppressed when a move is emitted, because the move is the higher-level representation
