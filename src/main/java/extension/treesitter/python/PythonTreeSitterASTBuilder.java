package extension.treesitter.python;

import com.github.gumtreediff.tree.Tree;
import extension.ast.node.*;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.*;
import extension.ast.node.literal.*;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.metadata.comment.LangComment;
import extension.ast.node.statement.*;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import extension.treesitter.AbstractTreeSitterASTBuilder;
import gr.uom.java.xmi.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-sitter AST builder for Python.
 * Converts Tree-sitter Python AST into LangASTNode hierarchy.
 */
public class PythonTreeSitterASTBuilder extends AbstractTreeSitterASTBuilder {

    @Override
    public String getLanguage() {
        return "python";
    }

    @Override
    protected LangASTNode buildFromRoot(Tree root) {
        // Tree-sitter Python root is "module"
        return visitModule(root);
    }

    private LangCompilationUnit visitModule(Tree node) {
        PositionInfo positionInfo = extractPosition(node);
        LangCompilationUnit compilationUnit = LangASTNodeFactory.createCompilationUnit(positionInfo);
        compilationUnit.setLanguage(LangSupportedEnum.PYTHON);

        for (Tree child : getChildren(node)) {
            LangASTNode astNode = visitNode(child);
            if (astNode == null) continue;

            if (astNode instanceof LangTypeDeclaration typeDecl) {
                compilationUnit.addType(typeDecl);
            } else if (astNode instanceof LangMethodDeclaration methodDecl) {
                compilationUnit.addMethod(methodDecl);
            } else if (astNode instanceof LangComment comment) {
                compilationUnit.addComment(comment);
            } else if (astNode instanceof LangImportStatement importStmt) {
                compilationUnit.addImport(importStmt);
            } else if (astNode instanceof LangExpressionStatement exprStmt) {
                if (exprStmt.getExpression() instanceof LangAssignment assignment) {
                    compilationUnit.addAssignment(assignment);
                } else if (exprStmt.getExpression() instanceof LangStringLiteral str) {
                    LangComment comment = new LangComment(str.getValue(), false, true, extractPosition(child));
                    compilationUnit.addComment(comment);
                } else {
                    compilationUnit.addStatement(astNode);
                }
            } else {
                compilationUnit.addStatement(astNode);
            }
        }

        return compilationUnit;
    }

    @Override
    protected LangASTNode visitNode(Tree node) {
        String nodeType = getNodeType(node);

        return switch (nodeType) {
            // Definitions
            case "class_definition" -> visitClassDefinition(node);
            case "function_definition" -> visitFunctionDefinition(node);
            case "decorated_definition" -> visitDecoratedDefinition(node);

            // Statements
            case "if_statement" -> visitIfStatement(node);
            case "for_statement" -> visitForStatement(node);
            case "while_statement" -> visitWhileStatement(node);
            case "try_statement" -> visitTryStatement(node);
            case "with_statement" -> visitWithStatement(node);
            case "match_statement" -> visitMatchStatement(node);
            case "return_statement" -> visitReturnStatement(node);
            case "raise_statement" -> visitRaiseStatement(node);
            case "assert_statement" -> visitAssertStatement(node);
            case "pass_statement" -> visitPassStatement(node);
            case "break_statement" -> visitBreakStatement(node);
            case "continue_statement" -> visitContinueStatement(node);
            case "import_statement" -> visitImportStatement(node);
            case "import_from_statement" -> visitImportFromStatement(node);
            case "global_statement" -> visitGlobalStatement(node);
            case "nonlocal_statement" -> visitNonlocalStatement(node);
            case "expression_statement" -> visitExpressionStatement(node);

            // Assignments
            case "assignment" -> visitAssignment(node);
            case "augmented_assignment" -> visitAugmentedAssignment(node);

            // Expressions
            case "call" -> visitCall(node);
            case "attribute" -> visitAttribute(node);
            case "subscript" -> visitSubscript(node);
            case "lambda" -> visitLambda(node);
            case "conditional_expression" -> visitConditionalExpression(node);
            case "boolean_operator" -> visitBooleanOperator(node);
            case "comparison_operator" -> visitComparisonOperator(node);
            case "binary_operator" -> visitBinaryOperator(node);
            case "unary_operator" -> visitUnaryOperator(node);
            case "not_operator" -> visitNotOperator(node);
            case "parenthesized_expression" -> visitParenthesizedExpression(node);
            case "await" -> visitAwait(node);

            // Literals
            case "identifier" -> visitIdentifier(node);
            case "integer" -> visitInteger(node);
            case "float" -> visitFloat(node);
            case "string" -> visitString(node);
            case "concatenated_string" -> visitConcatenatedString(node);
            case "true" -> visitTrue(node);
            case "false" -> visitFalse(node);
            case "none" -> visitNone(node);
            case "ellipsis" -> visitEllipsis(node);

            // Collections
            case "list" -> visitList(node);
            case "tuple" -> visitTuple(node);
            case "dictionary" -> visitDictionary(node);
            case "set" -> visitSet(node);
            case "list_comprehension" -> visitListComprehension(node);
            case "dict_comprehension" -> visitDictComprehension(node);
            case "set_comprehension" -> visitSetComprehension(node);
            case "generator_expression" -> visitGeneratorExpression(node);

            // Block
            case "block" -> visitBlock(node);

            // Skip or handle specially
            case "comment" -> visitComment(node);
            case "decorator" -> null; // Handled in decorated_definition
            case "parameters" -> null; // Handled in function_definition

            // Default: try to handle unknown nodes
            default -> handleUnknownNode(node);
        };
    }

    // ===== CLASS AND FUNCTION DEFINITIONS =====

    private LangTypeDeclaration visitClassDefinition(Tree node) {
        String className = "";
        Tree body = null;
        List<LangSimpleName> superClasses = new ArrayList<>();
        List<LangAnnotation> annotations = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> className = getNodeText(child);
                case "argument_list" -> superClasses = extractSuperClasses(child);
                case "block" -> body = child;
            }
        }

        PositionInfo positionInfo = extractPosition(node);
        LangTypeDeclaration typeDecl = LangASTNodeFactory.createTypeDeclaration(className, positionInfo);
        typeDecl.setActualSignature("class " + className);
        typeDecl.setVisibility(Visibility.PUBLIC);
        typeDecl.setTopLevel(true);
        typeDecl.setSuperClassNames(superClasses);

        // Check for abstract class markers
        boolean isAbstract = superClasses.stream()
                .anyMatch(s -> s.getIdentifier().contains("ABC") || s.getIdentifier().contains("ABCMeta"));
        typeDecl.setAbstract(isAbstract);

        // Check for enum
        boolean isEnum = superClasses.stream()
                .anyMatch(s -> s.getIdentifier().equals("Enum") || s.getIdentifier().equals("IntEnum"));
        typeDecl.setEnum(isEnum);

        // Process body
        if (body != null) {
            for (Tree stmt : getChildren(body)) {
                LangASTNode astNode = visitNode(stmt);
                if (astNode instanceof LangMethodDeclaration method) {
                    typeDecl.addMethod(method);
                } else if (astNode instanceof LangExpressionStatement exprStmt) {
                    if (exprStmt.getExpression() instanceof LangAssignment assignment) {
                        typeDecl.addAssignment(assignment);
                    } else if (exprStmt.getExpression() instanceof LangStringLiteral str) {
                        LangComment comment = new LangComment(str.getValue(), false, true, extractPosition(stmt));
                        typeDecl.addComment(comment);
                    } else {
                        typeDecl.addStatement(astNode);
                    }
                } else if (astNode != null) {
                    typeDecl.addStatement(astNode);
                }
            }
        }

        return typeDecl;
    }

    private List<LangSimpleName> extractSuperClasses(Tree argList) {
        List<LangSimpleName> superClasses = new ArrayList<>();
        for (Tree child : getChildren(argList)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "attribute".equals(childType)) {
                String name = getNodeText(child);
                if (!name.contains("metaclass=")) {
                    superClasses.add(LangASTNodeFactory.createSimpleName(name, extractPosition(child)));
                }
            } else if ("argument".equals(childType)) {
                // Handle keyword arguments like metaclass=ABCMeta
                String text = getNodeText(child);
                if (!text.contains("metaclass=")) {
                    Tree first = getChild(child, 0);
                    if (first != null) {
                        String name = getNodeText(first);
                        superClasses.add(LangASTNodeFactory.createSimpleName(name, extractPosition(first)));
                    }
                }
            }
        }
        return superClasses;
    }

    private LangMethodDeclaration visitFunctionDefinition(Tree node) {
        String methodName = "";
        List<LangSingleVariableDeclaration> parameters = new ArrayList<>();
        Tree body = null;
        String returnType = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> methodName = getNodeText(child);
                case "parameters" -> parameters = extractParameters(child);
                case "block" -> body = child;
                case "type" -> returnType = getNodeText(child);
            }
        }

        LangBlock bodyBlock = body != null ? visitBlock(body) : null;

        // Extract docstring if present
        String docstring = null;
        if (bodyBlock != null && !bodyBlock.getStatements().isEmpty()) {
            LangASTNode firstStmt = bodyBlock.getStatements().get(0);
            if (firstStmt instanceof LangExpressionStatement exprStmt &&
                    exprStmt.getExpression() instanceof LangStringLiteral str) {
                docstring = str.getValue();
                bodyBlock.getStatements().remove(0);
            }
        }

        PositionInfo positionInfo = extractPosition(node);
        LangMethodDeclaration methodDecl = LangASTNodeFactory.createMethodDeclaration(
                methodName, positionInfo, parameters, bodyBlock);

        methodDecl.setConstructor("__init__".equals(methodName));
        methodDecl.setVisibility(getMethodVisibility(methodName));
        methodDecl.setCleanName(extractCleanName(methodName));

        if (returnType != null) {
            methodDecl.setReturnTypeAnnotation(returnType);
        } else {
            // Check if method has return statement
            boolean hasReturn = bodyBlock != null && bodyBlock.getStatements().stream()
                    .anyMatch(s -> s.getNodeType() == NodeTypeEnum.RETURN_STATEMENT);
            methodDecl.setReturnTypeAnnotation(hasReturn ? TypeObjectEnum.OBJECT.getName() : "None");
        }

        if (docstring != null) {
            LangComment comment = new LangComment(docstring, false, true, positionInfo);
            methodDecl.addComment(comment);
        }

        return methodDecl;
    }

    private List<LangSingleVariableDeclaration> extractParameters(Tree paramsNode) {
        List<LangSingleVariableDeclaration> params = new ArrayList<>();

        for (Tree child : getChildren(paramsNode)) {
            String childType = getNodeType(child);
            LangSingleVariableDeclaration param = null;

            switch (childType) {
                case "identifier" -> {
                    String name = getNodeText(child);
                    param = LangASTNodeFactory.createSingleVariableDeclaration(
                            name, null, extractPosition(child));
                    param.setParameter(true);
                }
                case "typed_parameter" -> {
                    String name = findChildText(child, "identifier");
                    param = LangASTNodeFactory.createSingleVariableDeclaration(
                            name, null, extractPosition(child));
                    param.setParameter(true);
                    // Extract type annotation if present
                    Tree typeNode = findChildByType(child, "type");
                    if (typeNode != null) {
                        TypeObjectEnum typeEnum = TypeObjectEnum.fromType(getNodeText(typeNode));
                        param.setTypeAnnotation(typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT);
                    }
                }
                case "default_parameter" -> {
                    String name = findChildText(child, "identifier");
                    Tree defaultValue = getChild(child, child.getChildren().size() - 1);
                    LangASTNode defaultExpr = defaultValue != null ? visitNode(defaultValue) : null;
                    param = LangASTNodeFactory.createSingleVariableDeclaration(
                            name, defaultExpr, extractPosition(child));
                    param.setParameter(true);
                }
                case "typed_default_parameter" -> {
                    String name = findChildText(child, "identifier");
                    Tree defaultValue = getChild(child, child.getChildren().size() - 1);
                    LangASTNode defaultExpr = defaultValue != null ? visitNode(defaultValue) : null;
                    param = LangASTNodeFactory.createSingleVariableDeclaration(
                            name, defaultExpr, extractPosition(child));
                    param.setParameter(true);
                }
                case "list_splat_pattern", "dictionary_splat_pattern" -> {
                    Tree idNode = findChildByType(child, "identifier");
                    String name = idNode != null ? getNodeText(idNode) : getNodeText(child);
                    param = LangASTNodeFactory.createSingleVariableDeclaration(
                            name, null, extractPosition(child));
                    param.setParameter(true);
                    param.setVarArgs("list_splat_pattern".equals(childType));
                    param.setKwArgs("dictionary_splat_pattern".equals(childType));
                }
            }

            if (param != null) {
                params.add(param);
            }
        }

        return params;
    }

    private LangASTNode visitDecoratedDefinition(Tree node) {
        List<LangAnnotation> decorators = new ArrayList<>();
        LangASTNode definition = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "decorator" -> {
                    LangAnnotation annotation = visitDecorator(child);
                    if (annotation != null) {
                        decorators.add(annotation);
                    }
                }
                case "function_definition" -> definition = visitFunctionDefinition(child);
                case "class_definition" -> definition = visitClassDefinition(child);
            }
        }

        if (definition instanceof LangMethodDeclaration method) {
            method.setLangAnnotations(decorators);
            for (LangAnnotation decorator : decorators) {
                String name = decorator.getName().getIdentifier();
                if ("abstractmethod".equals(name)) {
                    method.setAbstract(true);
                } else if ("staticmethod".equals(name)) {
                    method.setStatic(true);
                } else if ("classmethod".equals(name)) {
                    // Mark as static since there's no classmethod in Java
                    method.setStatic(true);
                } else if ("async".equals(name)) {
                    method.setAsync(true);
                }
            }
        } else if (definition instanceof LangTypeDeclaration typeDecl) {
            typeDecl.setLangAnnotations(decorators);
        }

        return definition;
    }

    private LangAnnotation visitDecorator(Tree node) {
        Tree nameNode = null;
        Tree argsNode = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "attribute".equals(childType) || "call".equals(childType)) {
                if ("call".equals(childType)) {
                    nameNode = findChildByType(child, "identifier");
                    if (nameNode == null) {
                        nameNode = findChildByType(child, "attribute");
                    }
                    argsNode = findChildByType(child, "argument_list");
                } else {
                    nameNode = child;
                }
            }
        }

        if (nameNode == null) return null;

        String decoratorName = getNodeText(nameNode);
        LangSimpleName name = LangASTNodeFactory.createSimpleName(decoratorName, extractPosition(nameNode));

        LangAnnotation annotation = new LangAnnotation(name, extractPosition(node));

        // Process arguments if present
        if (argsNode != null) {
            List<LangASTNode> args = new ArrayList<>();
            for (Tree argChild : getChildren(argsNode)) {
                LangASTNode argNode = visitNode(argChild);
                if (argNode != null) {
                    args.add(argNode);
                }
            }
            annotation.setArguments(args);
        }

        return annotation;
    }

    // ===== STATEMENTS =====

    private LangBlock visitBlock(Tree node) {
        List<LangASTNode> statements = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            LangASTNode stmt = visitNode(child);
            if (stmt != null) {
                statements.add(stmt);
            }
        }

        return LangASTNodeFactory.createBlock(extractPosition(node), statements);
    }

    private LangIfStatement visitIfStatement(Tree node) {
        LangASTNode condition = null;
        LangBlock body = null;
        LangASTNode elseBody = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "block" -> {
                    if (body == null) {
                        body = visitBlock(child);
                    } else {
                        // else block
                        elseBody = visitBlock(child);
                    }
                }
                case "elif_clause" -> {
                    // elif is treated as nested if in else
                    elseBody = visitElifClause(child);
                }
                case "else_clause" -> {
                    Tree elseBlock = findChildByType(child, "block");
                    if (elseBlock != null) {
                        elseBody = visitBlock(elseBlock);
                    }
                }
                default -> {
                    if (condition == null && !List.of("if", ":", "elif", "else").contains(childType)) {
                        condition = visitNode(child);
                    }
                }
            }
        }

        return LangASTNodeFactory.createIfStatement(condition, body, elseBody, extractPosition(node));
    }

    private LangIfStatement visitElifClause(Tree node) {
        LangASTNode condition = null;
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("block".equals(childType)) {
                body = visitBlock(child);
            } else if (condition == null && !List.of("elif", ":").contains(childType)) {
                condition = visitNode(child);
            }
        }

        return LangASTNodeFactory.createIfStatement(condition, body, null, extractPosition(node));
    }

    private LangForStatement visitForStatement(Tree node) {
        List<LangSingleVariableDeclaration> initializers = new ArrayList<>();
        LangASTNode iterable = null;
        LangBlock body = null;
        LangBlock elseBody = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> {
                    LangSingleVariableDeclaration var = LangASTNodeFactory.createSingleVariableDeclaration(
                            getNodeText(child), null, extractPosition(child));
                    initializers.add(var);
                }
                case "pattern_list", "tuple_pattern" -> {
                    for (Tree patternChild : getChildren(child)) {
                        if ("identifier".equals(getNodeType(patternChild))) {
                            LangSingleVariableDeclaration var = LangASTNodeFactory.createSingleVariableDeclaration(
                                    getNodeText(patternChild), null, extractPosition(patternChild));
                            initializers.add(var);
                        }
                    }
                }
                case "block" -> {
                    if (body == null) {
                        body = visitBlock(child);
                    }
                }
                case "else_clause" -> {
                    Tree elseBlock = findChildByType(child, "block");
                    if (elseBlock != null) {
                        elseBody = visitBlock(elseBlock);
                    }
                }
                default -> {
                    if (iterable == null && !List.of("for", "in", ":").contains(childType)) {
                        iterable = visitNode(child);
                    }
                }
            }
        }

        return LangASTNodeFactory.createForStatement(initializers, iterable, new ArrayList<>(), body, elseBody, extractPosition(node));
    }

    private LangWhileStatement visitWhileStatement(Tree node) {
        LangASTNode condition = null;
        LangBlock body = null;
        LangBlock elseBody = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "block" -> {
                    if (body == null) {
                        body = visitBlock(child);
                    }
                }
                case "else_clause" -> {
                    Tree elseBlock = findChildByType(child, "block");
                    if (elseBlock != null) {
                        elseBody = visitBlock(elseBlock);
                    }
                }
                default -> {
                    if (condition == null && !List.of("while", ":").contains(childType)) {
                        condition = visitNode(child);
                    }
                }
            }
        }

        return LangASTNodeFactory.createWhileStatement(condition, body, elseBody, extractPosition(node));
    }

    private LangReturnStatement visitReturnStatement(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            if (!"return".equals(getNodeType(child))) {
                expression = visitNode(child);
                break;
            }
        }
        return LangASTNodeFactory.createReturnStatement(expression, extractPosition(node));
    }

    private LangExpressionStatement visitExpressionStatement(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            expression = visitNode(child);
            if (expression != null) break;
        }
        return LangASTNodeFactory.createExpressionStatement(expression, extractPosition(node));
    }

    private LangPassStatement visitPassStatement(Tree node) {
        return new LangPassStatement(extractPosition(node));
    }

    private LangBreakStatement visitBreakStatement(Tree node) {
        return new LangBreakStatement(extractPosition(node));
    }

    private LangContinueStatement visitContinueStatement(Tree node) {
        return new LangContinueStatement(extractPosition(node));
    }

    private LangASTNode visitRaiseStatement(Tree node) {
        LangASTNode exception = null;
        LangASTNode from = null;

        List<Tree> children = getChildren(node);
        for (int i = 0; i < children.size(); i++) {
            Tree child = children.get(i);
            String childType = getNodeType(child);
            if ("from".equals(childType)) {
                if (i + 1 < children.size()) {
                    from = visitNode(children.get(i + 1));
                }
            } else if (!"raise".equals(childType) && exception == null) {
                exception = visitNode(child);
            }
        }

        return new LangThrowStatement(extractPosition(node), exception, from);
    }

    private LangAssertStatement visitAssertStatement(Tree node) {
        LangASTNode expression = null;
        LangASTNode message = null;

        List<Tree> children = getChildren(node);
        for (int i = 0; i < children.size(); i++) {
            Tree child = children.get(i);
            String childType = getNodeType(child);
            if (!"assert".equals(childType) && !",".equals(childType)) {
                if (expression == null) {
                    expression = visitNode(child);
                } else {
                    message = visitNode(child);
                }
            }
        }

        return new LangAssertStatement(extractPosition(node), expression, message);
    }

    private LangGlobalStatement visitGlobalStatement(Tree node) {
        List<LangSimpleName> names = new ArrayList<>();
        for (Tree child : getChildren(node)) {
            if ("identifier".equals(getNodeType(child))) {
                names.add(LangASTNodeFactory.createSimpleName(getNodeText(child), extractPosition(child)));
            }
        }
        return new LangGlobalStatement(extractPosition(node), names);
    }

    private LangNonLocalStatement visitNonlocalStatement(Tree node) {
        List<LangSimpleName> names = new ArrayList<>();
        for (Tree child : getChildren(node)) {
            if ("identifier".equals(getNodeType(child))) {
                names.add(LangASTNodeFactory.createSimpleName(getNodeText(child), extractPosition(child)));
            }
        }
        return new LangNonLocalStatement(extractPosition(node), names);
    }

    private LangTryStatement visitTryStatement(Tree node) {
        LangBlock tryBlock = null;
        List<LangCatchClause> catchClauses = new ArrayList<>();
        LangBlock elseBlock = null;
        LangBlock finallyBlock = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "block" -> {
                    if (tryBlock == null) tryBlock = visitBlock(child);
                }
                case "except_clause" -> catchClauses.add(visitExceptClause(child));
                case "else_clause" -> {
                    Tree block = findChildByType(child, "block");
                    if (block != null) elseBlock = visitBlock(block);
                }
                case "finally_clause" -> {
                    Tree block = findChildByType(child, "block");
                    if (block != null) finallyBlock = visitBlock(block);
                }
            }
        }

        return new LangTryStatement(extractPosition(node), tryBlock, catchClauses, elseBlock, finallyBlock);
    }

    private LangCatchClause visitExceptClause(Tree node) {
        List<LangASTNode> exceptionTypes = new ArrayList<>();
        LangSimpleName exceptionVar = null;
        LangASTNode body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "block" -> body = visitBlock(child);
                case "identifier" -> {
                    if (exceptionTypes.isEmpty()) {
                        exceptionTypes.add(LangASTNodeFactory.createSimpleName(getNodeText(child), extractPosition(child)));
                    } else {
                        exceptionVar = LangASTNodeFactory.createSimpleName(getNodeText(child), extractPosition(child));
                    }
                }
                case "as_pattern" -> {
                    Tree typeNode = getChild(child, 0);
                    Tree varNode = getChild(child, child.getChildren().size() - 1);
                    if (typeNode != null) {
                        exceptionTypes.add(visitNode(typeNode));
                    }
                    if (varNode != null && "identifier".equals(getNodeType(varNode))) {
                        exceptionVar = LangASTNodeFactory.createSimpleName(getNodeText(varNode), extractPosition(varNode));
                    }
                }
            }
        }

        LangCatchClause catchClause = new LangCatchClause(extractPosition(node));
        exceptionTypes.forEach(catchClause::addExceptionType);
        catchClause.setExceptionVariable(exceptionVar);
        catchClause.setBody(body);
        return catchClause;
    }

    private LangWithStatement visitWithStatement(Tree node) {
        List<LangASTNode> contextItems = new ArrayList<>();
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "with_clause" -> {
                    for (Tree item : getChildren(child)) {
                        if ("with_item".equals(getNodeType(item))) {
                            LangASTNode expr = null;
                            LangASTNode alias = null;
                            for (Tree itemChild : getChildren(item)) {
                                String itemChildType = getNodeType(itemChild);
                                if ("as_pattern".equals(itemChildType)) {
                                    Tree asChild0 = getChild(itemChild, 0);
                                    if (asChild0 != null) expr = visitNode(asChild0);
                                    Tree asChildLast = getChild(itemChild, itemChild.getChildren().size() - 1);
                                    if (asChildLast != null && "identifier".equals(getNodeType(asChildLast))) {
                                        alias = LangASTNodeFactory.createSimpleName(getNodeText(asChildLast), extractPosition(asChildLast));
                                    }
                                } else if (expr == null && !"as".equals(itemChildType)) {
                                    expr = visitNode(itemChild);
                                }
                            }
                            if (expr != null) {
                                contextItems.add(new LangWithContextItem(extractPosition(item), expr, alias));
                            }
                        }
                    }
                }
                case "block" -> body = visitBlock(child);
                case "with_item" -> {
                    // Handle older grammar without with_clause
                    LangASTNode expr = null;
                    for (Tree itemChild : getChildren(child)) {
                        if (expr == null && !"as".equals(getNodeType(itemChild))) {
                            expr = visitNode(itemChild);
                        }
                    }
                    if (expr != null) {
                        contextItems.add(new LangWithContextItem(extractPosition(child), expr, null));
                    }
                }
            }
        }

        return new LangWithStatement(extractPosition(node), contextItems, body);
    }

    private LangSwitchStatement visitMatchStatement(Tree node) {
        LangASTNode subject = null;
        List<LangCaseStatement> cases = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "case_clause" -> cases.add(visitCaseClause(child));
                default -> {
                    if (subject == null && !List.of("match", ":").contains(childType)) {
                        subject = visitNode(child);
                    }
                }
            }
        }

        return new LangSwitchStatement(extractPosition(node), subject, cases);
    }

    private LangCaseStatement visitCaseClause(Tree node) {
        LangASTNode pattern = null;
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("block".equals(childType)) {
                body = visitBlock(child);
            } else if (pattern == null && !List.of("case", ":").contains(childType)) {
                pattern = visitNode(child);
            }
        }

        return new LangCaseStatement(extractPosition(node), pattern, body);
    }

    private LangImportStatement visitImportStatement(Tree node) {
        List<LangImportStatement.LangImportItem> items = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("dotted_name".equals(childType)) {
                items.add(new LangImportStatement.LangImportItem(getNodeText(child), null, extractPosition(child)));
            } else if ("aliased_import".equals(childType)) {
                String modulePath = findChildText(child, "dotted_name");
                String alias = findChildText(child, "identifier");
                items.add(new LangImportStatement.LangImportItem(modulePath, alias, extractPosition(child)));
            }
        }

        return new LangImportStatement(extractPosition(node), items);
    }

    private LangImportStatement visitImportFromStatement(Tree node) {
        String fromModule = null;
        int relativeLevel = 0;
        List<LangImportStatement.LangImportItem> items = new ArrayList<>();
        boolean isWildcard = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "dotted_name", "relative_import" -> {
                    String text = getNodeText(child);
                    // Count leading dots for relative imports
                    int dots = 0;
                    while (dots < text.length() && text.charAt(dots) == '.') {
                        dots++;
                    }
                    relativeLevel = dots;
                    fromModule = dots > 0 ? text.substring(dots) : text;
                    if (fromModule.isEmpty()) fromModule = null;
                }
                case "import_prefix" -> {
                    relativeLevel = getNodeText(child).length();
                }
                case "wildcard_import" -> isWildcard = true;
                case "identifier" -> {
                    if (fromModule == null) {
                        fromModule = getNodeText(child);
                    } else {
                        items.add(new LangImportStatement.LangImportItem(getNodeText(child), null, extractPosition(child)));
                    }
                }
                case "aliased_import" -> {
                    String name = findChildText(child, "identifier");
                    String alias = null;
                    List<Tree> kids = getChildren(child);
                    if (kids.size() > 1) {
                        alias = getNodeText(kids.get(kids.size() - 1));
                    }
                    items.add(new LangImportStatement.LangImportItem(name, alias, extractPosition(child)));
                }
            }
        }

        return new LangImportStatement(fromModule, relativeLevel, extractPosition(node), items, isWildcard);
    }

    // ===== EXPRESSIONS =====

    private LangAssignment visitAssignment(Tree node) {
        LangASTNode left = null;
        LangASTNode right = null;

        List<Tree> children = getChildren(node);
        for (int i = 0; i < children.size(); i++) {
            Tree child = children.get(i);
            String childType = getNodeType(child);
            if ("=".equals(childType)) {
                // Everything before = is left, after is right
                if (i > 0) left = visitNode(children.get(i - 1));
                if (i + 1 < children.size()) right = visitNode(children.get(i + 1));
                break;
            }
        }

        // Fallback if no = found
        if (left == null && children.size() >= 2) {
            left = visitNode(children.get(0));
            right = visitNode(children.get(children.size() - 1));
        }

        return LangASTNodeFactory.createAssignment("=", left, right, extractPosition(node));
    }

    private LangAssignment visitAugmentedAssignment(Tree node) {
        LangASTNode left = null;
        LangASTNode right = null;
        String operator = "=";

        List<Tree> children = getChildren(node);
        if (children.size() >= 3) {
            left = visitNode(children.get(0));
            operator = getNodeText(children.get(1));
            right = visitNode(children.get(2));
        }

        return LangASTNodeFactory.createAssignment(operator, left, right, extractPosition(node));
    }

    private LangMethodInvocation visitCall(Tree node) {
        LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));

        Tree functionNode = null;
        Tree argsNode = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("argument_list".equals(childType)) {
                argsNode = child;
            } else if (functionNode == null) {
                functionNode = child;
            }
        }

        if (functionNode != null) {
            String funcType = getNodeType(functionNode);
            if ("identifier".equals(funcType)) {
                invocation.setName(getNodeText(functionNode));
            } else if ("attribute".equals(funcType)) {
                // Method call on object: obj.method()
                Tree objNode = getChild(functionNode, 0);
                Tree attrNode = findChildByType(functionNode, "identifier");
                if (objNode != null) {
                    invocation.setExpression(visitNode(objNode));
                }
                if (attrNode != null) {
                    invocation.setName(getNodeText(attrNode));
                }
            } else {
                // Complex expression as callee
                invocation.setExpression(visitNode(functionNode));
            }
        }

        if (argsNode != null) {
            for (Tree argChild : getChildren(argsNode)) {
                String argType = getNodeType(argChild);
                if (!",".equals(argType) && !"(".equals(argType) && !")".equals(argType)) {
                    LangASTNode arg = visitNode(argChild);
                    if (arg != null) {
                        invocation.addArgument(arg);
                    }
                }
            }
        }

        return invocation;
    }

    private LangFieldAccess visitAttribute(Tree node) {
        LangASTNode expression = null;
        String fieldName = "";

        List<Tree> children = getChildren(node);
        if (children.size() >= 2) {
            expression = visitNode(children.get(0));
            // Last identifier is the attribute name
            for (int i = children.size() - 1; i >= 0; i--) {
                if ("identifier".equals(getNodeType(children.get(i)))) {
                    fieldName = getNodeText(children.get(i));
                    break;
                }
            }
        }

        return LangASTNodeFactory.createFieldAccess(expression, fieldName, extractPosition(node));
    }

    private LangIndexAccess visitSubscript(Tree node) {
        LangASTNode target = null;
        LangASTNode index = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (target == null && !"[".equals(childType) && !"]".equals(childType)) {
                target = visitNode(child);
            } else if (target != null && !"[".equals(childType) && !"]".equals(childType)) {
                index = visitNode(child);
            }
        }

        return new LangIndexAccess(extractPosition(node), target, index);
    }

    private LangLambdaExpression visitLambda(Tree node) {
        List<LangASTNode> params = new ArrayList<>();
        LangASTNode body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("lambda_parameters".equals(childType)) {
                for (Tree paramChild : getChildren(child)) {
                    if ("identifier".equals(getNodeType(paramChild))) {
                        params.add(LangASTNodeFactory.createSimpleName(getNodeText(paramChild), extractPosition(paramChild)));
                    }
                }
            } else if (!"lambda".equals(childType) && !":".equals(childType)) {
                body = visitNode(child);
            }
        }

        return new LangLambdaExpression(extractPosition(node), params, body);
    }

    private LangTernaryExpression visitConditionalExpression(Tree node) {
        LangASTNode thenExpr = null;
        LangASTNode condition = null;
        LangASTNode elseExpr = null;

        // Python: value_if_true if condition else value_if_false
        List<Tree> children = getChildren(node);
        if (children.size() >= 5) {
            thenExpr = visitNode(children.get(0));
            condition = visitNode(children.get(2));
            elseExpr = visitNode(children.get(4));
        }

        return new LangTernaryExpression(extractPosition(node), condition, thenExpr, elseExpr);
    }

    private LangInfixExpression visitBooleanOperator(Tree node) {
        LangASTNode left = null;
        String operator = "";
        LangASTNode right = null;

        List<Tree> children = getChildren(node);
        if (children.size() >= 3) {
            left = visitNode(children.get(0));
            operator = getNodeText(children.get(1));
            right = visitNode(children.get(2));
        }

        return LangASTNodeFactory.createInfixExpression(left, right, operator, extractPosition(node));
    }

    private LangInfixExpression visitComparisonOperator(Tree node) {
        LangASTNode left = null;
        String operator = "";
        LangASTNode right = null;

        List<Tree> children = getChildren(node);
        if (children.size() >= 3) {
            left = visitNode(children.get(0));
            operator = getNodeText(children.get(1));
            right = visitNode(children.get(2));
        }

        return LangASTNodeFactory.createInfixExpression(left, right, operator, extractPosition(node));
    }

    private LangInfixExpression visitBinaryOperator(Tree node) {
        LangASTNode left = null;
        String operator = "";
        LangASTNode right = null;

        List<Tree> children = getChildren(node);
        if (children.size() >= 3) {
            left = visitNode(children.get(0));
            operator = getNodeText(children.get(1));
            right = visitNode(children.get(2));
        }

        return LangASTNodeFactory.createInfixExpression(left, right, operator, extractPosition(node));
    }

    private LangPrefixExpression visitUnaryOperator(Tree node) {
        String operator = "";
        LangASTNode operand = null;

        List<Tree> children = getChildren(node);
        if (children.size() >= 2) {
            operator = getNodeText(children.get(0));
            operand = visitNode(children.get(1));
        }

        LangPrefixExpression expr = new LangPrefixExpression(extractPosition(node));
        expr.setOperator(OperatorEnum.fromSymbol(operator));
        expr.setOperand(operand);
        return expr;
    }

    private LangPrefixExpression visitNotOperator(Tree node) {
        LangASTNode operand = null;

        for (Tree child : getChildren(node)) {
            if (!"not".equals(getNodeType(child))) {
                operand = visitNode(child);
                break;
            }
        }

        LangPrefixExpression expr = new LangPrefixExpression(extractPosition(node));
        expr.setOperator(OperatorEnum.NOT);
        expr.setOperand(operand);
        return expr;
    }

    private LangParenthesizedExpression visitParenthesizedExpression(Tree node) {
        LangASTNode inner = null;
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"(".equals(childType) && !")".equals(childType)) {
                inner = visitNode(child);
                break;
            }
        }
        return new LangParenthesizedExpression(extractPosition(node), inner);
    }

    private LangAwaitExpression visitAwait(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            if (!"await".equals(getNodeType(child))) {
                expression = visitNode(child);
                break;
            }
        }
        LangAwaitExpression await = new LangAwaitExpression(extractPosition(node));
        await.setExpression(expression);
        return await;
    }

    // ===== LITERALS =====

    private LangSimpleName visitIdentifier(Tree node) {
        return LangASTNodeFactory.createSimpleName(getNodeText(node), extractPosition(node));
    }

    private LangNumberLiteral visitInteger(Tree node) {
        return LangASTNodeFactory.createNumberLiteral(extractPosition(node), getNodeText(node));
    }

    private LangNumberLiteral visitFloat(Tree node) {
        return LangASTNodeFactory.createNumberLiteral(extractPosition(node), getNodeText(node));
    }

    private LangStringLiteral visitString(Tree node) {
        String text = getNodeText(node);
        // Remove quotes
        if ((text.startsWith("\"\"\"") && text.endsWith("\"\"\"")) ||
                (text.startsWith("'''") && text.endsWith("'''"))) {
            text = text.substring(3, text.length() - 3);
        } else if ((text.startsWith("\"") && text.endsWith("\"")) ||
                (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1);
        }
        // Handle f-string, r-string prefixes
        if (text.length() > 0 && (text.charAt(0) == 'f' || text.charAt(0) == 'r' ||
                text.charAt(0) == 'b' || text.charAt(0) == 'F' || text.charAt(0) == 'R' || text.charAt(0) == 'B')) {
            // For now, keep the string as-is after quote removal
        }
        return LangASTNodeFactory.createStringLiteral(extractPosition(node), text);
    }

    private LangStringLiteral visitConcatenatedString(Tree node) {
        StringBuilder sb = new StringBuilder();
        for (Tree child : getChildren(node)) {
            if ("string".equals(getNodeType(child))) {
                LangStringLiteral lit = visitString(child);
                sb.append(lit.getValue());
            }
        }
        return LangASTNodeFactory.createStringLiteral(extractPosition(node), sb.toString());
    }

    private LangBooleanLiteral visitTrue(Tree node) {
        return LangASTNodeFactory.createBooleanLiteral(extractPosition(node), true);
    }

    private LangBooleanLiteral visitFalse(Tree node) {
        return LangASTNodeFactory.createBooleanLiteral(extractPosition(node), false);
    }

    private LangNullLiteral visitNone(Tree node) {
        return LangASTNodeFactory.createNullLiteral(extractPosition(node));
    }

    private LangEllipsisLiteral visitEllipsis(Tree node) {
        return LangASTNodeFactory.createEllipsisLiteral(extractPosition(node));
    }

    // ===== COLLECTIONS =====

    private LangListLiteral visitList(Tree node) {
        List<LangASTNode> elements = new ArrayList<>();
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"[".equals(childType) && !"]".equals(childType) && !",".equals(childType)) {
                LangASTNode elem = visitNode(child);
                if (elem != null) elements.add(elem);
            }
        }
        return LangASTNodeFactory.createListLiteral(extractPosition(node), elements);
    }

    private LangTupleLiteral visitTuple(Tree node) {
        List<LangASTNode> elements = new ArrayList<>();
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"(".equals(childType) && !")".equals(childType) && !",".equals(childType)) {
                LangASTNode elem = visitNode(child);
                if (elem != null) elements.add(elem);
            }
        }
        return LangASTNodeFactory.createTupleLiteral(extractPosition(node), elements);
    }

    private LangDictionaryLiteral visitDictionary(Tree node) {
        LangDictionaryLiteral dict = LangASTNodeFactory.createDictionaryLiteral(extractPosition(node));
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("pair".equals(childType)) {
                List<Tree> pairChildren = getChildren(child);
                if (pairChildren.size() >= 2) {
                    LangASTNode key = visitNode(pairChildren.get(0));
                    LangASTNode value = visitNode(pairChildren.get(pairChildren.size() - 1));
                    dict.addEntry(key, value);
                }
            }
        }
        return dict;
    }

    private LangListLiteral visitSet(Tree node) {
        // Treat set as list for now since there's no dedicated LangSetLiteral
        List<LangASTNode> elements = new ArrayList<>();
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"{".equals(childType) && !"}".equals(childType) && !",".equals(childType)) {
                LangASTNode elem = visitNode(child);
                if (elem != null) elements.add(elem);
            }
        }
        return LangASTNodeFactory.createListLiteral(extractPosition(node), elements);
    }

    private LangASTNode visitListComprehension(Tree node) {
        // For now, return simplified representation
        return LangASTNodeFactory.createListLiteral(extractPosition(node), new ArrayList<>());
    }

    private LangASTNode visitDictComprehension(Tree node) {
        return LangASTNodeFactory.createDictionaryLiteral(extractPosition(node));
    }

    private LangASTNode visitSetComprehension(Tree node) {
        return LangASTNodeFactory.createListLiteral(extractPosition(node), new ArrayList<>());
    }

    private LangASTNode visitGeneratorExpression(Tree node) {
        return LangASTNodeFactory.createListLiteral(extractPosition(node), new ArrayList<>());
    }

    // ===== HELPERS =====

    private LangComment visitComment(Tree node) {
        String text = getNodeText(node);
        if (text.startsWith("#")) {
            text = text.substring(1).trim();
        }
        return new LangComment(text, false, false, extractPosition(node));
    }

    private String findChildText(Tree node, String childType) {
        Tree child = findChildByType(node, childType);
        return child != null ? getNodeText(child) : "";
    }

    private Visibility getMethodVisibility(String methodName) {
        if (methodName.startsWith("__") && !methodName.endsWith("__")) {
            return Visibility.PRIVATE;
        } else if (methodName.startsWith("_") && !methodName.startsWith("__")) {
            return Visibility.PROTECTED;
        }
        return Visibility.PUBLIC;
    }

    private String extractCleanName(String name) {
        if (name.startsWith("__") && name.endsWith("__")) {
            return name.substring(2, name.length() - 2);
        }
        if (name.startsWith("__")) {
            return name.substring(2);
        }
        if (name.startsWith("_")) {
            return name.substring(1);
        }
        return name;
    }

    private LangASTNode handleUnknownNode(Tree node) {
        // For unknown nodes, try to visit children and return first non-null result
        for (Tree child : getChildren(node)) {
            LangASTNode result = visitNode(child);
            if (result != null) {
                return result;
            }
        }
        // Return a simple name for leaf nodes
        if (isLeaf(node)) {
            String label = getNodeLabel(node);
            if (!label.isEmpty()) {
                return LangASTNodeFactory.createSimpleName(label, extractPosition(node));
            }
        }
        return null;
    }
}
