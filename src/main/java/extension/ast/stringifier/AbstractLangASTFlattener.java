package extension.ast.stringifier;

import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.*;
import extension.ast.node.literal.*;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.pattern.LangLiteralPattern;
import extension.ast.node.pattern.LangVariablePattern;
import extension.ast.node.statement.*;
import extension.ast.node.unit.LangCompilationUnit;

import java.util.List;

/**
 * Abstract base class for language-specific AST flatteners.
 * Provides common implementations for most visit methods.
 *
 * Subclasses can override specific methods to customize formatting
 * for their language's syntax.
 */
public abstract class AbstractLangASTFlattener implements LangASTFlattener {

    protected final StringBuilder builder = new StringBuilder();
    protected final LangASTNode root;

    protected AbstractLangASTFlattener(LangASTNode root) {
        this.root = root;
    }

    @Override
    public String getResult() {
        return builder.toString();
    }

    /**
     * Get the statement terminator for this language.
     * Override to customize (e.g., ";\n" for C-like, "\n" for Python).
     */
    protected String getStatementTerminator() {
        return ";\n";
    }

    /**
     * Get the type annotation separator for this language.
     * TypeScript/Python use ":" after name, C#/Java use type before name.
     */
    protected boolean isTypeAfterName() {
        return false;  // C#/Java style: type before name
    }

    // ===== COMMON VISIT IMPLEMENTATIONS =====

    @Override
    public void visit(LangCompilationUnit unit) {
        List<LangASTNode> children = unit.getChildren();
        for (LangASTNode child : children) {
            if (child != null) child.accept(this);
        }
    }

    @Override
    public void visit(LangTypeDeclaration type) {
        builder.append("class ").append(type.getName());
        List<LangSimpleName> superClasses = type.getSuperClassNames();
        if (superClasses != null && !superClasses.isEmpty()) {
            builder.append(" : ");
            for (int i = 0; i < superClasses.size(); i++) {
                builder.append(superClasses.get(i).getIdentifier());
                if (i < superClasses.size() - 1) builder.append(", ");
            }
        }
        builder.append(" {\n");
        List<LangASTNode> children = type.getChildren();
        for (LangASTNode child : children) {
            if (child != null) child.accept(this);
        }
        builder.append("}\n");
    }

    @Override
    public void visit(LangMethodDeclaration method) {
        if (method.isAsync()) {
            builder.append("async ");
        }
        builder.append(method.getName()).append("(");
        List<LangSingleVariableDeclaration> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).accept(this);
            if (i < parameters.size() - 1) builder.append(", ");
        }
        builder.append(")");
        formatReturnType(method);
        builder.append(" {\n");
        if (method.getBody() != null) {
            for (LangASTNode stmt : method.getBody().getStatements()) {
                if (stmt != null) stmt.accept(this);
            }
        }
        builder.append("}\n");
    }

    /**
     * Format the return type annotation. Override for language-specific formatting.
     */
    protected void formatReturnType(LangMethodDeclaration method) {
        if (method.getReturnTypeAnnotation() != null) {
            builder.append(": ").append(method.getReturnTypeAnnotation());
        }
    }

    @Override
    public void visit(LangSingleVariableDeclaration var) {
        if (isTypeAfterName()) {
            // TypeScript/Python style: name: type
            if (var.getLangSimpleName() != null) {
                var.getLangSimpleName().accept(this);
            }
            if (var.getTypeAnnotation() != null) {
                builder.append(": ").append(var.getTypeAnnotation().getName());
            }
        } else {
            // C#/Java style: type name
            if (var.getTypeAnnotation() != null) {
                builder.append(var.getTypeAnnotation().getName()).append(" ");
            }
            if (var.getLangSimpleName() != null) {
                var.getLangSimpleName().accept(this);
            }
        }
        // Print initializer if available (for local variables, not parameters)
        if (var.getDefaultValue() != null && !var.isParameter()) {
            builder.append(" = ");
            var.getDefaultValue().accept(this);
        }
    }

    @Override
    public void visit(LangBlock block) {
        if (block.getStatements().isEmpty()) {
            return;
        }
        for (LangASTNode stmt : block.getStatements()) {
            if (stmt != null) stmt.accept(this);
        }
    }

    @Override
    public void visit(LangReturnStatement stmt) {
        builder.append("return");
        if (stmt.getExpression() != null) {
            builder.append(" ");
            stmt.getExpression().accept(this);
        }
        builder.append(getStatementTerminator());
    }

    @Override
    public void visit(LangInfixExpression expr) {
        expr.getLeft().accept(this);
        builder.append(" ").append(expr.getOperator().getSymbol()).append(" ");
        expr.getRight().accept(this);
    }

    @Override
    public void visit(LangMethodInvocation call) {
        if (call.getExpression() != null) {
            call.getExpression().accept(this);
            builder.append(".");
        }
        builder.append(call.getName());
        List<LangASTNode> arguments = call.getArguments();
        builder.append("(");
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                arguments.get(i).accept(this);
                if (i != arguments.size() - 1) builder.append(", ");
            }
        }
        builder.append(")");
    }

    @Override
    public void visit(LangSimpleName name) {
        builder.append(name.getIdentifier());
    }

    @Override
    public void visit(LangIfStatement stmt) {
        builder.append("if (");
        if (stmt.getCondition() != null) {
            stmt.getCondition().accept(this);
        }
        builder.append(") {\n");
        if (stmt.getBody() != null) {
            stmt.getBody().accept(this);
        }
        builder.append("}");
        LangASTNode elseBody = stmt.getElseBody();
        if (elseBody != null) {
            builder.append(" else {\n");
            elseBody.accept(this);
            builder.append("}");
        }
        builder.append("\n");
    }

    @Override
    public void visit(LangWhileStatement stmt) {
        builder.append("while (");
        if (stmt.getCondition() != null) {
            stmt.getCondition().accept(this);
        }
        builder.append(") {\n");
        if (stmt.getBody() != null) {
            stmt.getBody().accept(this);
        }
        builder.append("}\n");
    }

    @Override
    public void visit(LangForStatement stmt) {
        builder.append("for (");
        // Initializers
        List<? extends LangASTNode> initializers = stmt.getInitializers();
        if (initializers != null && !initializers.isEmpty()) {
            for (int i = 0; i < initializers.size(); i++) {
                initializers.get(i).accept(this);
                if (i < initializers.size() - 1) builder.append(", ");
            }
        }
        builder.append("; ");
        // Condition
        if (stmt.getCondition() != null) {
            stmt.getCondition().accept(this);
        }
        builder.append("; ");
        builder.append(") {\n");
        if (stmt.getBody() != null) {
            stmt.getBody().accept(this);
        }
        builder.append("}\n");
    }

    @Override
    public void visit(LangExpressionStatement stmt) {
        if (stmt.getExpression() != null) {
            stmt.getExpression().accept(this);
        }
        builder.append(getStatementTerminator());
    }

    @Override
    public void visit(LangAssignment stmt) {
        if (stmt.getLeftSide() != null) {
            stmt.getLeftSide().accept(this);
        }
        builder.append(" = ");
        if (stmt.getRightSide() != null) {
            stmt.getRightSide().accept(this);
        }
    }

    @Override
    public void visit(LangBooleanLiteral literal) {
        builder.append(literal.getValue());
    }

    @Override
    public void visit(LangNumberLiteral literal) {
        builder.append(literal.getValue());
    }

    @Override
    public void visit(LangStringLiteral literal) {
        builder.append("\"").append(literal.getValue()).append("\"");
    }

    @Override
    public void visit(LangListLiteral literal) {
        builder.append("[");
        List<LangASTNode> elements = literal.getElements();
        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).accept(this);
                if (i < elements.size() - 1) builder.append(", ");
            }
        }
        builder.append("]");
    }

    @Override
    public void visit(LangFieldAccess access) {
        if (access.getExpression() != null) {
            access.getExpression().accept(this);
            builder.append(".");
        }
        if (access.getName() != null) {
            access.getName().accept(this);
        }
    }

    @Override
    public void visit(LangDictionaryLiteral literal) {
        builder.append("{");
        List<LangDictionaryLiteral.Entry> entries = literal.getEntries();
        if (entries != null) {
            for (int i = 0; i < entries.size(); i++) {
                LangDictionaryLiteral.Entry e = entries.get(i);
                e.getKey().accept(this);
                builder.append(": ");
                e.getValue().accept(this);
                if (i < entries.size() - 1) builder.append(", ");
            }
        }
        builder.append("}");
    }

    @Override
    public void visit(LangTupleLiteral literal) {
        builder.append("(");
        List<LangASTNode> elements = literal.getElements();
        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).accept(this);
                if (i < elements.size() - 1) builder.append(", ");
            }
        }
        builder.append(")");
    }

    @Override
    public void visit(LangImportStatement stmt) {
        builder.append("import ");
        if (stmt.getModuleName() != null) {
            builder.append(stmt.getModuleName());
        }
        builder.append(getStatementTerminator());
    }

    @Override
    public void visit(LangPrefixExpression expr) {
        builder.append(expr.getOperator().getSymbol());
        if (expr.getOperand() != null) {
            expr.getOperand().accept(this);
        }
    }

    @Override
    public void visit(LangPostfixExpression expr) {
        if (expr.getOperand() != null) {
            expr.getOperand().accept(this);
        }
        builder.append(expr.getOperator().getSymbol());
    }

    @Override
    public void visit(LangNullLiteral literal) {
        builder.append("null");
    }

    @Override
    public void visit(LangTryStatement stmt) {
        builder.append("try {\n");
        if (stmt.getBody() != null) {
            stmt.getBody().accept(this);
        }
        builder.append("}");
        List<LangCatchClause> catchClauses = stmt.getCatchClauses();
        if (catchClauses != null) {
            for (LangCatchClause catchClause : catchClauses) {
                catchClause.accept(this);
            }
        }
        if (stmt.getFinallyBlock() != null) {
            builder.append(" finally {\n");
            stmt.getFinallyBlock().accept(this);
            builder.append("}");
        }
        builder.append("\n");
    }

    @Override
    public void visit(LangCatchClause clause) {
        builder.append(" catch (");
        if (clause.getExceptionVariable() != null) {
            clause.getExceptionVariable().accept(this);
        }
        builder.append(") {\n");
        if (clause.getBody() != null) {
            clause.getBody().accept(this);
        }
        builder.append("}");
    }

    @Override
    public void visit(LangBreakStatement stmt) {
        builder.append("break").append(getStatementTerminator());
    }

    @Override
    public void visit(LangContinueStatement stmt) {
        builder.append("continue").append(getStatementTerminator());
    }

    @Override
    public void visit(LangDelStatement stmt) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangGlobalStatement stmt) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangPassStatement stmt) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangYieldStatement stmt) {
        builder.append("yield");
        if (stmt.getExpression() != null) {
            builder.append(" ");
            stmt.getExpression().accept(this);
        }
        builder.append(getStatementTerminator());
    }

    @Override
    public void visit(LangAnnotation annotation) {
        builder.append("@").append(annotation.getName());
    }

    @Override
    public void visit(LangAssertStatement stmt) {
        builder.append("assert(");
        if (stmt.getExpression() != null) {
            stmt.getExpression().accept(this);
        }
        builder.append(")").append(getStatementTerminator());
    }

    @Override
    public void visit(LangThrowStatement stmt) {
        builder.append("throw ");
        List<LangASTNode> exprs = stmt.getExpressions();
        if (exprs != null) {
            for (int i = 0; i < exprs.size(); i++) {
                exprs.get(i).accept(this);
                if (i < exprs.size() - 1) builder.append(", ");
            }
        }
        builder.append(getStatementTerminator());
    }

    @Override
    public void visit(LangWithContextItem item) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangWithStatement stmt) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangNonLocalStatement stmt) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangAsyncStatement stmt) {
        // Language-specific, empty default
    }

    @Override
    public void visit(LangAwaitExpression expr) {
        builder.append("await ");
        if (expr.getExpression() != null) {
            expr.getExpression().accept(this);
        }
    }

    @Override
    public void visit(LangLambdaExpression expr) {
        builder.append("(");
        List<LangASTNode> params = expr.getParameters();
        if (params != null) {
            for (int i = 0; i < params.size(); i++) {
                params.get(i).accept(this);
                if (i < params.size() - 1) builder.append(", ");
            }
        }
        builder.append(") => ");
        if (expr.getBody() != null) {
            expr.getBody().accept(this);
        }
    }

    @Override
    public void visit(LangSwitchStatement stmt) {
        builder.append("switch (");
        if (stmt.getExpression() != null) {
            stmt.getExpression().accept(this);
        }
        builder.append(") {\n");
        List<LangCaseStatement> cases = stmt.getCases();
        if (cases != null) {
            for (LangCaseStatement caseStmt : cases) {
                caseStmt.accept(this);
            }
        }
        builder.append("}\n");
    }

    @Override
    public void visit(LangCaseStatement stmt) {
        if (stmt.getPattern() != null) {
            builder.append("case ");
            stmt.getPattern().accept(this);
        } else {
            builder.append("default");
        }
        builder.append(":\n");
        if (stmt.getBody() != null) {
            stmt.getBody().accept(this);
        }
    }

    @Override
    public void visit(LangVariablePattern pattern) {
        builder.append(pattern.getVariableName());
    }

    @Override
    public void visit(LangLiteralPattern pattern) {
        builder.append(pattern.getValue());
    }

    @Override
    public void visit(LangComment comment) {
        if (comment.isBlockComment()) {
            builder.append("/* ").append(comment.getContent()).append(" */\n");
        } else {
            builder.append("// ").append(comment.getContent()).append("\n");
        }
    }

    @Override
    public void visit(LangTernaryExpression expr) {
        if (expr.getCondition() != null) {
            expr.getCondition().accept(this);
        }
        builder.append(" ? ");
        if (expr.getThenExpression() != null) {
            expr.getThenExpression().accept(this);
        }
        builder.append(" : ");
        if (expr.getElseExpression() != null) {
            expr.getElseExpression().accept(this);
        }
    }

    @Override
    public void visit(LangIndexAccess access) {
        if (access.getTarget() != null) {
            access.getTarget().accept(this);
        }
        builder.append("[");
        if (access.getIndex() != null) {
            access.getIndex().accept(this);
        }
        builder.append("]");
    }

    @Override
    public void visit(LangSliceExpression expr) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangEllipsisLiteral literal) {
        builder.append("...");
    }

    @Override
    public void visit(LangParenthesizedExpression expr) {
        builder.append("(");
        if (expr.getParenthesizedExpression() != null) {
            expr.getParenthesizedExpression().accept(this);
        }
        builder.append(")");
    }

    @Override
    public void visit(LangComprehensionExpression expr) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangComprehensionExpression.LangComprehensionClause clause) {
        // Python-specific, empty default
    }

    @Override
    public void visit(LangImportStatement.LangImportItem item) {
        // Empty default
    }
}
