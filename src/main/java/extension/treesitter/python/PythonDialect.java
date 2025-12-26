package extension.treesitter.python;

import com.github.gumtreediff.tree.Tree;
import extension.treesitter.TreeSitterDialect;
import gr.uom.java.xmi.Constants;

import java.util.Set;

/**
 * TreeSitter dialect implementation for Python.
 * Defines Python-specific node type mappings.
 */
public class PythonDialect implements TreeSitterDialect {

    @Override
    public String getLanguageName() {
        return "python";
    }

    @Override
    public Constants getConstants() {
        return Constants.PYTHON;
    }

    // ===== TYPE DECLARATIONS =====

    @Override
    public Set<String> getClassDeclarationTypes() {
        return Set.of("class_definition");
    }

    @Override
    public String getInterfaceDeclarationType() {
        // Python uses ABC (Abstract Base Class) for interfaces
        return null;
    }

    @Override
    public String getEnumDeclarationType() {
        // Python enums are classes that inherit from Enum
        return null;
    }

    // ===== METHOD/FUNCTION DECLARATIONS =====

    @Override
    public String getMethodDeclarationType() {
        return "function_definition";
    }

    @Override
    public String getFunctionDeclarationType() {
        return "function_definition";
    }

    @Override
    public String getConstructorDeclarationType() {
        // Python uses __init__ method
        return "function_definition";
    }

    // ===== VARIABLE DECLARATIONS =====

    @Override
    public Set<String> getVariableDeclarationTypes() {
        // Python uses assignment for variable declarations
        return Set.of("assignment", "augmented_assignment");
    }

    // ===== STATEMENTS =====

    @Override
    public String getBlockType() {
        return "block";
    }

    @Override
    public String getForEachStatementType() {
        // Python for loops are always for-each style
        return "for_statement";
    }

    @Override
    public String getDoStatementType() {
        // Python doesn't have do-while loops
        return null;
    }

    @Override
    public String getSwitchStatementType() {
        // Python 3.10+ has match statement
        return "match_statement";
    }

    @Override
    public String getThrowStatementType() {
        return "raise_statement";
    }

    // ===== EXPRESSIONS =====

    @Override
    public String getCallExpressionType() {
        return "call";
    }

    @Override
    public String getMemberExpressionType() {
        return "attribute";
    }

    @Override
    public String getSubscriptExpressionType() {
        return "subscript";
    }

    @Override
    public String getNewExpressionType() {
        // Python uses regular function calls for object creation
        return null;
    }

    @Override
    public Set<String> getBinaryExpressionTypes() {
        return Set.of("binary_operator", "comparison_operator", "boolean_operator");
    }

    @Override
    public Set<String> getAssignmentExpressionTypes() {
        return Set.of("assignment", "augmented_assignment");
    }

    @Override
    public String getLambdaExpressionType() {
        return "lambda";
    }

    @Override
    public String getTernaryExpressionType() {
        return "conditional_expression";
    }

    // ===== PARAMETERS =====

    @Override
    public String getParameterListType() {
        return "parameters";
    }

    @Override
    public Set<String> getParameterTypes() {
        return Set.of("identifier", "default_parameter", "typed_parameter", "typed_default_parameter",
                      "list_splat_pattern", "dictionary_splat_pattern");
    }

    // ===== IDENTIFIERS AND TYPES =====

    @Override
    public Set<String> getIdentifierTypes() {
        return Set.of("identifier");
    }

    @Override
    public String getTypeAnnotationType() {
        // Python type hints are optional
        return "type";
    }

    // ===== MODIFIERS =====

    @Override
    public Set<String> getAccessModifiers() {
        // Python uses naming conventions for access (_, __)
        return Set.of();
    }

    @Override
    public Set<String> getMethodModifiers() {
        return Set.of("async", "staticmethod", "classmethod", "property", "abstractmethod");
    }

    // ===== CLASS BODY =====

    @Override
    public String getClassBodyType() {
        return "block";
    }

    @Override
    public Set<String> getFieldDeclarationTypes() {
        // Python doesn't have explicit field declarations
        return Set.of();
    }

    // ===== INHERITANCE =====

    @Override
    public String getExtendsClauseType() {
        // Python uses argument_list after class name
        return "argument_list";
    }

    @Override
    public String getImplementsClauseType() {
        // Python doesn't have explicit interface implementation
        return null;
    }

    // ===== LITERALS =====

    @Override
    public Set<String> getNumberLiteralTypes() {
        return Set.of("integer", "float");
    }

    @Override
    public Set<String> getStringLiteralTypes() {
        return Set.of("string", "concatenated_string");
    }

    @Override
    public String getTrueKeyword() {
        return "True";
    }

    @Override
    public String getFalseKeyword() {
        return "False";
    }

    @Override
    public String getNullKeyword() {
        return "None";
    }

    @Override
    public String getThisKeyword() {
        return "self";
    }

    // ===== LANGUAGE-SPECIFIC BEHAVIORS =====

    @Override
    public boolean isConstructor(Tree node, String methodName) {
        return "__init__".equals(methodName);
    }

    @Override
    public Set<String> getStatementTypes() {
        return Set.of(
            "block",
            "if_statement",
            "for_statement",
            "while_statement",
            "try_statement",
            "with_statement",
            "match_statement",
            "return_statement",
            "raise_statement",
            "assert_statement",
            "pass_statement",
            "break_statement",
            "continue_statement",
            "import_statement",
            "import_from_statement",
            "global_statement",
            "nonlocal_statement",
            "expression_statement"
        );
    }

    @Override
    public Set<String> getExpressionTypes() {
        return Set.of(
            "call",
            "attribute",
            "subscript",
            "lambda",
            "conditional_expression",
            "boolean_operator",
            "comparison_operator",
            "binary_operator",
            "unary_operator",
            "not_operator",
            "parenthesized_expression",
            "await",
            "list",
            "tuple",
            "dictionary",
            "set",
            "list_comprehension",
            "dict_comprehension",
            "set_comprehension",
            "generator_expression"
        );
    }
}
