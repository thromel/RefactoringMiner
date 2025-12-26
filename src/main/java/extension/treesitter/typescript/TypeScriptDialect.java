package extension.treesitter.typescript;

import com.github.gumtreediff.tree.Tree;
import extension.treesitter.TreeSitterDialect;
import gr.uom.java.xmi.Constants;

import java.util.Set;

/**
 * TreeSitter dialect implementation for TypeScript.
 * Defines TypeScript-specific node type mappings.
 */
public class TypeScriptDialect implements TreeSitterDialect {

    @Override
    public String getLanguageName() {
        return "typescript";
    }

    @Override
    public Constants getConstants() {
        return Constants.TYPESCRIPT;
    }

    // ===== TYPE DECLARATIONS =====

    @Override
    public Set<String> getClassDeclarationTypes() {
        return Set.of("class_declaration");
    }

    @Override
    public String getInterfaceDeclarationType() {
        return "interface_declaration";
    }

    @Override
    public String getEnumDeclarationType() {
        return "enum_declaration";
    }

    // ===== METHOD/FUNCTION DECLARATIONS =====

    @Override
    public String getMethodDeclarationType() {
        return "method_definition";
    }

    @Override
    public String getFunctionDeclarationType() {
        return "function_declaration";
    }

    @Override
    public String getConstructorDeclarationType() {
        // TypeScript uses method_definition with name "constructor"
        return "method_definition";
    }

    // ===== VARIABLE DECLARATIONS =====

    @Override
    public Set<String> getVariableDeclarationTypes() {
        return Set.of("lexical_declaration", "variable_declaration");
    }

    // ===== STATEMENTS =====

    @Override
    public String getBlockType() {
        return "statement_block";
    }

    @Override
    public String getForEachStatementType() {
        return "for_in_statement";
    }

    @Override
    public String getDoStatementType() {
        return "do_statement";
    }

    @Override
    public String getThrowStatementType() {
        return "throw_statement";
    }

    // ===== EXPRESSIONS =====

    @Override
    public String getCallExpressionType() {
        return "call_expression";
    }

    @Override
    public String getMemberExpressionType() {
        return "member_expression";
    }

    @Override
    public String getSubscriptExpressionType() {
        return "subscript_expression";
    }

    @Override
    public String getNewExpressionType() {
        return "new_expression";
    }

    @Override
    public Set<String> getBinaryExpressionTypes() {
        return Set.of("binary_expression");
    }

    @Override
    public Set<String> getAssignmentExpressionTypes() {
        return Set.of("assignment_expression", "augmented_assignment_expression");
    }

    @Override
    public String getLambdaExpressionType() {
        return "arrow_function";
    }

    @Override
    public String getTernaryExpressionType() {
        return "ternary_expression";
    }

    // ===== PARAMETERS =====

    @Override
    public String getParameterListType() {
        return "formal_parameters";
    }

    @Override
    public Set<String> getParameterTypes() {
        return Set.of("required_parameter", "optional_parameter", "rest_parameter");
    }

    // ===== IDENTIFIERS AND TYPES =====

    @Override
    public Set<String> getIdentifierTypes() {
        return Set.of("identifier", "property_identifier", "shorthand_property_identifier", "type_identifier");
    }

    @Override
    public String getTypeAnnotationType() {
        return "type_annotation";
    }

    // ===== CLASS BODY =====

    @Override
    public String getClassBodyType() {
        return "class_body";
    }

    @Override
    public Set<String> getFieldDeclarationTypes() {
        return Set.of("public_field_definition", "property_signature", "field_definition");
    }

    // ===== INHERITANCE =====

    @Override
    public String getExtendsClauseType() {
        return "extends_clause";
    }

    @Override
    public String getImplementsClauseType() {
        return "implements_clause";
    }

    // ===== LITERALS =====

    @Override
    public Set<String> getNumberLiteralTypes() {
        return Set.of("number");
    }

    @Override
    public Set<String> getStringLiteralTypes() {
        return Set.of("string", "template_string");
    }

    @Override
    public String getNullKeyword() {
        return "null";
    }

    @Override
    public String getThisKeyword() {
        return "this";
    }

    // ===== LANGUAGE-SPECIFIC BEHAVIORS =====

    @Override
    public boolean isConstructor(Tree node, String methodName) {
        return "constructor".equals(methodName);
    }

    @Override
    public Set<String> getStatementTypes() {
        return Set.of(
            "statement_block",
            "if_statement",
            "for_statement",
            "for_in_statement",
            "while_statement",
            "do_statement",
            "switch_statement",
            "try_statement",
            "return_statement",
            "break_statement",
            "continue_statement",
            "throw_statement",
            "expression_statement",
            "lexical_declaration",
            "variable_declaration"
        );
    }

    @Override
    public Set<String> getExpressionTypes() {
        return Set.of(
            "assignment_expression",
            "call_expression",
            "member_expression",
            "subscript_expression",
            "new_expression",
            "binary_expression",
            "unary_expression",
            "update_expression",
            "ternary_expression",
            "arrow_function",
            "parenthesized_expression",
            "await_expression",
            "template_string",
            "array",
            "object"
        );
    }
}
