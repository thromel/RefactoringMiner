package extension.treesitter;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import extension.umladapter.UMLModelAdapter;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base test class that new language adapters should extend.
 * Provides standard test cases for validating that an adapter
 * fulfills the Language Adapter Contract.
 *
 * <p>To use this class for a new language:</p>
 * <ol>
 *   <li>Create a subclass for your language</li>
 *   <li>Override {@link #getSampleClassCode()} to return sample code in your language</li>
 *   <li>Override {@link #getFileExtension()} to return the file extension (e.g., ".rs")</li>
 *   <li>Optionally override {@link #getSampleCodeWithMethodCall()} for method invocation tests</li>
 *   <li>Run the tests - all should pass if your adapter is complete</li>
 * </ol>
 *
 * <p>Example:</p>
 * <pre>
 * class RustAdapterContractTest extends AbstractLanguageAdapterTest {
 *     @Override
 *     protected String getSampleClassCode() {
 *         return "struct Calculator { value: i32 } impl Calculator { fn add(&self, x: i32) -> i32 { let result = self.value + x; result } }";
 *     }
 *
 *     @Override
 *     protected String getFileExtension() {
 *         return ".rs";
 *     }
 * }
 * </pre>
 *
 * @see docs/LANGUAGE_ADAPTER_CONTRACT.md for the full contract specification
 */
public abstract class AbstractLanguageAdapterTest {

    /**
     * Override to provide sample code that contains:
     * <ul>
     *   <li>A class named "Calculator" (or containing "Calculator" in the name)</li>
     *   <li>A field named "value"</li>
     *   <li>A method named "add" (case-insensitive) with a local variable named "result"</li>
     * </ul>
     *
     * @return Sample source code in the target language
     */
    protected abstract String getSampleClassCode();

    /**
     * Override to return the file extension for this language.
     *
     * @return File extension including the dot (e.g., ".java", ".cs", ".py")
     */
    protected abstract String getFileExtension();

    /**
     * Override to provide sample code that contains a method calling another method.
     * Default implementation returns null, which will skip the method invocation test.
     *
     * <p>The code should contain:</p>
     * <ul>
     *   <li>A class named "Calculator"</li>
     *   <li>A method named "calculate" that calls another method</li>
     * </ul>
     *
     * @return Sample source code with method invocation, or null to skip test
     */
    protected String getSampleCodeWithMethodCall() {
        return null;
    }

    /**
     * Contract Requirement #1: Adapter must create UMLClass for class declarations.
     */
    @Test
    void testUMLClassCreated() throws Exception {
        UMLModel model = parseCode(getSampleClassCode());

        assertNotNull(model, "UMLModel should not be null");
        assertFalse(model.getClassList().isEmpty(),
            "Adapter must create UMLClass for class declarations");

        UMLClass calcClass = findCalculatorClass(model);
        assertNotNull(calcClass,
            "Should find Calculator class. Found classes: " +
            model.getClassList().stream().map(UMLClass::getName).toList());
    }

    /**
     * Contract Requirement #2: Adapter must create UMLAttribute for class fields.
     * Without this, Rename Attribute and Change Attribute Type refactorings will fail.
     */
    @Test
    void testUMLAttributesCreated() throws Exception {
        UMLModel model = parseCode(getSampleClassCode());
        UMLClass calcClass = findCalculatorClass(model);

        List<UMLAttribute> attributes = calcClass.getAttributes();
        assertFalse(attributes.isEmpty(),
            "Adapter must create UMLAttributes for class fields. Found 0 attributes. " +
            "This breaks Rename Attribute and Change Attribute Type detection.");

        // Verify the 'value' field exists (accounting for Python's self.value)
        boolean hasValueField = attributes.stream()
            .anyMatch(attr -> attr.getName().contains("value"));

        assertTrue(hasValueField,
            "Should find 'value' field attribute. Found: " +
            attributes.stream().map(UMLAttribute::getName).toList());
    }

    /**
     * Contract Requirement #3: Adapter must populate OperationBody for methods.
     * Without this, all method-level refactoring detection fails.
     */
    @Test
    void testOperationBodiesPopulated() throws Exception {
        UMLModel model = parseCode(getSampleClassCode());
        UMLClass calcClass = findCalculatorClass(model);

        assertFalse(calcClass.getOperations().isEmpty(),
            "Should have at least one method");

        // Find the add method
        UMLOperation addMethod = calcClass.getOperations().stream()
            .filter(op -> op.getName().equalsIgnoreCase("add"))
            .findFirst()
            .orElse(null);

        assertNotNull(addMethod,
            "Should find 'add' method. Found methods: " +
            calcClass.getOperations().stream().map(UMLOperation::getName).toList());

        OperationBody body = addMethod.getBody();
        assertNotNull(body,
            "Adapter must populate OperationBody for methods. " +
            "Body is null, which breaks all method-level refactoring detection.");

        assertNotNull(body.getCompositeStatement(),
            "OperationBody must have CompositeStatement");

        assertFalse(body.stringRepresentation().isEmpty(),
            "OperationBody must have string representation for statements");
    }

    /**
     * Contract Requirement #4: VariableDeclaration must have initializer.
     * Without this, Inline Variable and Extract Variable detection fails.
     */
    @Test
    void testVariableInitializersExtracted() throws Exception {
        UMLModel model = parseCode(getSampleClassCode());
        UMLClass calcClass = findCalculatorClass(model);

        // Find the add method
        UMLOperation addMethod = calcClass.getOperations().stream()
            .filter(op -> op.getName().equalsIgnoreCase("add"))
            .findFirst()
            .orElse(null);

        assertNotNull(addMethod, "Should find 'add' method");

        // Get variable declarations from method body
        List<VariableDeclaration> varDecls = addMethod.getAllVariableDeclarations();

        assertFalse(varDecls.isEmpty(),
            "Should find variable declarations in add() method");

        // Find the 'result' variable
        VariableDeclaration resultVar = varDecls.stream()
            .filter(v -> v.getVariableName().equals("result"))
            .findFirst()
            .orElse(null);

        assertNotNull(resultVar,
            "Should find 'result' variable declaration. Found vars: " +
            varDecls.stream().map(VariableDeclaration::getVariableName).toList());

        // CRITICAL: Verify initializer is populated
        assertNotNull(resultVar.getInitializer(),
            "CRITICAL: VariableDeclaration.getInitializer() must return the expression. " +
            "Without this, Inline Variable and Extract Variable refactorings CANNOT be detected!");
    }

    /**
     * Contract Requirement #5: Method invocations must be detected.
     * Without this, Extract Method and Inline Method detection fails.
     */
    @Test
    void testMethodInvocationsDetected() throws Exception {
        String codeWithCall = getSampleCodeWithMethodCall();
        if (codeWithCall == null) {
            // Skip test if not implemented
            return;
        }

        UMLModel model = parseCode(codeWithCall);
        UMLClass calcClass = findCalculatorClass(model);

        // Find the calculate method (which calls helper)
        UMLOperation calculateMethod = calcClass.getOperations().stream()
            .filter(op -> op.getName().equalsIgnoreCase("calculate"))
            .findFirst()
            .orElse(null);

        assertNotNull(calculateMethod, "Should find 'calculate' method");

        // Verify method invocations are detected
        List<AbstractCall> invocations = calculateMethod.getAllOperationInvocations();

        assertFalse(invocations.isEmpty(),
            "Adapter must detect method invocations. Found 0 invocations. " +
            "This breaks Extract Method and Inline Method detection.");
    }

    /**
     * Helper: Parse code and return UMLModel.
     */
    protected UMLModel parseCode(String code) throws Exception {
        Map<String, String> files = new HashMap<>();
        files.put("Sample" + getFileExtension(), code);
        return new UMLModelAdapter(files).getUMLModel();
    }

    /**
     * Helper: Find the Calculator class in the model.
     */
    protected UMLClass findCalculatorClass(UMLModel model) {
        return model.getClassList().stream()
            .filter(c -> c.getName().contains("Calculator"))
            .findFirst()
            .orElse(null);
    }
}
