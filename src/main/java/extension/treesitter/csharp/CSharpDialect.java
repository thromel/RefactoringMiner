package extension.treesitter.csharp;

import com.github.gumtreediff.tree.Tree;
import extension.treesitter.TreeSitterDialect;
import gr.uom.java.xmi.Constants;

import java.util.Set;

/**
 * TreeSitter dialect implementation for C#.
 * Defines C#-specific node type mappings.
 */
public class CSharpDialect implements TreeSitterDialect {

    @Override
    public String getLanguageName() {
        return "csharp";
    }

    @Override
    public Constants getConstants() {
        return Constants.CSHARP;
    }

    // ===== TYPE DECLARATIONS =====

    @Override
    public Set<String> getClassDeclarationTypes() {
        return Set.of("class_declaration", "struct_declaration", "record_declaration");
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
        return "method_declaration";
    }

    @Override
    public String getFunctionDeclarationType() {
        // C# doesn't have standalone functions
        return null;
    }

    @Override
    public String getConstructorDeclarationType() {
        return "constructor_declaration";
    }

    // ===== VARIABLE DECLARATIONS =====

    @Override
    public Set<String> getVariableDeclarationTypes() {
        return Set.of("local_declaration_statement", "field_declaration");
    }

    // ===== STATEMENTS =====

    @Override
    public String getBlockType() {
        return "block";
    }

    @Override
    public String getForEachStatementType() {
        return "foreach_statement";
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
        return "invocation_expression";
    }

    @Override
    public String getMemberExpressionType() {
        return "member_access_expression";
    }

    @Override
    public String getSubscriptExpressionType() {
        return "element_access_expression";
    }

    @Override
    public String getNewExpressionType() {
        return "object_creation_expression";
    }

    @Override
    public Set<String> getBinaryExpressionTypes() {
        return Set.of("binary_expression");
    }

    @Override
    public Set<String> getAssignmentExpressionTypes() {
        return Set.of("assignment_expression");
    }

    @Override
    public String getLambdaExpressionType() {
        return "lambda_expression";
    }

    @Override
    public String getTernaryExpressionType() {
        return "conditional_expression";
    }

    // ===== PARAMETERS =====

    @Override
    public String getParameterListType() {
        return "parameter_list";
    }

    @Override
    public Set<String> getParameterTypes() {
        return Set.of("parameter");
    }

    // ===== IDENTIFIERS AND TYPES =====

    @Override
    public Set<String> getIdentifierTypes() {
        return Set.of("identifier");
    }

    @Override
    public String getTypeAnnotationType() {
        // C# has type before variable name, not as annotation
        return null;
    }

    // ===== MODIFIERS =====

    @Override
    public Set<String> getAccessModifiers() {
        return Set.of("public", "private", "protected", "internal", "protected internal", "private protected");
    }

    @Override
    public Set<String> getMethodModifiers() {
        return Set.of("static", "async", "abstract", "virtual", "override", "readonly", "sealed", "extern", "new", "partial");
    }

    // ===== CLASS BODY =====

    @Override
    public String getClassBodyType() {
        return "declaration_list";
    }

    @Override
    public Set<String> getFieldDeclarationTypes() {
        return Set.of("field_declaration", "property_declaration");
    }

    // ===== INHERITANCE =====

    @Override
    public String getExtendsClauseType() {
        return "base_list";
    }

    @Override
    public String getImplementsClauseType() {
        // C# uses same base_list for both inheritance and implementation
        return "base_list";
    }

    // ===== LITERALS =====

    @Override
    public Set<String> getNumberLiteralTypes() {
        return Set.of("integer_literal", "real_literal");
    }

    @Override
    public Set<String> getStringLiteralTypes() {
        return Set.of("string_literal", "verbatim_string_literal", "raw_string_literal", "interpolated_string_expression");
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
        return "constructor_declaration".equals(node.getType().name);
    }

    @Override
    public Set<String> getStatementTypes() {
        return Set.of(
            "block",
            "if_statement",
            "for_statement",
            "foreach_statement",
            "while_statement",
            "do_statement",
            "switch_statement",
            "try_statement",
            "return_statement",
            "break_statement",
            "continue_statement",
            "throw_statement",
            "expression_statement",
            "local_declaration_statement",
            "using_statement",
            "lock_statement"
        );
    }

    @Override
    public Set<String> getExpressionTypes() {
        return Set.of(
            "assignment_expression",
            "invocation_expression",
            "member_access_expression",
            "element_access_expression",
            "object_creation_expression",
            "binary_expression",
            "prefix_unary_expression",
            "postfix_unary_expression",
            "conditional_expression",
            "lambda_expression",
            "parenthesized_expression",
            "cast_expression",
            "await_expression",
            "interpolated_string_expression",
            "this_expression",
            "base_expression",
            "typeof_expression",
            "default_expression",
            "nameof_expression"
        );
    }
}
