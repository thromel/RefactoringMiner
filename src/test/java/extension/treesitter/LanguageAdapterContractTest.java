package extension.treesitter;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import extension.umladapter.UMLModelAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that language adapters fulfill the contract required
 * for refactoring detection algorithms to work correctly.
 *
 * This test suite runs the same assertions against multiple languages
 * to ensure consistent behavior across all supported languages.
 *
 * NOTE: Java uses a different parsing mechanism (Eclipse JDT), not the
 * UMLModelAdapter used by tree-sitter based languages. This test focuses
 * on the tree-sitter adapters (C#, Python, etc.).
 *
 * @see docs/LANGUAGE_ADAPTER_CONTRACT.md for the full contract specification
 */
class LanguageAdapterContractTest {

    // Test code samples for each language - equivalent semantics
    // NOTE: Java is excluded as it uses Eclipse JDT, not UMLModelAdapter
    private static final Map<String, String> SAMPLE_CODE = new HashMap<>();

    static {
        // C# sample
        SAMPLE_CODE.put("Calculator.cs", """
            public class Calculator {
                private int value;

                public int Add(int x) {
                    int result = value + x;
                    return result;
                }
            }
            """);

        // Python sample (equivalent semantics)
        SAMPLE_CODE.put("calculator.py", """
            class Calculator:
                def __init__(self):
                    self.value = 0

                def add(self, x):
                    result = self.value + x
                    return result
            """);
    }

    /**
     * Contract Requirement #1: UMLClass must be created for class declarations.
     */
    @ParameterizedTest(name = "testClassExtraction [{0}]")
    @ValueSource(strings = {"Calculator.cs", "calculator.py"})
    @DisplayName("Adapter must create UMLClass for class declarations")
    void testClassExtraction(String filename) throws Exception {
        UMLModel model = parseCode(filename);

        assertNotNull(model, "UMLModel should not be null");
        assertFalse(model.getClassList().isEmpty(),
            "Adapter must create UMLClass for class declarations in " + filename);

        // Find the Calculator class
        UMLClass calcClass = model.getClassList().stream()
            .filter(c -> c.getName().contains("Calculator"))
            .findFirst()
            .orElse(null);

        assertNotNull(calcClass,
            "Should find Calculator class in " + filename +
            ". Found classes: " + model.getClassList().stream().map(UMLClass::getName).toList());
    }

    /**
     * Contract Requirement #2: UMLAttribute must be created for class fields.
     * This is CRITICAL for Rename Attribute and Change Attribute Type refactorings.
     */
    @ParameterizedTest(name = "testAttributeExtraction [{0}]")
    @ValueSource(strings = {"Calculator.cs", "calculator.py"})
    @DisplayName("Adapter must create UMLAttribute for class fields")
    void testAttributeExtraction(String filename) throws Exception {
        UMLModel model = parseCode(filename);
        UMLClass calcClass = findCalculatorClass(model, filename);

        List<UMLAttribute> attributes = calcClass.getAttributes();
        assertFalse(attributes.isEmpty(),
            "Adapter must create UMLAttribute for class fields in " + filename +
            ". Found 0 attributes. This breaks Rename/Change Attribute Type detection.");

        // Verify the 'value' field exists
        boolean hasValueField = attributes.stream()
            .anyMatch(attr -> attr.getName().equals("value") || attr.getName().equals("self.value"));

        assertTrue(hasValueField,
            "Should find 'value' field attribute in " + filename +
            ". Found attributes: " + attributes.stream().map(UMLAttribute::getName).toList());
    }

    /**
     * Contract Requirement #3: UMLOperation must have OperationBody with statements.
     * This is CRITICAL for all method-level refactorings.
     */
    @ParameterizedTest(name = "testOperationBodyExtraction [{0}]")
    @ValueSource(strings = {"Calculator.cs", "calculator.py"})
    @DisplayName("Adapter must populate OperationBody for methods")
    void testOperationBodyExtraction(String filename) throws Exception {
        UMLModel model = parseCode(filename);
        UMLClass calcClass = findCalculatorClass(model, filename);

        // Find the add method
        UMLOperation addMethod = calcClass.getOperations().stream()
            .filter(op -> op.getName().equalsIgnoreCase("add"))
            .findFirst()
            .orElse(null);

        assertNotNull(addMethod,
            "Should find 'add' method in " + filename +
            ". Found methods: " + calcClass.getOperations().stream().map(UMLOperation::getName).toList());

        OperationBody body = addMethod.getBody();
        assertNotNull(body,
            "Adapter must populate OperationBody for methods in " + filename +
            ". Body is null, which breaks all method-level refactoring detection.");

        assertNotNull(body.getCompositeStatement(),
            "OperationBody must have CompositeStatement in " + filename);

        assertFalse(body.stringRepresentation().isEmpty(),
            "OperationBody must have string representation for statements in " + filename);
    }

    /**
     * Contract Requirement #4: VariableDeclaration must have initializer.
     * This is CRITICAL for Inline Variable and Extract Variable refactorings.
     */
    @ParameterizedTest(name = "testVariableDeclarationWithInitializer [{0}]")
    @ValueSource(strings = {"Calculator.cs", "calculator.py"})
    @DisplayName("Adapter must extract variable initializers")
    void testVariableDeclarationWithInitializer(String filename) throws Exception {
        UMLModel model = parseCode(filename);
        UMLClass calcClass = findCalculatorClass(model, filename);

        // Find the add method
        UMLOperation addMethod = calcClass.getOperations().stream()
            .filter(op -> op.getName().equalsIgnoreCase("add"))
            .findFirst()
            .orElse(null);

        assertNotNull(addMethod, "Should find 'add' method in " + filename);

        // Get variable declarations from method body
        List<VariableDeclaration> varDecls = addMethod.getAllVariableDeclarations();

        assertFalse(varDecls.isEmpty(),
            "Should find variable declarations in add() method in " + filename);

        // Find the 'result' variable
        VariableDeclaration resultVar = varDecls.stream()
            .filter(v -> v.getVariableName().equals("result"))
            .findFirst()
            .orElse(null);

        assertNotNull(resultVar,
            "Should find 'result' variable declaration in " + filename +
            ". Found vars: " + varDecls.stream().map(VariableDeclaration::getVariableName).toList());

        // CRITICAL: Verify initializer is populated
        assertNotNull(resultVar.getInitializer(),
            "CRITICAL: VariableDeclaration.getInitializer() must return the expression in " + filename +
            ". Without this, Inline Variable and Extract Variable refactorings CANNOT be detected!");
    }

    /**
     * Contract Requirement #5: OperationInvocations must be detected.
     * This is CRITICAL for Extract Method and Inline Method refactorings.
     */
    @ParameterizedTest(name = "testMethodInvocationDetection [{0}]")
    @ValueSource(strings = {"Calculator.cs", "calculator.py"})
    @DisplayName("Adapter must detect method invocations")
    void testMethodInvocationDetection(String filename) throws Exception {
        // Use a sample with a method call
        String codeWithCall = getCodeWithMethodCall(filename);

        Map<String, String> files = new HashMap<>();
        files.put(filename, codeWithCall);

        UMLModel model = new UMLModelAdapter(files).getUMLModel();
        UMLClass calcClass = findCalculatorClass(model, filename);

        // Find the calculate method (which calls helper)
        UMLOperation calculateMethod = calcClass.getOperations().stream()
            .filter(op -> op.getName().equalsIgnoreCase("calculate"))
            .findFirst()
            .orElse(null);

        assertNotNull(calculateMethod, "Should find 'calculate' method in " + filename);

        // Verify method invocations are detected
        var invocations = calculateMethod.getAllOperationInvocations();

        assertFalse(invocations.isEmpty(),
            "Adapter must detect method invocations in " + filename +
            ". Found 0 invocations. This breaks Extract Method and Inline Method detection.");
    }

    // Helper methods

    private UMLModel parseCode(String filename) throws Exception {
        String code = SAMPLE_CODE.get(filename);
        assertNotNull(code, "No sample code defined for " + filename);

        Map<String, String> files = new HashMap<>();
        files.put(filename, code);

        return new UMLModelAdapter(files).getUMLModel();
    }

    private UMLClass findCalculatorClass(UMLModel model, String filename) {
        UMLClass calcClass = model.getClassList().stream()
            .filter(c -> c.getName().contains("Calculator"))
            .findFirst()
            .orElse(null);

        assertNotNull(calcClass, "Should find Calculator class in " + filename);
        return calcClass;
    }

    private String getCodeWithMethodCall(String filename) {
        if (filename.endsWith(".java")) {
            return """
                public class Calculator {
                    private int value;

                    public int calculate(int x) {
                        int result = helper(x);
                        return result;
                    }

                    private int helper(int x) {
                        return value + x;
                    }
                }
                """;
        } else if (filename.endsWith(".cs")) {
            return """
                public class Calculator {
                    private int value;

                    public int Calculate(int x) {
                        int result = Helper(x);
                        return result;
                    }

                    private int Helper(int x) {
                        return value + x;
                    }
                }
                """;
        } else if (filename.endsWith(".py")) {
            return """
                class Calculator:
                    def __init__(self):
                        self.value = 0

                    def calculate(self, x):
                        result = self.helper(x)
                        return result

                    def helper(self, x):
                        return self.value + x
                """;
        }
        throw new IllegalArgumentException("Unknown file type: " + filename);
    }
}
