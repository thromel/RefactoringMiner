# Language Adapter Contract

This document defines the contract that language adapters must fulfill for RefactoringMiner's refactoring detection algorithms to work correctly across all supported languages.

## Overview

RefactoringMiner's detection algorithms are **language-agnostic** by design. They operate on a UML abstraction layer that hides language-specific details. For these algorithms to work correctly, each language adapter must properly populate this abstraction layer.

```
┌─────────────────────────────────────────────────────────┐
│  Detection Algorithms (Language-Agnostic)               │
│  Uses: LANG constants, AbstractCodeMapping, etc.        │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│  UML Abstraction Layer                                  │
│  UMLOperation, UMLAttribute, VariableDeclarationContainer│
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│  Language Adapters (Must Fulfill This Contract)         │
│  Example: CSharpTreeSitterASTBuilder, PyASTBuilder      │
└─────────────────────────────────────────────────────────┘
```

---

## Required UML Elements

### 1. UMLClass

Every class/type declaration in source code MUST produce a `UMLClass` with:

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Yes | Class name (without package/namespace prefix for matching) |
| `qualifiedName` | Yes | Fully qualified name including package/namespace |
| `visibility` | Yes | PUBLIC, PRIVATE, PROTECTED, or PACKAGE |
| `isAbstract` | Yes | Whether class is abstract |
| `isInterface` | Yes | Whether it's an interface |
| `isEnum` | Yes | Whether it's an enum |
| `operations` | Yes | List of methods (see UMLOperation below) |
| `attributes` | Yes | List of fields (see UMLAttribute below) |

### 2. UMLAttribute (Class Fields)

Every class field/property MUST produce a `UMLAttribute` with:

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Yes | Field name |
| `type` | Yes | Field type as `UMLType` |
| `visibility` | Yes | Access level |
| `isFinal` | If applicable | Whether field is readonly/final |
| `isStatic` | If applicable | Whether field is static |
| `variableDeclaration` | Yes | Linked `VariableDeclaration` with initializer |

**Why This Matters:**
- `Rename Attribute` refactoring requires UMLAttribute objects to compare
- `Change Attribute Type` refactoring requires type information

### 3. UMLOperation (Methods)

Every method declaration MUST produce a `UMLOperation` with:

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Yes | Method name |
| `parameters` | Yes | List of `UMLParameter` objects |
| `returnType` | Yes | Return type (can be void) |
| `visibility` | Yes | Access level |
| `body` | Yes | `OperationBody` with decomposed statements |
| `isConstructor` | Yes | Whether it's a constructor |
| `isAbstract` | If applicable | Whether method is abstract |
| `isStatic` | If applicable | Whether method is static |

### 4. UMLParameter

Every method parameter MUST produce a `UMLParameter` with:

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Yes | Parameter name |
| `type` | Yes | Parameter type as `UMLType` |
| `defaultValue` | If present | Default value expression |
| `isVarargs` | If applicable | Whether it's a varargs parameter |

**Why This Matters:**
- `Add Parameter`, `Remove Parameter` require parameter lists
- `Change Parameter Type` requires type comparison
- `Rename Parameter` requires parameter name tracking

---

## Required Decomposition Elements

### 5. OperationBody

Every method body MUST be decomposed into:

| Element | Required | Description |
|---------|----------|-------------|
| `CompositeStatement` | Yes | Root statement containing all others |
| `statements` | Yes | All statements in the method |
| `operationInvocations` | Yes | All method calls detected |
| `variableDeclarations` | Yes | All local variable declarations |

### 6. VariableDeclaration

Every variable declaration (local or field) MUST include:

| Field | Required | Description |
|-------|----------|-------------|
| `variableName` | Yes | Variable name |
| `type` | Yes | Variable type |
| `initializer` | If present | **CRITICAL**: The initialization expression |
| `isAttribute` | Yes | Whether this is a class field |

**Why This Matters:**
- `Extract Variable` detects when an expression is extracted into a variable
- `Inline Variable` detects when a variable is replaced by its initializer
- Without `initializer`, these refactorings CANNOT be detected

### 7. OperationInvocation (AbstractCall)

Every method call MUST produce an `OperationInvocation` with:

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Yes | Method name being called |
| `expression` | If present | The receiver object (e.g., `this`, `obj`) |
| `arguments` | Yes | List of argument expressions |
| `typeArguments` | If present | Generic type arguments |

**Why This Matters:**
- `Extract Method` detects calls to newly added methods
- `Inline Method` detects calls to removed methods

---

## String Representation Requirements

### 8. Use LANG Constants

All string representations MUST use language-specific constants from `Constants` enum:

```java
// CORRECT - Language agnostic
String stmt = LANG.RETURN_SPACE + value + LANG.STATEMENT_TERMINATION;

// WRONG - Java-specific, breaks for Python
String stmt = "return " + value + ";\n";
```

Available constants:
- `LANG.STATEMENT_TERMINATION` - `";\n"` for Java/C#, `"\n"` for Python
- `LANG.RETURN_SPACE` - `"return "` for all languages
- `LANG.LAMBDA_ARROW` - `" -> "` for Java, `" => "` for C#
- `LANG.THIS_DOT` - `"this."` for Java/C#, `"self."` for Python
- `LANG.AND`, `LANG.OR`, `LANG.NOT` - Logical operators
- `LANG.NULL`, `LANG.TRUE`, `LANG.FALSE` - Literal keywords

### 9. Statement String Format

Statements MUST use consistent format for matching:

```
// Variable declaration format
type name = expression;

// Return statement format
return expression;

// Method call format
receiver.methodName(arg1, arg2);
```

---

## Testing Contract Compliance

### Required Tests

Every language adapter MUST pass these test cases:

1. **Class Extraction Test**
   ```java
   @Test void testUMLClassCreated()
   // Verify: UMLClass created for class declaration
   ```

2. **Attribute Extraction Test**
   ```java
   @Test void testUMLAttributesCreated()
   // Verify: UMLAttribute created for each field
   // Verify: Type and visibility populated
   ```

3. **Operation Body Test**
   ```java
   @Test void testOperationBodiesPopulated()
   // Verify: OperationBody is not null
   // Verify: Statements are decomposed
   ```

4. **Variable Initializer Test**
   ```java
   @Test void testVariableInitializersExtracted()
   // Verify: VariableDeclaration.getInitializer() returns expression
   // CRITICAL for inline/extract variable detection
   ```

5. **Method Invocation Test**
   ```java
   @Test void testMethodInvocationsDetected()
   // Verify: getAllOperationInvocations() finds calls
   // CRITICAL for extract/inline method detection
   ```

### Using AbstractLanguageAdapterTest

New language adapters SHOULD extend `AbstractLanguageAdapterTest`:

```java
class MyLanguageAdapterTest extends AbstractLanguageAdapterTest {

    @Override
    protected String getSampleClassCode() {
        return "class Foo { int x; void bar() { int y = x + 1; } }";
    }

    @Override
    protected String getFileExtension() {
        return ".mylang";
    }
}
```

---

## Impact on Refactoring Detection

| If Adapter Misses... | These Refactorings Fail |
|---------------------|------------------------|
| UMLAttribute for fields | Rename Attribute, Change Attribute Type |
| VariableDeclaration.initializer | Inline Variable, Extract Variable |
| OperationInvocation detection | Extract Method, Inline Method |
| Parameter types | Change Parameter Type |
| Return types | Change Return Type |

---

## Checklist for New Language Adapters

- [ ] Creates `UMLClass` for every class/type declaration
- [ ] Creates `UMLAttribute` for every class field
- [ ] Creates `UMLOperation` with `OperationBody` for every method
- [ ] Populates `VariableDeclaration.initializer` for all variable declarations
- [ ] Detects all `OperationInvocation` (method calls) in method bodies
- [ ] Uses `LANG` constants for all string representations
- [ ] Passes all tests in `AbstractLanguageAdapterTest`
- [ ] All 13+ refactoring types detected in integration test
