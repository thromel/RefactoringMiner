# Assertion Migration Detection in RefactoringMiner

This document describes how RefactoringMiner detects assertion migration refactorings, including implementations for Issue #875.

## Table of Contents

1. [Current Implementation](#current-implementation)
2. [Architecture Overview](#architecture-overview)
3. [Detection Algorithm](#detection-algorithm)
4. [Supported Refactoring Types](#supported-refactoring-types)
5. [Advanced Assertion Migrations (Issue #875)](#advanced-assertion-migrations-issue-875)
6. [Implementation Guidelines](#implementation-guidelines)

---

## Current Implementation

RefactoringMiner supports detection of the following assertion-related refactorings:

| Refactoring Type | Description | Example |
|------------------|-------------|---------|
| `ASSERT_THROWS` | Migration to `assertThrows()` or `assertThatThrownBy()` | `@Test(expected=Ex.class)` → `assertThrows(Ex.class, ...)` |
| `ASSERT_TIMEOUT` | Migration to `assertTimeout()` | Introduction of timeout assertions |
| `REPLACE_CONDITIONAL_WITH_ASSUMPTION` | Conditional → `assume*()` | `if (!cond) return;` → `assumeTrue(cond)` |
| `REPLACE_ASSERTION_WITH_ASSUMPTION` | Assert → Assume | `assertTrue(cond)` → `assumeTrue(cond)` |
| `MODIFY_ASSERTION` | General assertion modification | Hamcrest → AssertJ migrations |
| `REPLACE_EXPECTED_EXCEPTION_RULE` | JUnit Rule → assertThatThrownBy | `@Rule ExpectedException` → `assertThatThrownBy()` |

---

## Architecture Overview

### Key Classes

```
gr.uom.java.xmi.diff/
├── AssertionRefactoring.java              # Interface for all assertion refactorings
├── AssertThrowsRefactoring.java           # assertThrows/assertThatThrownBy detection
├── AssertTimeoutRefactoring.java          # assertTimeout detection
├── AssumeRefactoring.java                 # assume* call detection (conditional replacement)
├── ModifyAssertionRefactoring.java        # [NEW] General assertion modifications
├── ReplaceExpectedExceptionRuleRefactoring.java  # [NEW] ExpectedException rule migration
└── MethodLevelRefactoring.java            # Interface for method-level refactorings

gr.uom.java.xmi.decomposition/
├── UMLOperationBodyMapper.java      # Core detection logic
├── AbstractCall.java                # Method call with isAssertCall(), isAssumeCall()
└── AbstractCodeFragment.java        # Code fragment with assertion helpers

gr.uom.java.xmi/
├── Constants.java                   # Language constants (ASSERT_THROWS, etc.)

org.refactoringminer.api/
└── RefactoringType.java             # Enum defining all refactoring types
```

### Interface Hierarchy

```java
Refactoring (interface)
    └── AssertionRefactoring (interface)
            ├── getMappings(): Set<AbstractCodeMapping>
            └── getCall(): AbstractCall
    └── MethodLevelRefactoring (interface)
            ├── getOperationBefore(): VariableDeclarationContainer
            └── getOperationAfter(): VariableDeclarationContainer
```

All assertion refactoring classes implement both `AssertionRefactoring` and `MethodLevelRefactoring`.

---

## Detection Algorithm

The detection logic is in `UMLOperationBodyMapper.java` (starting at line 4224):

### Step 1: Count Assertion Calls in "Before" Version

```java
int assertThrows1 = 0;
int assertThatThrownBy1 = 0;
int assertTimeout1 = 0;
int assume1 = 0;

for (AbstractCall call : container1.getAllOperationInvocations()) {
    if (call.getName().equals(JAVA.ASSERT_THROWS)) {
        assertThrows1++;
    }
    if (call.getName().equals(JAVA.ASSERT_THAT_THROWN_BY)) {
        assertThatThrownBy1++;
    }
    else if (call.getName().equals("assertTimeout")) {
        assertTimeout1++;
    }
    else if (call.isAssumeCall()) {
        assume1++;
    }
}
```

### Step 2: Collect Assertion Calls and Mappings in "After" Version

For each assertion call in the "after" version, the `populate()` method:
1. Adds the call to a list
2. Finds all code mappings where the assertion call location subsumes (contains) the mapped fragment

```java
private void populate(AbstractCall call, List<AbstractCall> calls,
                      Map<String, Set<AbstractCodeMapping>> codeMappings) {
    calls.add(call);
    for (AbstractCodeMapping mapping : this.mappings) {
        if (call.getLocationInfo().subsumes(mapping.getFragment2().getLocationInfo())
            || mapping.getFragment2().getLocationInfo().subsumes(call.getLocationInfo())) {
            // Add mapping to the map keyed by call.actualString()
        }
    }
}
```

### Step 3: Create Refactorings

The `createAssertRefactorings()` method creates refactoring instances when:
- Count of assertions in "before" < Count in "after" (new assertions introduced)
- The assertion call has associated code mappings
- The call is not a simple rename (e.g., `assertTrue` → `assumeTrue`)

```java
private void createAssertRefactorings(
    int assertCountBefore,
    Map<String, Set<AbstractCodeMapping>> assertMappings,
    List<AbstractCall> assertCalls,
    BiFunction<Set<AbstractCodeMapping>, AbstractCall, AssertionRefactoring> assertionRefProvider
) {
    if (assertCountBefore < assertCalls.size()) {
        for (AbstractCall assertCall : assertCalls) {
            Set<AbstractCodeMapping> set = assertMappings.get(assertCall.actualString());
            if (set != null && set.size() > 0) {
                // Validate it's a real refactoring, not just same call
                AssertionRefactoring ref = assertionRefProvider.apply(set, assertCall);
                refactorings.add(ref);
            }
        }
    }
}
```

### Step 4: Handle @Test(expected) Annotation

For `AssertThrowsRefactoring`, additional processing in `handleAdditionalAssertThrowMappings()`:
- Looks for `expected` attribute in `@Test` annotation
- Maps the expected exception type to the `assertThrows` argument

---

## Supported Refactoring Types

### 1. AssertThrowsRefactoring

**Detects**: Migration from `@Test(expected=...)` or try-catch to `assertThrows()` / `assertThatThrownBy()`

**Before:**
```java
@Test(expected = IllegalArgumentException.class)
public void testMethod() {
    obj.methodThatThrows();
}
```

**After:**
```java
@Test
public void testMethod() {
    assertThrows(IllegalArgumentException.class, () -> {
        obj.methodThatThrows();
    });
}
```

**Key Constants:**
- `JAVA.ASSERT_THROWS` = `"assertThrows"`
- `JAVA.ASSERT_THAT_THROWN_BY` = `"assertThatThrownBy"`

### 2. AssertTimeoutRefactoring

**Detects**: Introduction of `assertTimeout()` calls

**After:**
```java
assertTimeout(Duration.ofSeconds(5), () -> {
    slowOperation();
});
```

### 3. AssumeRefactoring

**Detects**: Conditional statements replaced with assumption calls

**Before:**
```java
if (!systemProperty.equals("expected")) {
    return;
}
// rest of test
```

**After:**
```java
assumeTrue(systemProperty.equals("expected"));
// rest of test
```

---

## Advanced Assertion Migrations (Issue #875)

Issue #875 requests detection of more sophisticated assertion migration patterns observed in real-world projects.

### Pattern 1: ExpectedException Rule → assertThatThrownBy (Mockito commit 6b818ba)

**Before:**
```java
@Rule
public ExpectedException expectedException = ExpectedException.none();

@Test
public void testMethod() {
    expectedException.expect(SomeException.class);
    expectedException.expectMessage("error message");
    // code that throws
    obj.methodThatThrows();
}
```

**After:**
```java
@Test
public void testMethod() {
    assertThatThrownBy(() -> {
        obj.methodThatThrows();
    })
    .isInstanceOf(SomeException.class)
    .hasMessageContaining("error message");
}
```

**Required Changes:**
1. New refactoring type: `REPLACE_EXPECTED_EXCEPTION_RULE`
2. Detect removal of `@Rule ExpectedException` field
3. Map `expect()` / `expectMessage()` calls to `isInstanceOf()` / `hasMessageContaining()`
4. Track field-level changes in addition to method-level

### Pattern 2: instanceof Check → asInstanceOf Assertion (Spring commit bed3689)

**Before:**
```java
boolean condition = result instanceof String;
assertThat(condition).isTrue();
assertThat(result).as("Invalid result").isEqualTo(expected);
```

**After:**
```java
assertThat(result).asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(expected);
```

Or with type-specific factories:
```java
assertThat(result).asInstanceOf(array(String[].class)).containsExactly(expected);
assertThat(result).asInstanceOf(LIST).containsExactly(expected1, expected2);
assertThat(result).asInstanceOf(optional(MultipartFile.class)).contains(expected);
```

**Required Changes:**
1. New refactoring type: `REPLACE_INSTANCEOF_WITH_ASSERTJ_TYPE_ASSERTION`
2. Detect removal of `instanceof` expression
3. Detect introduction of `asInstanceOf()` call
4. Map the type being checked to the AssertJ factory

### Pattern 3: Hamcrest → AssertJ Migration (Flink JUnit5 migration)

**Before (Hamcrest style):**
```java
assertThat("Open was called multiple times", openCalled, is(false));
assertThat(context.getMetricGroup(), notNullValue(MetricGroup.class));
assertEquals(nextEx, nextAct);
```

**After (AssertJ style):**
```java
assertThat(openCalled).as("Open was called multiple times").isFalse();
assertThat(context.getMetricGroup()).isNotNull();
assertThat(nextAct).isEqualTo(nextEx);
```

**Required Changes:**
1. New refactoring type: `MIGRATE_HAMCREST_TO_ASSERTJ` or `MIGRATE_JUNIT_TO_ASSERTJ`
2. Detect Hamcrest matcher patterns: `is()`, `notNullValue()`, `equalTo()`, etc.
3. Map to equivalent AssertJ methods
4. Handle argument reordering (message position changes)

---

## Implementation Guidelines

### Adding a New Assertion Refactoring Type

1. **Define RefactoringType** in `RefactoringType.java`:
```java
REPLACE_EXPECTED_EXCEPTION_RULE("Replace ExpectedException Rule",
    "Replace ExpectedException Rule with (.+) in method (.+) from class (.+)"),
```

2. **Create Refactoring Class** implementing `AssertionRefactoring` and `MethodLevelRefactoring`:
```java
public class ExpectedExceptionRuleRefactoring
    implements MethodLevelRefactoring, AssertionRefactoring {

    private Set<AbstractCodeMapping> mappings;
    private AbstractCall assertThatThrownByCall;
    private UMLAttribute removedField;  // The @Rule field
    private VariableDeclarationContainer operationBefore;
    private VariableDeclarationContainer operationAfter;

    // Implement required methods...
}
```

3. **Add Detection Logic** in `UMLOperationBodyMapper.java`:
```java
// In the assertion detection section (~line 4224)
int expectedException1 = 0;
for (AbstractCall call : container1.getAllOperationInvocations()) {
    if (call.getExpression() != null &&
        call.getExpression().equals("expectedException") &&
        call.getName().equals("expect")) {
        expectedException1++;
    }
}
// ... similar pattern for "after" version
```

4. **Add Constants** if needed in `Constants.java`:
```java
public final String ASSERT_THAT_THROWN_BY = "assertThatThrownBy";
// Already exists, but additional constants may be needed
```

5. **Create Tests** following the pattern in `ExpectedAnnotationToAssertThrowsTest.java`

### Key Considerations

1. **Code Mapping**: The detection relies on `AbstractCodeMapping` to track which code fragments in "before" correspond to "after". Ensure mappings capture the relationship.

2. **Lambda Detection**: Many modern assertions use lambdas. The system already handles this via `LambdaExpressionObject` tracking.

3. **Field-Level Changes**: For patterns like `ExpectedException` rule removal, you may need to integrate with `UMLClassDiff` which tracks field changes.

4. **Annotation Changes**: Changes to annotations (like removing `@Rule`) are tracked via `UMLAnnotationDiff` and related classes.

5. **Call Chain Analysis**: For fluent APIs like AssertJ (`assertThatThrownBy().isInstanceOf().hasMessage()`), you may need to analyze the full method chain.

### Testing Strategy

1. Create inline test code in test classes (see `ExpectedAnnotationToAssertThrowsTest.java`)
2. Use `UMLModelASTReader` to parse before/after code
3. Call `modelDiff.getRefactorings()` to get detected refactorings
4. Assert on refactoring type, count, and properties

---

## References

- Issue #875: https://github.com/tsantalis/RefactoringMiner/issues/875
- Spring Framework commit (asInstanceOf patterns): spring-projects/spring-framework@bed3689
- Mockito commit (ExpectedException rule): mockito/mockito@6b818ba
- Flink JUnit5 migration: FLINK-25544
