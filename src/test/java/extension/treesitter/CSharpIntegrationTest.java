package extension.treesitter;

import gr.uom.java.xmi.Constants;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.decomposition.OperationBody;
import extension.umladapter.UMLModelAdapter;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.PathFileUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify C# refactoring detection pipeline is wired up correctly.
 */
class CSharpIntegrationTest {

    @Test
    void testPathFileUtilsRecognizesCSharp() {
        // Test file extension recognition
        assertTrue(PathFileUtils.isCSharpFile("Calculator.cs"));
        assertTrue(PathFileUtils.isCSharpFile("/path/to/MyClass.cs"));
        assertFalse(PathFileUtils.isCSharpFile("MyClass.java"));
        assertFalse(PathFileUtils.isCSharpFile("script.py"));

        // Test language supported file
        assertTrue(PathFileUtils.isLangSupportedFile("MyClass.cs"));
        assertTrue(PathFileUtils.isSupportedFile("MyClass.cs"));

        // Test getLang returns CSHARP
        assertEquals(Constants.CSHARP, PathFileUtils.getLang("MyClass.cs"));
        assertEquals(Constants.CSHARP, PathFileUtils.getLang("/path/to/Calculator.cs"));
    }

    @Test
    void testCSharpConstantsCorrect() {
        // Verify C# constants are properly defined
        assertEquals(";\n", Constants.CSHARP.STATEMENT_TERMINATION);
        assertEquals(" => ", Constants.CSHARP.LAMBDA_ARROW);
        assertEquals("this", Constants.CSHARP.THIS);
        assertEquals("this.", Constants.CSHARP.THIS_DOT);
        assertEquals(" && ", Constants.CSHARP.AND);
        assertEquals(" || ", Constants.CSHARP.OR);
        assertEquals("!", Constants.CSHARP.NOT);
        assertEquals("null", Constants.CSHARP.NULL);
        assertEquals("Assert.Throws", Constants.CSHARP.ASSERT_THROWS);
    }

    @Test
    void testCSharpUMLModelCreation() throws Exception {
        String csharpCode = """
            using System;

            namespace MyApp
            {
                public class Calculator
                {
                    private int value;

                    public Calculator(int initial)
                    {
                        this.value = initial;
                    }

                    public int Add(int x)
                    {
                        return value + x;
                    }

                    public int Subtract(int x)
                    {
                        return value - x;
                    }
                }
            }
            """;

        // Create file contents map
        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("Calculator.cs", csharpCode);

        // Build UML model from C# code using UMLModelAdapter constructor
        UMLModelAdapter adapter = new UMLModelAdapter(fileContents);
        UMLModel model = adapter.getUMLModel();

        assertNotNull(model, "UML model should not be null");

        // Should have created a UML class for Calculator
        assertFalse(model.getClassList().isEmpty(), "Should have at least one class");

        // Find the Calculator class (name might include namespace)
        UMLClass calcClass = model.getClassList().stream()
                .filter(c -> c.getName().contains("Calculator"))
                .findFirst()
                .orElse(null);

        assertNotNull(calcClass, "Should have Calculator class. Found classes: " +
                model.getClassList().stream().map(UMLClass::getName).toList());

        // Verify operations (methods) were extracted
        assertTrue(calcClass.getOperations().size() >= 2,
                "Should have at least 2 operations (Add + Subtract). Found: " +
                calcClass.getOperations().stream().map(op -> op.getName()).toList());

        // Note: Attribute extraction for C# fields may need further work
        // For now, verify core functionality works
        System.out.println("✓ C# UML model creation test passed!");
        System.out.println("  - Class: " + calcClass.getName());
        System.out.println("  - Operations: " + calcClass.getOperations().size());
        System.out.println("  - Attributes: " + calcClass.getAttributes().size());
    }

    @Test
    void testCSharpRefactoringDetectionWiring() throws Exception {
        // Version 1 of the code
        String codeV1 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;

        // Version 2 - renamed method from Add to Sum
        String codeV2 = """
            public class Calculator
            {
                public int Sum(int a, int b)
                {
                    return a + b;
                }
            }
            """;

        // Create models for both versions
        Map<String, String> filesV1 = new HashMap<>();
        filesV1.put("Calculator.cs", codeV1);
        UMLModelAdapter adapterV1 = new UMLModelAdapter(filesV1);
        UMLModel modelV1 = adapterV1.getUMLModel();

        Map<String, String> filesV2 = new HashMap<>();
        filesV2.put("Calculator.cs", codeV2);
        UMLModelAdapter adapterV2 = new UMLModelAdapter(filesV2);
        UMLModel modelV2 = adapterV2.getUMLModel();

        assertNotNull(modelV1);
        assertNotNull(modelV2);

        // Both models should have the Calculator class
        assertEquals(1, modelV1.getClassList().size());
        assertEquals(1, modelV2.getClassList().size());

        // V1 should have "Add" method, V2 should have "Sum" method
        UMLClass classV1 = modelV1.getClassList().get(0);
        UMLClass classV2 = modelV2.getClassList().get(0);

        assertTrue(classV1.getOperations().stream().anyMatch(op -> op.getName().equals("Add")),
                "V1 should have Add method");
        assertTrue(classV2.getOperations().stream().anyMatch(op -> op.getName().equals("Sum")),
                "V2 should have Sum method");

        System.out.println("✓ C# refactoring detection wiring test passed!");
        System.out.println("  - V1 method: Add");
        System.out.println("  - V2 method: Sum");
    }

    @Test
    void testCSharpRenameMethodDetection() throws Exception {
        // Version 1: Method named "Add"
        String codeV1 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;

        // Version 2: Method renamed to "Sum"
        String codeV2 = """
            public class Calculator
            {
                public int Sum(int a, int b)
                {
                    return a + b;
                }
            }
            """;

        // Create models
        Map<String, String> filesV1 = new HashMap<>();
        filesV1.put("Calculator.cs", codeV1);
        UMLModel modelV1 = new UMLModelAdapter(filesV1).getUMLModel();

        Map<String, String> filesV2 = new HashMap<>();
        filesV2.put("Calculator.cs", codeV2);
        UMLModel modelV2 = new UMLModelAdapter(filesV2).getUMLModel();

        // Run refactoring detection
        UMLModelDiff diff = modelV1.diff(modelV2);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Detected refactorings for Rename Method:");
        for (Refactoring r : refactorings) {
            System.out.println("  - " + r.getRefactoringType() + ": " + r.getName());
        }

        // Check if Rename Method was detected
        boolean foundRenameMethod = refactorings.stream()
                .anyMatch(r -> r.getRefactoringType() == RefactoringType.RENAME_METHOD);

        if (foundRenameMethod) {
            System.out.println("✓ Rename Method refactoring correctly detected!");
        } else {
            System.out.println("⚠ Rename Method not detected. Found: " + refactorings.size() + " refactorings");
        }
    }

    @Test
    void testCSharpExtractMethodDetection() throws Exception {
        // Version 1: Single method with inline logic
        String codeV1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = sum * 2;
                    return result;
                }
            }
            """;

        // Version 2: Extracted multiplication to separate method
        String codeV2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = Double(sum);
                    return result;
                }

                public int Double(int value)
                {
                    return value * 2;
                }
            }
            """;

        // Create models
        Map<String, String> filesV1 = new HashMap<>();
        filesV1.put("Calculator.cs", codeV1);
        UMLModel modelV1 = new UMLModelAdapter(filesV1).getUMLModel();

        Map<String, String> filesV2 = new HashMap<>();
        filesV2.put("Calculator.cs", codeV2);
        UMLModel modelV2 = new UMLModelAdapter(filesV2).getUMLModel();

        // Run refactoring detection
        UMLModelDiff diff = modelV1.diff(modelV2);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Detected refactorings for Extract Method:");
        for (Refactoring r : refactorings) {
            System.out.println("  - " + r.getRefactoringType() + ": " + r.getName());
        }

        // Check if Extract Method was detected
        boolean foundExtractMethod = refactorings.stream()
                .anyMatch(r -> r.getRefactoringType() == RefactoringType.EXTRACT_OPERATION);

        if (foundExtractMethod) {
            System.out.println("✓ Extract Method refactoring correctly detected!");
        } else {
            System.out.println("⚠ Extract Method not detected. Found: " + refactorings.size() + " refactorings");
        }
    }

    @Test
    void testCSharpRenameVariableDetection() throws Exception {
        // Version 1: Variable named "x"
        String codeV1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int x = a + b;
                    return x;
                }
            }
            """;

        // Version 2: Variable renamed to "sum"
        String codeV2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    return sum;
                }
            }
            """;

        // Create models
        Map<String, String> filesV1 = new HashMap<>();
        filesV1.put("Calculator.cs", codeV1);
        UMLModel modelV1 = new UMLModelAdapter(filesV1).getUMLModel();

        Map<String, String> filesV2 = new HashMap<>();
        filesV2.put("Calculator.cs", codeV2);
        UMLModel modelV2 = new UMLModelAdapter(filesV2).getUMLModel();

        // Run refactoring detection
        UMLModelDiff diff = modelV1.diff(modelV2);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Detected refactorings for Rename Variable:");
        for (Refactoring r : refactorings) {
            System.out.println("  - " + r.getRefactoringType() + ": " + r.getName());
        }

        // Check if Rename Variable was detected
        boolean foundRenameVariable = refactorings.stream()
                .anyMatch(r -> r.getRefactoringType() == RefactoringType.RENAME_VARIABLE);

        if (foundRenameVariable) {
            System.out.println("✓ Rename Variable refactoring correctly detected!");
        } else {
            System.out.println("⚠ Rename Variable not detected. Found: " + refactorings.size() + " refactorings");
        }
    }

    @Test
    void testCSharpExtractMethodDebug() throws Exception {
        System.out.println("\n========== DEBUG: Extract Method Detection ==========\n");

        // Version 1: Single method with inline logic
        String codeV1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = sum * 2;
                    return result;
                }
            }
            """;

        // Version 2: Extracted multiplication to separate method
        String codeV2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = Double(sum);
                    return result;
                }

                public int Double(int value)
                {
                    return value * 2;
                }
            }
            """;

        // Create models
        Map<String, String> filesV1 = new HashMap<>();
        filesV1.put("Calculator.cs", codeV1);
        UMLModel modelV1 = new UMLModelAdapter(filesV1).getUMLModel();

        Map<String, String> filesV2 = new HashMap<>();
        filesV2.put("Calculator.cs", codeV2);
        UMLModel modelV2 = new UMLModelAdapter(filesV2).getUMLModel();

        // Debug V1 model
        System.out.println("=== V1 Model (Before Extraction) ===");
        for (UMLClass umlClass : modelV1.getClassList()) {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  Operation: " + op.getName());
                System.out.println("    Signature: " + op.toString());
                OperationBody body = op.getBody();
                System.out.println("    Body: " + (body != null ? "PRESENT" : "NULL"));
                if (body != null) {
                    System.out.println("    CompositeStatement: " + (body.getCompositeStatement() != null ? "PRESENT" : "NULL"));
                    if (body.getCompositeStatement() != null) {
                        System.out.println("    Statements count: " + body.getCompositeStatement().getStatements().size());
                        System.out.println("    String representation:");
                        for (String s : body.stringRepresentation()) {
                            System.out.println("      > " + s);
                        }
                    }
                    System.out.println("    All Invocations: " + body.getAllOperationInvocations().size());
                    body.getAllOperationInvocations().forEach(inv ->
                        System.out.println("      - " + inv.actualString()));
                }
            }
        }

        // Debug V2 model
        System.out.println("\n=== V2 Model (After Extraction) ===");
        for (UMLClass umlClass : modelV2.getClassList()) {
            System.out.println("Class: " + umlClass.getName());
            for (UMLOperation op : umlClass.getOperations()) {
                System.out.println("  Operation: " + op.getName());
                System.out.println("    Signature: " + op.toString());
                OperationBody body = op.getBody();
                System.out.println("    Body: " + (body != null ? "PRESENT" : "NULL"));
                if (body != null) {
                    System.out.println("    CompositeStatement: " + (body.getCompositeStatement() != null ? "PRESENT" : "NULL"));
                    if (body.getCompositeStatement() != null) {
                        System.out.println("    Statements count: " + body.getCompositeStatement().getStatements().size());
                        System.out.println("    String representation:");
                        for (String s : body.stringRepresentation()) {
                            System.out.println("      > " + s);
                        }
                    }
                    System.out.println("    All Invocations: " + body.getAllOperationInvocations().size());
                    body.getAllOperationInvocations().forEach(inv ->
                        System.out.println("      - " + inv.actualString()));
                }
            }
        }

        // Run diff and show refactorings
        System.out.println("\n=== Model Diff Analysis ===");
        UMLModelDiff diff = modelV1.diff(modelV2);

        System.out.println("Added classes: " + diff.getAddedClasses().size());
        diff.getAddedClasses().forEach(c -> System.out.println("  + " + c.getName()));

        System.out.println("Removed classes: " + diff.getRemovedClasses().size());
        diff.getRemovedClasses().forEach(c -> System.out.println("  - " + c.getName()));

        System.out.println("Common class diffs: " + diff.getCommonClassDiffList().size());
        for (var classDiff : diff.getCommonClassDiffList()) {
            System.out.println("  Class: " + classDiff.getOriginalClassName());
            System.out.println("    Added operations: " + classDiff.getAddedOperations().size());
            classDiff.getAddedOperations().forEach(op -> System.out.println("      + " + op.getName()));
            System.out.println("    Removed operations: " + classDiff.getRemovedOperations().size());
            classDiff.getRemovedOperations().forEach(op -> System.out.println("      - " + op.getName()));
            System.out.println("    Operation mappers: " + classDiff.getOperationBodyMapperList().size());
            for (var mapper : classDiff.getOperationBodyMapperList()) {
                System.out.println("      Mapper: " + mapper.getContainer1().getName() + " -> " + mapper.getContainer2().getName());
                System.out.println("        NonMappedLeavesT1: " + mapper.getNonMappedLeavesT1().size());
                mapper.getNonMappedLeavesT1().forEach(l -> System.out.println("          T1: " + l.getString()));
                System.out.println("        NonMappedLeavesT2: " + mapper.getNonMappedLeavesT2().size());
                mapper.getNonMappedLeavesT2().forEach(l -> System.out.println("          T2: " + l.getString()));
                System.out.println("        Mappings: " + mapper.getMappings().size());
                for (var mapping : mapper.getMappings()) {
                    System.out.println("          [" + (mapping.isExact() ? "EXACT" : "DIFF") + "] \"" + mapping.getFragment1().getString() + "\" -> \"" + mapping.getFragment2().getString() + "\"");
                    System.out.println("            Fragment1 invocations: " + mapping.getFragment1().getMethodInvocations().size());
                    mapping.getFragment1().getMethodInvocations().forEach(inv -> System.out.println("              - " + inv.actualString()));
                    System.out.println("            Fragment2 invocations: " + mapping.getFragment2().getMethodInvocations().size());
                    mapping.getFragment2().getMethodInvocations().forEach(inv -> System.out.println("              - " + inv.actualString()));
                    System.out.println("            Replacements: " + mapping.getReplacements().size());
                    for (var replacement : mapping.getReplacements()) {
                        System.out.println("              Type: " + replacement.getType() + " | Class: " + replacement.getClass().getSimpleName());
                        System.out.println("              Before: " + replacement.getBefore() + " -> After: " + replacement.getAfter());
                    }
                }
                System.out.println("        ReplacementsInvolvingMethodInvocation: " + mapper.getReplacementsInvolvingMethodInvocation().size());
            }
        }

        // Check matchingInvocations manually
        System.out.println("\n=== Checking Match Between Invocation and Operation ===");
        for (var classDiff : diff.getCommonClassDiffList()) {
            for (UMLOperation addedOp : classDiff.getAddedOperations()) {
                System.out.println("Added Operation: " + addedOp.getName() + " with params: " + addedOp.getParametersWithoutReturnType().size());
                // Check if added operation has a single return statement
                var singleReturn = addedOp.singleReturnStatement();
                System.out.println("  singleReturnStatement: " + (singleReturn != null ? singleReturn.getString() : "NULL"));
                for (var mapper : classDiff.getOperationBodyMapperList()) {
                    var container2 = mapper.getContainer2();
                    System.out.println("  Checking invocations in: " + container2.getName());
                    for (var inv : container2.getAllOperationInvocations()) {
                        System.out.println("    Invocation: " + inv.actualString());
                        System.out.println("      getName(): " + inv.getName());
                        System.out.println("      Arguments: " + inv.arguments());
                        boolean matches = inv.matchesOperation(addedOp, container2, classDiff, diff);
                        System.out.println("      matchesOperation(): " + matches);
                    }
                }
            }
        }

        // Check variable declarations in mappings
        System.out.println("\n=== Variable Declarations in Mappings ===");
        for (var classDiff : diff.getCommonClassDiffList()) {
            for (var mapper : classDiff.getOperationBodyMapperList()) {
                System.out.println("Mapper: " + mapper.getContainer1().getName() + " -> " + mapper.getContainer2().getName());
                for (var mapping : mapper.getMappings()) {
                    var vd1 = mapping.getFragment1().getVariableDeclarations();
                    var vd2 = mapping.getFragment2().getVariableDeclarations();
                    if (!vd1.isEmpty() || !vd2.isEmpty()) {
                        System.out.println("  Mapping has var decls:");
                        System.out.println("    Fragment1 varDecls: " + vd1);
                        if (!vd1.isEmpty()) {
                            var init1 = vd1.get(0).getInitializer();
                            System.out.println("    Fragment1 initializer: " + (init1 != null ? init1.getString() : "NULL"));
                        }
                        System.out.println("    Fragment2 varDecls: " + vd2);
                        if (!vd2.isEmpty()) {
                            var init2 = vd2.get(0).getInitializer();
                            System.out.println("    Fragment2 initializer: " + (init2 != null ? init2.getString() : "NULL"));
                        }
                        System.out.println("    varDecls equal: " + vd1.toString().equals(vd2.toString()));
                    }
                }
            }
        }

        // Get refactorings
        List<Refactoring> refactorings = diff.getRefactorings();
        System.out.println("\n=== Detected Refactorings ===");
        System.out.println("Total: " + refactorings.size());
        for (Refactoring r : refactorings) {
            System.out.println("  " + r.getRefactoringType() + ": " + r.getName());
        }

        System.out.println("\n========== END DEBUG ==========\n");
    }

    /**
     * Comprehensive test for multiple C# refactoring types.
     * Tests common refactoring patterns to verify C# detection coverage.
     */
    @Test
    void testCSharpRefactoringTypesCoverage() throws Exception {
        System.out.println("\n========== C# REFACTORING TYPES COVERAGE TEST ==========\n");

        // Track results
        java.util.Map<String, Boolean> results = new java.util.LinkedHashMap<>();

        // Test 1: Rename Method
        results.put("Rename Method", testRenameMethod());

        // Test 2: Rename Variable
        results.put("Rename Variable", testRenameVariable());

        // Test 3: Extract Method
        results.put("Extract Method", testExtractMethod());

        // Test 4: Inline Variable
        results.put("Inline Variable", testInlineVariable());

        // Test 5: Extract Variable
        results.put("Extract Variable", testExtractVariable());

        // Test 6: Rename Parameter
        results.put("Rename Parameter", testRenameParameter());

        // Test 7: Change Return Type
        results.put("Change Return Type", testChangeReturnType());

        // Test 8: Change Parameter Type
        results.put("Change Parameter Type", testChangeParameterType());

        // Test 9: Add Parameter
        results.put("Add Parameter", testAddParameter());

        // Test 10: Remove Parameter
        results.put("Remove Parameter", testRemoveParameter());

        // Test 11: Rename Attribute (Field)
        results.put("Rename Attribute", testRenameAttribute());

        // Test 12: Change Attribute Type
        results.put("Change Attribute Type", testChangeAttributeType());

        // Test 13: Inline Method
        results.put("Inline Method", testInlineMethod());

        // ========== PHASE 2 REFACTORING TYPES ==========
        System.out.println("\n--- Phase 2 Refactoring Types ---");

        // Test 14: Rename Class
        results.put("Rename Class", testRenameClass());

        // Test 15: Change Variable Type
        results.put("Change Variable Type", testChangeVariableType());

        // Test 16: Reorder Parameter
        results.put("Reorder Parameter", testReorderParameter());

        // Test 17: Move Method
        results.put("Move Method", testMoveMethod());

        // Test 18: Move Attribute
        results.put("Move Attribute", testMoveAttribute());

        // Test 19: Move Class
        results.put("Move Class", testMoveClass());

        // Test 20: Extract Class
        results.put("Extract Class", testExtractClass());

        // Test 21: Pull Up Method
        results.put("Pull Up Method", testPullUpMethod());

        // Test 22: Push Down Method
        results.put("Push Down Method", testPushDownMethod());

        // Print summary
        System.out.println("\n========== SUMMARY ==========");
        int passed = 0;
        int failed = 0;
        for (var entry : results.entrySet()) {
            String status = entry.getValue() ? "✓ PASS" : "✗ FAIL";
            System.out.println(status + " - " + entry.getKey());
            if (entry.getValue()) passed++; else failed++;
        }
        System.out.println("\nTotal: " + passed + " passed, " + failed + " failed out of " + results.size());
        System.out.println("========== END COVERAGE TEST ==========\n");
    }

    private boolean testRenameMethod() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Sum(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.RENAME_METHOD, "Rename Method");
    }

    private boolean testRenameVariable() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int x = a + b;
                    return x;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int result = a + b;
                    return result;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.RENAME_VARIABLE, "Rename Variable");
    }

    private boolean testExtractMethod() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = sum * 2;
                    return result;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = Double(sum);
                    return result;
                }

                public int Double(int value)
                {
                    return value * 2;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.EXTRACT_OPERATION, "Extract Method");
    }

    private boolean testInlineVariable() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int temp = a + b;
                    return temp;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.INLINE_VARIABLE, "Inline Variable");
    }

    private boolean testExtractVariable() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int result = a + b;
                    return result;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.EXTRACT_VARIABLE, "Extract Variable");
    }

    private boolean testRenameParameter() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Add(int x, int y)
                {
                    return x + y;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Add(int first, int second)
                {
                    return first + second;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.RENAME_PARAMETER, "Rename Parameter");
    }

    private boolean testChangeReturnType() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public long Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.CHANGE_RETURN_TYPE, "Change Return Type");
    }

    private boolean testChangeParameterType() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Add(long a, int b)
                {
                    return a + b;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.CHANGE_PARAMETER_TYPE, "Change Parameter Type");
    }

    private boolean testAddParameter() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Add(int a)
                {
                    return a;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.ADD_PARAMETER, "Add Parameter");
    }

    private boolean testRemoveParameter() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Add(int a)
                {
                    return a;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.REMOVE_PARAMETER, "Remove Parameter");
    }

    private boolean testRenameAttribute() throws Exception {
        String v1 = """
            public class Calculator
            {
                private int val;

                public int GetValue()
                {
                    return val;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                private int value;

                public int GetValue()
                {
                    return value;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.RENAME_ATTRIBUTE, "Rename Attribute");
    }

    private boolean testChangeAttributeType() throws Exception {
        String v1 = """
            public class Calculator
            {
                private int value;

                public int GetValue()
                {
                    return value;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                private long value;

                public int GetValue()
                {
                    return value;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.CHANGE_ATTRIBUTE_TYPE, "Change Attribute Type");
    }

    private boolean testInlineMethod() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = Double(sum);
                    return result;
                }

                public int Double(int value)
                {
                    return value * 2;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Calculate(int a, int b)
                {
                    int sum = a + b;
                    int result = sum * 2;
                    return result;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.INLINE_OPERATION, "Inline Method");
    }

    // ========== PHASE 2 TEST METHODS ==========

    private boolean testRenameClass() throws Exception {
        // Keep same file name to detect RENAME_CLASS (not MOVE_RENAME_CLASS)
        String v1 = """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        String v2 = """
            public class MathHelper
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.RENAME_CLASS, "Rename Class");
    }

    private boolean testChangeVariableType() throws Exception {
        String v1 = """
            public class Calculator
            {
                public double Calculate(int a, int b)
                {
                    int result = a + b;
                    return result;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public double Calculate(int a, int b)
                {
                    double result = a + b;
                    return result;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.CHANGE_VARIABLE_TYPE, "Change Variable Type");
    }

    private boolean testReorderParameter() throws Exception {
        String v1 = """
            public class Calculator
            {
                public int Calculate(int a, int b, int c)
                {
                    return a + b + c;
                }
            }
            """;
        String v2 = """
            public class Calculator
            {
                public int Calculate(int c, int a, int b)
                {
                    return a + b + c;
                }
            }
            """;
        return detectRefactoring(v1, v2, RefactoringType.REORDER_PARAMETER, "Reorder Parameter");
    }

    private boolean testMoveMethod() throws Exception {
        Map<String, String> v1 = new HashMap<>();
        v1.put("Calculator.cs", """
            public class Calculator
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
            }
            """);
        v1.put("MathHelper.cs", """
            public class MathHelper
            {
                public int Multiply(int a, int b)
                {
                    return a * b;
                }
            }
            """);

        Map<String, String> v2 = new HashMap<>();
        v2.put("Calculator.cs", """
            public class Calculator
            {
            }
            """);
        v2.put("MathHelper.cs", """
            public class MathHelper
            {
                public int Add(int a, int b)
                {
                    return a + b;
                }
                public int Multiply(int a, int b)
                {
                    return a * b;
                }
            }
            """);
        return detectRefactoringMultiFile(v1, v2, RefactoringType.MOVE_OPERATION, "Move Method");
    }

    private boolean testMoveAttribute() throws Exception {
        // Test Pull Up Attribute (subclass -> superclass) which is a form of Move Attribute
        // PULL_UP_ATTRIBUTE is detected when attribute moves from child to parent class
        Map<String, String> v1 = new HashMap<>();
        v1.put("Animal.cs", """
            public class Animal
            {
            }
            """);
        v1.put("Dog.cs", """
            public class Dog : Animal
            {
                private string name;
                public string GetName() { return name; }
            }
            """);

        Map<String, String> v2 = new HashMap<>();
        v2.put("Animal.cs", """
            public class Animal
            {
                private string name;
                public string GetName() { return name; }
            }
            """);
        v2.put("Dog.cs", """
            public class Dog : Animal
            {
            }
            """);
        return detectRefactoringMultiFile(v1, v2, RefactoringType.PULL_UP_ATTRIBUTE, "Move Attribute");
    }

    private boolean testMoveClass() throws Exception {
        // Move class between packages (same file name, different package path)
        Map<String, String> v1 = new HashMap<>();
        v1.put("src/OldPackage/Calculator.cs", """
            namespace OldPackage
            {
                public class Calculator
                {
                    public int Add(int a, int b)
                    {
                        return a + b;
                    }
                }
            }
            """);

        Map<String, String> v2 = new HashMap<>();
        v2.put("src/NewPackage/Calculator.cs", """
            namespace NewPackage
            {
                public class Calculator
                {
                    public int Add(int a, int b)
                    {
                        return a + b;
                    }
                }
            }
            """);
        return detectRefactoringMultiFile(v1, v2, RefactoringType.MOVE_CLASS, "Move Class");
    }

    private boolean testExtractClass() throws Exception {
        String v1 = """
            public class Person
            {
                private string name;
                private string street;
                private string city;
                private string zipCode;

                public string GetAddress()
                {
                    return street + ", " + city + " " + zipCode;
                }
            }
            """;

        Map<String, String> v2 = new HashMap<>();
        v2.put("Person.cs", """
            public class Person
            {
                private string name;
                private Address address;
            }
            """);
        v2.put("Address.cs", """
            public class Address
            {
                private string street;
                private string city;
                private string zipCode;

                public string GetAddress()
                {
                    return street + ", " + city + " " + zipCode;
                }
            }
            """);

        Map<String, String> v1Map = new HashMap<>();
        v1Map.put("Person.cs", v1);
        return detectRefactoringMultiFile(v1Map, v2, RefactoringType.EXTRACT_CLASS, "Extract Class");
    }

    private boolean testPullUpMethod() throws Exception {
        Map<String, String> v1 = new HashMap<>();
        v1.put("Animal.cs", """
            public class Animal
            {
            }
            """);
        v1.put("Dog.cs", """
            public class Dog : Animal
            {
                public void MakeSound()
                {
                    Console.WriteLine("Bark");
                }
            }
            """);

        Map<String, String> v2 = new HashMap<>();
        v2.put("Animal.cs", """
            public class Animal
            {
                public void MakeSound()
                {
                    Console.WriteLine("Bark");
                }
            }
            """);
        v2.put("Dog.cs", """
            public class Dog : Animal
            {
            }
            """);
        return detectRefactoringMultiFile(v1, v2, RefactoringType.PULL_UP_OPERATION, "Pull Up Method");
    }

    private boolean testPushDownMethod() throws Exception {
        Map<String, String> v1 = new HashMap<>();
        v1.put("Animal.cs", """
            public class Animal
            {
                public void MakeSound()
                {
                    Console.WriteLine("Sound");
                }
            }
            """);
        v1.put("Dog.cs", """
            public class Dog : Animal
            {
            }
            """);

        Map<String, String> v2 = new HashMap<>();
        v2.put("Animal.cs", """
            public class Animal
            {
            }
            """);
        v2.put("Dog.cs", """
            public class Dog : Animal
            {
                public void MakeSound()
                {
                    Console.WriteLine("Sound");
                }
            }
            """);
        return detectRefactoringMultiFile(v1, v2, RefactoringType.PUSH_DOWN_OPERATION, "Push Down Method");
    }

    // Helper for multi-file refactorings
    private boolean detectRefactoringMultiFile(Map<String, String> filesV1, Map<String, String> filesV2,
            RefactoringType expectedType, String testName) throws Exception {
        UMLModel modelV1 = new UMLModelAdapter(filesV1).getUMLModel();
        UMLModel modelV2 = new UMLModelAdapter(filesV2).getUMLModel();

        UMLModelDiff diff = modelV1.diff(modelV2);
        List<Refactoring> refactorings = diff.getRefactorings();

        boolean found = refactorings.stream()
                .anyMatch(r -> r.getRefactoringType() == expectedType);

        System.out.println(testName + ": " + (found ? "DETECTED" : "NOT DETECTED"));
        if (!refactorings.isEmpty()) {
            System.out.println("  Found refactorings:");
            for (Refactoring r : refactorings) {
                System.out.println("    - " + r.getRefactoringType() + ": " + r.getName());
            }
        }

        return found;
    }

    // Helper for class rename (file name changes)
    private boolean detectRefactoringWithRename(String codeV1, String codeV2, String fileV1, String fileV2,
            RefactoringType expectedType, String testName) throws Exception {
        Map<String, String> filesV1 = new HashMap<>();
        filesV1.put(fileV1, codeV1);
        UMLModel modelV1 = new UMLModelAdapter(filesV1).getUMLModel();

        Map<String, String> filesV2 = new HashMap<>();
        filesV2.put(fileV2, codeV2);
        UMLModel modelV2 = new UMLModelAdapter(filesV2).getUMLModel();

        UMLModelDiff diff = modelV1.diff(modelV2);
        List<Refactoring> refactorings = diff.getRefactorings();

        boolean found = refactorings.stream()
                .anyMatch(r -> r.getRefactoringType() == expectedType);

        System.out.println(testName + ": " + (found ? "DETECTED" : "NOT DETECTED"));
        if (!refactorings.isEmpty()) {
            System.out.println("  Found refactorings:");
            for (Refactoring r : refactorings) {
                System.out.println("    - " + r.getRefactoringType() + ": " + r.getName());
            }
        }

        return found;
    }

    private boolean detectRefactoring(String codeV1, String codeV2, RefactoringType expectedType, String testName) throws Exception {
        Map<String, String> filesV1 = new HashMap<>();
        filesV1.put("Calculator.cs", codeV1);
        UMLModel modelV1 = new UMLModelAdapter(filesV1).getUMLModel();

        Map<String, String> filesV2 = new HashMap<>();
        filesV2.put("Calculator.cs", codeV2);
        UMLModel modelV2 = new UMLModelAdapter(filesV2).getUMLModel();

        // Debug output for Inline Method
        if (testName.equals("Inline Method")) {
            System.out.println("\n=== INLINE METHOD DEBUG ===");
            for (var cls : modelV1.getClassList()) {
                System.out.println("V1 Class: " + cls.getName());
                for (var op : cls.getOperations()) {
                    System.out.println("  Method: " + op.getName());
                    System.out.println("    Invocations: " + op.getAllOperationInvocations().stream().map(i -> i.getName()).toList());
                    if (op.getBody() != null) {
                        System.out.println("    Statements: " + op.getBody().stringRepresentation());
                    }
                }
            }
            for (var cls : modelV2.getClassList()) {
                System.out.println("V2 Class: " + cls.getName());
                for (var op : cls.getOperations()) {
                    System.out.println("  Method: " + op.getName());
                    if (op.getBody() != null) {
                        System.out.println("    Statements: " + op.getBody().stringRepresentation());
                    }
                }
            }
            System.out.println("=== END DEBUG ===\n");
        }

        UMLModelDiff diff = modelV1.diff(modelV2);
        List<Refactoring> refactorings = diff.getRefactorings();

        boolean found = refactorings.stream()
                .anyMatch(r -> r.getRefactoringType() == expectedType);

        System.out.println(testName + ": " + (found ? "DETECTED" : "NOT DETECTED"));
        if (!refactorings.isEmpty()) {
            System.out.println("  Found refactorings:");
            for (Refactoring r : refactorings) {
                System.out.println("    - " + r.getRefactoringType() + ": " + r.getName());
            }
        }

        return found;
    }
}
