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
 * AST flattener for TypeScript.
 * Based on CSharpASTFlattener but adapted for TypeScript syntax.
 */
public class TypeScriptASTFlattener implements LangASTFlattener {

    private final StringBuilder builder = new StringBuilder();
    private final LangASTNode root;

    public TypeScriptASTFlattener(LangASTNode root) {
        this.root = root;
    }

    @Override
    public String getResult() {
        return builder.toString();
    }

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
        if (method.getReturnTypeAnnotation() != null) {
            builder.append(": ").append(method.getReturnTypeAnnotation());
        }
        builder.append(" {\n");
        if (method.getBody() != null) {
            for (LangASTNode stmt : method.getBody().getStatements()) {
                if (stmt != null) stmt.accept(this);
            }
        }
        builder.append("}\n");
    }

    @Override
    public void visit(LangSingleVariableDeclaration var) {
        // For TypeScript local variables, output type annotation if available
        if (!var.isParameter() && var.getTypeAnnotation() != null) {
            builder.append(var.getTypeAnnotation().getName()).append(" ");
        }
        // Print variable name
        if (var.getLangSimpleName() != null) {
            var.getLangSimpleName().accept(this);
        }
        // For TypeScript parameters, type comes after name with ":"
        if (var.getTypeAnnotation() != null && var.isParameter()) {
            builder.append(": ").append(var.getTypeAnnotation().getName());
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
        builder.append("return ");
        if (stmt.getExpression() != null) {
            stmt.getExpression().accept(this);
        }
        builder.append(";\n");
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
        stmt.getCondition().accept(this);
        builder.append(") {\n");
        stmt.getBody().accept(this);
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
        stmt.getCondition().accept(this);
        builder.append(") {\n");
        stmt.getBody().accept(this);
        builder.append("}\n");
    }

    @Override
    public void visit(LangForStatement stmt) {
        builder.append("for (");
        List<? extends LangASTNode> inits = stmt.getInitializers();
        for (int i = 0; i < inits.size(); i++) {
            inits.get(i).accept(this);
            if (i < inits.size() - 1) builder.append(", ");
        }
        builder.append("; ");
        if (stmt.getCondition() != null) {
            stmt.getCondition().accept(this);
        }
        builder.append("; ");
        builder.append(") {\n");
        stmt.getBody().accept(this);
        builder.append("}\n");
    }

    @Override
    public void visit(LangExpressionStatement stmt) {
        if (stmt.getExpression() != null) {
            stmt.getExpression().accept(this);
        }
        builder.append(";\n");
    }

    @Override
    public void visit(LangAssignment assign) {
        assign.getLeftSide().accept(this);
        builder.append("=");
        assign.getRightSide().accept(this);
    }

    @Override
    public void visit(LangBooleanLiteral lit) {
        builder.append(lit.getValue());
    }

    @Override
    public void visit(LangNumberLiteral lit) {
        builder.append(lit.getValue());
    }

    @Override
    public void visit(LangStringLiteral lit) {
        builder.append("\"").append(lit.getValue()).append("\"");
    }

    @Override
    public void visit(LangListLiteral lit) {
        builder.append("[");
        List<LangASTNode> elems = lit.getElements();
        for (int i = 0; i < elems.size(); i++) {
            elems.get(i).accept(this);
            if (i < elems.size() - 1) builder.append(", ");
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
    public void visit(LangDictionaryLiteral dict) {
        builder.append("{");
        List<LangDictionaryLiteral.Entry> entries = dict.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            LangDictionaryLiteral.Entry e = entries.get(i);
            e.getKey().accept(this);
            builder.append(": ");
            e.getValue().accept(this);
            if (i < entries.size() - 1) builder.append(", ");
        }
        builder.append("}");
    }

    @Override
    public void visit(LangTupleLiteral tuple) {
        builder.append("(");
        List<LangASTNode> elems = tuple.getElements();
        for (int i = 0; i < elems.size(); i++) {
            elems.get(i).accept(this);
            if (i < elems.size() - 1) builder.append(", ");
        }
        builder.append(")");
    }

    @Override
    public void visit(LangImportStatement imp) {
        builder.append("import ");
        if (imp.getModuleName() != null) {
            builder.append(imp.getModuleName());
        }
        builder.append(";\n");
    }

    @Override
    public void visit(LangImportStatement.LangImportItem langImportItem) {
    }

    @Override
    public void visit(LangPrefixExpression expr) {
        builder.append(expr.getOperator().getSymbol());
        expr.getOperand().accept(this);
    }

    @Override
    public void visit(LangPostfixExpression expr) {
        expr.getOperand().accept(this);
        builder.append(expr.getOperator().getSymbol());
    }

    @Override
    public void visit(LangNullLiteral lit) {
        builder.append("null");
    }

    @Override
    public void visit(LangTryStatement stmt) {
        builder.append("try {\n");
        stmt.getBody().accept(this);
        builder.append("}");
        for (LangCatchClause c : stmt.getCatchClauses()) {
            c.accept(this);
        }
        if (stmt.getFinallyBlock() != null) {
            builder.append(" finally {\n");
            stmt.getFinallyBlock().accept(this);
            builder.append("}");
        }
        builder.append("\n");
    }

    @Override
    public void visit(LangCatchClause c) {
        builder.append(" catch (");
        if (c.getExceptionVariable() != null) {
            c.getExceptionVariable().accept(this);
        }
        builder.append(") {\n");
        c.getBody().accept(this);
        builder.append("}");
    }

    @Override
    public void visit(LangBreakStatement s) {
        builder.append("break;\n");
    }

    @Override
    public void visit(LangContinueStatement s) {
        builder.append("continue;\n");
    }

    @Override
    public void visit(LangDelStatement s) { }

    @Override
    public void visit(LangGlobalStatement s) { }

    @Override
    public void visit(LangPassStatement s) { }

    @Override
    public void visit(LangYieldStatement stmt) {
        builder.append("yield ");
        if (stmt.getExpression() != null) {
            stmt.getExpression().accept(this);
        }
        builder.append(";\n");
    }

    @Override
    public void visit(LangAnnotation a) {
        builder.append("@").append(a.getName());
    }

    @Override
    public void visit(LangAssertStatement s) {
        builder.append("assert(");
        if (s.getExpression() != null) {
            s.getExpression().accept(this);
        }
        builder.append(");\n");
    }

    @Override
    public void visit(LangThrowStatement s) {
        builder.append("throw ");
        List<LangASTNode> exprs = s.getExpressions();
        for (int i = 0; i < exprs.size(); i++) {
            exprs.get(i).accept(this);
            if (i < exprs.size() - 1) builder.append(", ");
        }
        builder.append(";\n");
    }

    @Override
    public void visit(LangWithContextItem item) { }

    @Override
    public void visit(LangWithStatement s) { }

    @Override
    public void visit(LangNonLocalStatement s) { }

    @Override
    public void visit(LangAsyncStatement s) { }

    @Override
    public void visit(LangAwaitExpression e) {
        builder.append("await ");
        e.getExpression().accept(this);
    }

    @Override
    public void visit(LangLambdaExpression e) {
        builder.append("(");
        for (int i = 0; i < e.getParameters().size(); i++) {
            e.getParameters().get(i).accept(this);
            if (i < e.getParameters().size() - 1) builder.append(", ");
        }
        builder.append(") => ");
        e.getBody().accept(this);
    }

    @Override
    public void visit(LangSwitchStatement s) {
        builder.append("switch (");
        s.getExpression().accept(this);
        builder.append(") {\n");
        for (LangCaseStatement cs : s.getCases()) {
            cs.accept(this);
        }
        builder.append("}\n");
    }

    @Override
    public void visit(LangCaseStatement c) {
        if (c.getPattern() != null) {
            builder.append("case ");
            c.getPattern().accept(this);
        } else {
            builder.append("default");
        }
        builder.append(":\n");
        if (c.getBody() != null) {
            c.getBody().accept(this);
        }
    }

    @Override
    public void visit(LangVariablePattern p) {
        builder.append(p.getVariableName());
    }

    @Override
    public void visit(LangLiteralPattern p) {
        builder.append(p.getValue());
    }

    @Override
    public void visit(LangComment c) {
        builder.append("// ").append(c.getContent()).append("\n");
    }

    @Override
    public void visit(LangTernaryExpression e) {
        if (e.getCondition() != null) e.getCondition().accept(this);
        builder.append(" ? ");
        if (e.getThenExpression() != null) e.getThenExpression().accept(this);
        builder.append(" : ");
        if (e.getElseExpression() != null) e.getElseExpression().accept(this);
    }

    @Override
    public void visit(LangIndexAccess e) {
        if (e.getTarget() != null) e.getTarget().accept(this);
        builder.append("[");
        if (e.getIndex() != null) e.getIndex().accept(this);
        builder.append("]");
    }

    @Override
    public void visit(LangSliceExpression e) { }

    @Override
    public void visit(LangEllipsisLiteral e) {
        builder.append("...");
    }

    @Override
    public void visit(LangComprehensionExpression e) { }

    @Override
    public void visit(LangComprehensionExpression.LangComprehensionClause e) { }

    @Override
    public void visit(LangParenthesizedExpression e) {
        builder.append("(");
        if (e.getParenthesizedExpression() != null) e.getParenthesizedExpression().accept(this);
        builder.append(")");
    }
}
