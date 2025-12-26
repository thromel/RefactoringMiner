package extension.treesitter;

import com.github.gumtreediff.tree.Tree;
import gr.uom.java.xmi.Constants;

import java.util.List;
import java.util.Set;

/**
 * Interface defining language-specific node type mappings and behaviors
 * for TreeSitter AST builders.
 *
 * Each language has different node type names in TreeSitter grammars.
 * This interface abstracts those differences so the common AST building
 * logic can be language-agnostic.
 */
public interface TreeSitterDialect {

    // ===== LANGUAGE IDENTIFICATION =====

    /**
     * Get the language name (e.g., "typescript", "csharp", "python").
     */
    String getLanguageName();

    /**
     * Get the language constants for refactoring detection.
     */
    Constants getConstants();

    // ===== TYPE DECLARATIONS =====

    /**
     * Get the node type(s) for class declarations.
     * TypeScript: "class_declaration"
     * C#: "class_declaration", "struct_declaration", "record_declaration"
     * Python: "class_definition"
     */
    Set<String> getClassDeclarationTypes();

    /**
     * Get the node type for interface declarations.
     * TypeScript: "interface_declaration"
     * C#: "interface_declaration"
     * Python: null (uses class with ABC base)
     */
    String getInterfaceDeclarationType();

    /**
     * Get the node type for enum declarations.
     */
    String getEnumDeclarationType();

    // ===== METHOD/FUNCTION DECLARATIONS =====

    /**
     * Get the node type for method definitions within a class.
     * TypeScript: "method_definition"
     * C#: "method_declaration"
     * Python: "function_definition"
     */
    String getMethodDeclarationType();

    /**
     * Get the node type for standalone function declarations.
     * TypeScript: "function_declaration"
     * C#: null (no standalone functions)
     * Python: "function_definition"
     */
    String getFunctionDeclarationType();

    /**
     * Get the node type for constructor declarations.
     * TypeScript: "method_definition" with name "constructor"
     * C#: "constructor_declaration"
     * Python: "function_definition" with name "__init__"
     */
    String getConstructorDeclarationType();

    // ===== VARIABLE DECLARATIONS =====

    /**
     * Get the node types for variable declarations.
     * TypeScript: "lexical_declaration", "variable_declaration"
     * C#: "local_declaration_statement", "field_declaration"
     * Python: "assignment" (Python uses assignment for declarations)
     */
    Set<String> getVariableDeclarationTypes();

    // ===== STATEMENTS =====

    /**
     * Get the node type for block statements.
     * TypeScript: "statement_block"
     * C#: "block"
     * Python: "block"
     */
    String getBlockType();

    /**
     * Get the node type for if statements.
     */
    default String getIfStatementType() {
        return "if_statement";
    }

    /**
     * Get the node type for for loops.
     */
    default String getForStatementType() {
        return "for_statement";
    }

    /**
     * Get the node type for foreach/for-in loops.
     * TypeScript: "for_in_statement"
     * C#: "foreach_statement"
     * Python: "for_statement" (same as regular for)
     */
    String getForEachStatementType();

    /**
     * Get the node type for while loops.
     */
    default String getWhileStatementType() {
        return "while_statement";
    }

    /**
     * Get the node type for do-while loops.
     * TypeScript/C#: "do_statement"
     * Python: null (no do-while in Python)
     */
    String getDoStatementType();

    /**
     * Get the node type for switch statements.
     */
    default String getSwitchStatementType() {
        return "switch_statement";
    }

    /**
     * Get the node type for try statements.
     */
    default String getTryStatementType() {
        return "try_statement";
    }

    /**
     * Get the node type for return statements.
     */
    default String getReturnStatementType() {
        return "return_statement";
    }

    /**
     * Get the node type for throw/raise statements.
     * TypeScript: "throw_statement"
     * C#: "throw_statement"
     * Python: "raise_statement"
     */
    String getThrowStatementType();

    /**
     * Get the node type for expression statements.
     */
    default String getExpressionStatementType() {
        return "expression_statement";
    }

    // ===== EXPRESSIONS =====

    /**
     * Get the node type for call/invocation expressions.
     * TypeScript: "call_expression"
     * C#: "invocation_expression"
     * Python: "call"
     */
    String getCallExpressionType();

    /**
     * Get the node type for member access expressions.
     * TypeScript: "member_expression"
     * C#: "member_access_expression"
     * Python: "attribute"
     */
    String getMemberExpressionType();

    /**
     * Get the node type for subscript/element access.
     * TypeScript: "subscript_expression"
     * C#: "element_access_expression"
     * Python: "subscript"
     */
    String getSubscriptExpressionType();

    /**
     * Get the node type for new/object creation expressions.
     * TypeScript: "new_expression"
     * C#: "object_creation_expression"
     * Python: null (uses call on class name)
     */
    String getNewExpressionType();

    /**
     * Get the node type for binary expressions.
     * TypeScript: "binary_expression"
     * C#: "binary_expression"
     * Python: "binary_operator", "comparison_operator", "boolean_operator"
     */
    Set<String> getBinaryExpressionTypes();

    /**
     * Get the node type for assignment expressions.
     * TypeScript: "assignment_expression"
     * C#: "assignment_expression"
     * Python: "assignment", "augmented_assignment"
     */
    Set<String> getAssignmentExpressionTypes();

    /**
     * Get the node type for lambda/arrow functions.
     * TypeScript: "arrow_function"
     * C#: "lambda_expression"
     * Python: "lambda"
     */
    String getLambdaExpressionType();

    /**
     * Get the node type for ternary/conditional expressions.
     * TypeScript: "ternary_expression"
     * C#: "conditional_expression"
     * Python: "conditional_expression"
     */
    String getTernaryExpressionType();

    // ===== PARAMETERS =====

    /**
     * Get the node type for parameter lists.
     * TypeScript: "formal_parameters"
     * C#: "parameter_list"
     * Python: "parameters"
     */
    String getParameterListType();

    /**
     * Get the node types for individual parameters.
     * TypeScript: "required_parameter", "optional_parameter"
     * C#: "parameter"
     * Python: "identifier", "default_parameter", "typed_parameter"
     */
    Set<String> getParameterTypes();

    // ===== IDENTIFIERS AND TYPES =====

    /**
     * Get the node types for identifiers.
     * TypeScript: "identifier", "property_identifier", "shorthand_property_identifier", "type_identifier"
     * C#: "identifier"
     * Python: "identifier"
     */
    Set<String> getIdentifierTypes();

    /**
     * Get the node type for type annotations.
     * TypeScript: "type_annotation"
     * C#: null (type comes before variable name)
     * Python: null (optional type hints)
     */
    String getTypeAnnotationType();

    // ===== MODIFIERS =====

    /**
     * Get the access modifier keywords.
     */
    default Set<String> getAccessModifiers() {
        return Set.of("public", "private", "protected", "internal");
    }

    /**
     * Get the method modifier keywords.
     */
    default Set<String> getMethodModifiers() {
        return Set.of("static", "async", "abstract", "virtual", "override", "readonly");
    }

    // ===== CLASS BODY =====

    /**
     * Get the node type for class body.
     * TypeScript: "class_body"
     * C#: "declaration_list"
     * Python: "block"
     */
    String getClassBodyType();

    /**
     * Get the node type for field/property definitions.
     * TypeScript: "public_field_definition", "property_signature"
     * C#: "field_declaration", "property_declaration"
     * Python: null (uses assignment in __init__)
     */
    Set<String> getFieldDeclarationTypes();

    // ===== INHERITANCE =====

    /**
     * Get the node type for class heritage/extends.
     * TypeScript: "class_heritage" -> "extends_clause"
     * C#: "base_list"
     * Python: "argument_list" after class name
     */
    String getExtendsClauseType();

    /**
     * Get the node type for implements clause.
     * TypeScript: "implements_clause"
     * C#: (part of base_list)
     * Python: null
     */
    String getImplementsClauseType();

    // ===== LITERALS =====

    /**
     * Get the node types for number literals.
     */
    Set<String> getNumberLiteralTypes();

    /**
     * Get the node types for string literals.
     */
    Set<String> getStringLiteralTypes();

    /**
     * Get the keywords for boolean true.
     */
    default String getTrueKeyword() {
        return "true";
    }

    /**
     * Get the keywords for boolean false.
     */
    default String getFalseKeyword() {
        return "false";
    }

    /**
     * Get the null/none keyword.
     */
    String getNullKeyword();

    /**
     * Get the this/self keyword.
     */
    String getThisKeyword();

    // ===== LANGUAGE-SPECIFIC BEHAVIORS =====

    /**
     * Check if a node represents a constructor.
     */
    default boolean isConstructor(Tree node, String methodName) {
        return "constructor".equals(methodName) || "__init__".equals(methodName);
    }

    /**
     * Check if a node represents an anonymous class.
     */
    default boolean isAnonymousClass(Tree node) {
        return false;
    }

    /**
     * Get all statement node types for this language.
     */
    Set<String> getStatementTypes();

    /**
     * Get all expression node types for this language.
     */
    Set<String> getExpressionTypes();
}
