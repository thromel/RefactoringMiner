package extension.treesitter.typescript;

import com.github.gumtreediff.tree.Tree;
import extension.ast.node.*;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangSingleVariableDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.expression.*;
import extension.ast.node.literal.*;
import extension.ast.node.metadata.LangAnnotation;
import extension.ast.node.statement.*;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import extension.treesitter.AbstractTreeSitterASTBuilder;
import gr.uom.java.xmi.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-sitter AST builder for TypeScript.
 * Converts Tree-sitter TypeScript AST into LangASTNode hierarchy.
 */
public class TypeScriptTreeSitterASTBuilder extends AbstractTreeSitterASTBuilder {

    @Override
    public String getLanguage() {
        return "typescript";
    }

    @Override
    protected LangASTNode buildFromRoot(Tree root) {
        return visitProgram(root);
    }

    private LangCompilationUnit visitProgram(Tree node) {
        PositionInfo positionInfo = extractPosition(node);
        LangCompilationUnit compilationUnit = LangASTNodeFactory.createCompilationUnit(positionInfo);
        compilationUnit.setLanguage(LangSupportedEnum.TYPESCRIPT);

        for (Tree child : getChildren(node)) {
            processTopLevelDeclaration(child, compilationUnit);
        }

        return compilationUnit;
    }

    private void processTopLevelDeclaration(Tree node, LangCompilationUnit compilationUnit) {
        String nodeType = getNodeType(node);

        switch (nodeType) {
            case "class_declaration" -> {
                LangTypeDeclaration typeDecl = visitClassDeclaration(node);
                if (typeDecl != null) {
                    compilationUnit.addType(typeDecl);
                }
            }
            case "interface_declaration" -> {
                LangTypeDeclaration typeDecl = visitInterfaceDeclaration(node);
                if (typeDecl != null) {
                    compilationUnit.addType(typeDecl);
                }
            }
            case "function_declaration" -> {
                LangMethodDeclaration methodDecl = visitFunctionDeclaration(node);
                if (methodDecl != null) {
                    compilationUnit.addMethod(methodDecl);
                }
            }
            case "import_statement" -> {
                LangImportStatement importStmt = visitImportStatement(node);
                if (importStmt != null) {
                    compilationUnit.addImport(importStmt);
                }
            }
            case "export_statement" -> {
                // Process exported declaration
                for (Tree child : getChildren(node)) {
                    processTopLevelDeclaration(child, compilationUnit);
                }
            }
            case "lexical_declaration", "variable_declaration" -> {
                LangASTNode stmt = visitVariableDeclarationStatement(node);
                if (stmt != null) {
                    compilationUnit.addStatement(stmt);
                }
            }
            default -> {
                LangASTNode astNode = visitNode(node);
                if (astNode instanceof LangTypeDeclaration typeDecl) {
                    compilationUnit.addType(typeDecl);
                } else if (astNode instanceof LangMethodDeclaration methodDecl) {
                    compilationUnit.addMethod(methodDecl);
                } else if (astNode != null) {
                    compilationUnit.addStatement(astNode);
                }
            }
        }
    }

    @Override
    protected LangASTNode visitNode(Tree node) {
        String nodeType = getNodeType(node);

        return switch (nodeType) {
            // Type declarations
            case "class_declaration" -> visitClassDeclaration(node);
            case "interface_declaration" -> visitInterfaceDeclaration(node);

            // Member declarations
            case "method_definition" -> visitMethodDefinition(node);
            case "function_declaration" -> visitFunctionDeclaration(node);
            case "public_field_definition", "property_signature" -> visitFieldDefinition(node);

            // Statements
            case "statement_block" -> visitStatementBlock(node);
            case "if_statement" -> visitIfStatement(node);
            case "for_statement" -> visitForStatement(node);
            case "for_in_statement" -> visitForInStatement(node);
            case "while_statement" -> visitWhileStatement(node);
            case "do_statement" -> visitDoStatement(node);
            case "switch_statement" -> visitSwitchStatement(node);
            case "try_statement" -> visitTryStatement(node);
            case "return_statement" -> visitReturnStatement(node);
            case "break_statement" -> visitBreakStatement(node);
            case "continue_statement" -> visitContinueStatement(node);
            case "throw_statement" -> visitThrowStatement(node);
            case "expression_statement" -> visitExpressionStatement(node);
            case "lexical_declaration", "variable_declaration" -> visitVariableDeclarationStatement(node);

            // Expressions
            case "assignment_expression" -> visitAssignmentExpression(node);
            case "call_expression" -> visitCallExpression(node);
            case "member_expression" -> visitMemberExpression(node);
            case "subscript_expression" -> visitSubscriptExpression(node);
            case "new_expression" -> visitNewExpression(node);
            case "binary_expression" -> visitBinaryExpression(node);
            case "unary_expression" -> visitUnaryExpression(node);
            case "update_expression" -> visitUpdateExpression(node);
            case "ternary_expression" -> visitTernaryExpression(node);
            case "arrow_function" -> visitArrowFunction(node);
            case "parenthesized_expression" -> visitParenthesizedExpression(node);
            case "await_expression" -> visitAwaitExpression(node);
            case "template_string" -> visitTemplateString(node);

            // Literals
            case "identifier", "property_identifier", "shorthand_property_identifier" -> visitIdentifier(node);
            case "number" -> visitNumberLiteral(node);
            case "string" -> visitStringLiteral(node);
            case "true" -> LangASTNodeFactory.createBooleanLiteral(extractPosition(node), true);
            case "false" -> LangASTNodeFactory.createBooleanLiteral(extractPosition(node), false);
            case "null" -> LangASTNodeFactory.createNullLiteral(extractPosition(node));
            case "undefined" -> LangASTNodeFactory.createNullLiteral(extractPosition(node));
            case "this" -> LangASTNodeFactory.createSimpleName("this", extractPosition(node));

            // Arrays and objects
            case "array" -> visitArrayLiteral(node);
            case "object" -> visitObjectLiteral(node);

            default -> visitDefaultNode(node);
        };
    }

    // ===== TYPE DECLARATIONS =====

    private LangTypeDeclaration visitClassDeclaration(Tree node) {
        String className = "";
        String superClassName = "";
        List<String> interfaces = new ArrayList<>();
        List<LangMethodDeclaration> methods = new ArrayList<>();
        List<LangAssignment> fields = new ArrayList<>();
        List<LangAnnotation> decorators = new ArrayList<>();
        Visibility visibility = Visibility.PUBLIC; // Default to public for TypeScript
        boolean isAbstract = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "type_identifier", "identifier" -> className = getNodeText(child);
                case "class_heritage" -> {
                    for (Tree heritageChild : getChildren(child)) {
                        String heritageType = getNodeType(heritageChild);
                        if ("extends_clause".equals(heritageType)) {
                            Tree extendsType = findChildByType(heritageChild, "identifier");
                            if (extendsType != null) {
                                superClassName = getNodeText(extendsType);
                            }
                        } else if ("implements_clause".equals(heritageType)) {
                            for (Tree impl : getChildren(heritageChild)) {
                                if ("type_identifier".equals(getNodeType(impl)) || "identifier".equals(getNodeType(impl))) {
                                    interfaces.add(getNodeText(impl));
                                }
                            }
                        }
                    }
                }
                case "class_body" -> {
                    for (Tree member : getChildren(node)) {
                        String memberType = getNodeType(member);
                        if ("method_definition".equals(memberType)) {
                            LangMethodDeclaration method = visitMethodDefinition(member);
                            if (method != null) {
                                methods.add(method);
                            }
                        } else if ("public_field_definition".equals(memberType)) {
                            LangAssignment field = visitFieldAsAssignment(member);
                            if (field != null) {
                                fields.add(field);
                            }
                        }
                    }
                }
                case "decorator" -> {
                    LangAnnotation decorator = visitDecorator(child);
                    if (decorator != null) {
                        decorators.add(decorator);
                    }
                }
                case "abstract" -> isAbstract = true;
            }
        }

        // Also check class_body directly if not found in children
        Tree classBody = findChildByType(node, "class_body");
        if (classBody != null) {
            for (Tree member : getChildren(classBody)) {
                String memberType = getNodeType(member);
                if ("method_definition".equals(memberType)) {
                    LangMethodDeclaration method = visitMethodDefinition(member);
                    if (method != null && !methods.contains(method)) {
                        methods.add(method);
                    }
                } else if ("public_field_definition".equals(memberType)) {
                    LangAssignment field = visitFieldAsAssignment(member);
                    if (field != null) {
                        fields.add(field);
                    }
                }
            }
        }

        if (!className.isEmpty()) {
            LangTypeDeclaration typeDecl = LangASTNodeFactory.createTypeDeclaration(
                    className, extractPosition(node));

            // Set visibility and modifiers
            typeDecl.setVisibility(visibility);
            typeDecl.setAbstract(isAbstract);
            typeDecl.setTopLevel(true);

            // Set superclass and interfaces
            List<LangSimpleName> superClasses = new ArrayList<>();
            if (!superClassName.isEmpty()) {
                superClasses.add(LangASTNodeFactory.createSimpleName(superClassName, extractPosition(node)));
            }
            for (String iface : interfaces) {
                superClasses.add(LangASTNodeFactory.createSimpleName(iface, extractPosition(node)));
            }
            if (!superClasses.isEmpty()) {
                typeDecl.setSuperClassNames(superClasses);
            }

            for (LangMethodDeclaration method : methods) {
                typeDecl.addMethod(method);
            }
            for (LangAssignment field : fields) {
                typeDecl.addAssignment(field);
            }
            if (!decorators.isEmpty()) {
                typeDecl.setLangAnnotations(decorators);
            }
            return typeDecl;
        }

        return null;
    }

    private LangTypeDeclaration visitInterfaceDeclaration(Tree node) {
        String interfaceName = "";
        List<LangMethodDeclaration> methods = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("type_identifier".equals(childType) || "identifier".equals(childType)) {
                interfaceName = getNodeText(child);
            } else if ("interface_body".equals(childType) || "object_type".equals(childType)) {
                for (Tree member : getChildren(child)) {
                    if ("method_signature".equals(getNodeType(member)) || "property_signature".equals(getNodeType(member))) {
                        LangMethodDeclaration method = visitMethodSignature(member);
                        if (method != null) {
                            methods.add(method);
                        }
                    }
                }
            }
        }

        if (!interfaceName.isEmpty()) {
            LangTypeDeclaration typeDecl = LangASTNodeFactory.createTypeDeclaration(
                    interfaceName, extractPosition(node));
            typeDecl.setInterface(true);
            typeDecl.setVisibility(Visibility.PUBLIC);
            typeDecl.setTopLevel(true);
            for (LangMethodDeclaration method : methods) {
                typeDecl.addMethod(method);
            }
            return typeDecl;
        }

        return null;
    }

    // ===== METHOD DECLARATIONS =====

    private LangMethodDeclaration visitMethodDefinition(Tree node) {
        String methodName = "";
        List<LangSingleVariableDeclaration> params = new ArrayList<>();
        LangBlock body = null;
        Visibility visibility = Visibility.PUBLIC;
        boolean isStatic = false;
        boolean isAsync = false;
        String returnType = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "property_identifier", "identifier" -> methodName = getNodeText(child);
                case "formal_parameters" -> params = extractParameters(child);
                case "statement_block" -> body = visitStatementBlock(child);
                case "type_annotation" -> {
                    // Return type annotation: (): number
                    Tree typeNode = getChild(child, 1);
                    if (typeNode != null) {
                        returnType = getNodeText(typeNode);
                    }
                }
                case "accessibility_modifier" -> {
                    String mod = getNodeText(child).toLowerCase();
                    visibility = switch (mod) {
                        case "private" -> Visibility.PRIVATE;
                        case "protected" -> Visibility.PROTECTED;
                        default -> Visibility.PUBLIC;
                    };
                }
                case "static" -> isStatic = true;
                case "async" -> isAsync = true;
            }
        }

        if (!methodName.isEmpty()) {
            LangMethodDeclaration methodDecl = LangASTNodeFactory.createMethodDeclaration(
                    methodName, extractPosition(node), params, body);
            methodDecl.setVisibility(visibility);
            methodDecl.setStatic(isStatic);
            if (returnType != null) {
                methodDecl.setReturnTypeAnnotation(returnType);
            }
            // Mark constructor
            if ("constructor".equals(methodName)) {
                methodDecl.setConstructor(true);
                methodDecl.setReturnTypeAnnotation("void");
            }
            return methodDecl;
        }

        return null;
    }

    private LangMethodDeclaration visitFunctionDeclaration(Tree node) {
        String funcName = "";
        List<LangSingleVariableDeclaration> params = new ArrayList<>();
        LangBlock body = null;
        boolean isAsync = false;
        String returnType = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> funcName = getNodeText(child);
                case "formal_parameters" -> params = extractParameters(child);
                case "statement_block" -> body = visitStatementBlock(child);
                case "type_annotation" -> {
                    Tree typeNode = getChild(child, 1);
                    if (typeNode != null) {
                        returnType = getNodeText(typeNode);
                    }
                }
                case "async" -> isAsync = true;
            }
        }

        if (!funcName.isEmpty()) {
            LangMethodDeclaration methodDecl = LangASTNodeFactory.createMethodDeclaration(
                    funcName, extractPosition(node), params, body);
            methodDecl.setVisibility(Visibility.PUBLIC);
            if (returnType != null) {
                methodDecl.setReturnTypeAnnotation(returnType);
            }
            return methodDecl;
        }

        return null;
    }

    private LangMethodDeclaration visitMethodSignature(Tree node) {
        String methodName = "";
        List<LangSingleVariableDeclaration> params = new ArrayList<>();
        String returnType = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("property_identifier".equals(childType) || "identifier".equals(childType)) {
                methodName = getNodeText(child);
            } else if ("formal_parameters".equals(childType)) {
                params = extractParameters(child);
            } else if ("type_annotation".equals(childType)) {
                Tree typeNode = getChild(child, 1);
                if (typeNode != null) {
                    returnType = getNodeText(typeNode);
                }
            }
        }

        if (!methodName.isEmpty()) {
            LangMethodDeclaration methodDecl = LangASTNodeFactory.createMethodDeclaration(
                    methodName, extractPosition(node), params, null);
            methodDecl.setVisibility(Visibility.PUBLIC);
            if (returnType != null) {
                methodDecl.setReturnTypeAnnotation(returnType);
            }
            return methodDecl;
        }

        return null;
    }

    private List<LangSingleVariableDeclaration> extractParameters(Tree paramList) {
        List<LangSingleVariableDeclaration> params = new ArrayList<>();

        for (Tree child : getChildren(paramList)) {
            String childType = getNodeType(child);
            if ("required_parameter".equals(childType) || "optional_parameter".equals(childType)) {
                String paramName = "";
                String paramType = "";

                for (Tree paramChild : getChildren(child)) {
                    String pChildType = getNodeType(paramChild);
                    if ("identifier".equals(pChildType)) {
                        paramName = getNodeText(paramChild);
                    } else if ("type_annotation".equals(pChildType)) {
                        Tree typeNode = getChild(paramChild, 1);
                        if (typeNode != null) {
                            paramType = getNodeText(typeNode);
                        }
                    }
                }

                if (!paramName.isEmpty()) {
                    LangSingleVariableDeclaration param = LangASTNodeFactory.createSingleVariableDeclaration(
                            paramName, null, extractPosition(child));
                    param.setParameter(true);
                    if (!paramType.isEmpty()) {
                        TypeObjectEnum typeEnum = TypeObjectEnum.fromType(paramType);
                        param.setTypeAnnotation(typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT);
                        param.setHasTypeAnnotation(true);
                    }
                    params.add(param);
                }
            }
        }

        return params;
    }

    // ===== FIELD DECLARATIONS =====

    private LangAssignment visitFieldAsAssignment(Tree node) {
        String fieldName = "";
        String fieldType = "";
        LangASTNode initializer = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("property_identifier".equals(childType) || "identifier".equals(childType)) {
                fieldName = getNodeText(child);
            } else if ("type_annotation".equals(childType)) {
                Tree typeNode = getChild(child, 1);
                if (typeNode != null) {
                    fieldType = getNodeText(typeNode);
                }
            } else if (!"accessibility_modifier".equals(childType) && !"static".equals(childType) && !"readonly".equals(childType)) {
                initializer = visitNode(child);
            }
        }

        if (!fieldName.isEmpty()) {
            PositionInfo positionInfo = extractPosition(node);
            LangSingleVariableDeclaration varDecl = LangASTNodeFactory.createSingleVariableDeclaration(
                    fieldName, initializer, positionInfo);
            varDecl.setAttribute(true);
            if (!fieldType.isEmpty()) {
                TypeObjectEnum typeEnum = TypeObjectEnum.fromType(fieldType);
                varDecl.setTypeAnnotation(typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT);
                varDecl.setHasTypeAnnotation(true);
            }
            return LangASTNodeFactory.createAssignment("=", varDecl, initializer, positionInfo);
        }

        return null;
    }

    private LangASTNode visitFieldDefinition(Tree node) {
        return visitFieldAsAssignment(node);
    }

    // ===== STATEMENTS =====

    private LangBlock visitStatementBlock(Tree node) {
        List<LangASTNode> statements = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"{".equals(childType) && !"}".equals(childType)) {
                LangASTNode stmt = visitNode(child);
                if (stmt != null) {
                    statements.add(stmt);
                }
            }
        }

        return LangASTNodeFactory.createBlock(extractPosition(node), statements);
    }

    private LangIfStatement visitIfStatement(Tree node) {
        LangASTNode condition = null;
        LangBlock thenBlock = null;
        LangASTNode elseBlock = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parenthesized_expression".equals(childType)) {
                condition = visitNode(getChild(child, 1));
            } else if ("statement_block".equals(childType) && thenBlock == null) {
                thenBlock = visitStatementBlock(child);
            } else if ("else_clause".equals(childType)) {
                Tree elseContent = getChild(child, 1);
                if (elseContent != null) {
                    elseBlock = visitNode(elseContent);
                }
            }
        }

        return LangASTNodeFactory.createIfStatement(condition, thenBlock, elseBlock, extractPosition(node));
    }

    private LangForStatement visitForStatement(Tree node) {
        List<LangSingleVariableDeclaration> initializers = new ArrayList<>();
        LangASTNode condition = null;
        List<LangASTNode> updates = new ArrayList<>();
        LangBlock body = null;

        List<Tree> children = getChildren(node);
        int parenCount = 0;
        int partIndex = 0;

        for (Tree child : children) {
            String childType = getNodeType(child);
            if ("(".equals(childType)) {
                parenCount++;
            } else if (")".equals(childType)) {
                parenCount--;
            } else if ("statement_block".equals(childType)) {
                body = visitStatementBlock(child);
            } else if (";".equals(childType)) {
                partIndex++;
            } else if (parenCount > 0) {
                LangASTNode part = visitNode(child);
                if (part != null) {
                    switch (partIndex) {
                        case 0 -> {
                            // initializer - wrap as variable declaration
                            LangSingleVariableDeclaration varDecl = LangASTNodeFactory.createSingleVariableDeclaration(
                                    "init", null, extractPosition(child));
                            initializers.add(varDecl);
                        }
                        case 1 -> condition = part;
                        case 2 -> updates.add(part);
                    }
                }
            }
        }

        return LangASTNodeFactory.createForStatement(initializers, condition, updates, body, null, extractPosition(node));
    }

    private LangForStatement visitForInStatement(Tree node) {
        // TypeScript for-in/for-of maps to LangForStatement (like C# foreach)
        List<LangSingleVariableDeclaration> iterators = new ArrayList<>();
        LangASTNode iterable = null;
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) && iterators.isEmpty()) {
                String varName = getNodeText(child);
                LangSingleVariableDeclaration iter = LangASTNodeFactory.createSingleVariableDeclaration(
                        varName, null, extractPosition(child));
                iterators.add(iter);
            } else if (!"in".equals(childType) && !"of".equals(childType) && !"(".equals(childType) && !")".equals(childType)) {
                if (iterable == null && !"statement_block".equals(childType)) {
                    iterable = visitNode(child);
                } else if ("statement_block".equals(childType)) {
                    body = visitStatementBlock(child);
                }
            }
        }

        return LangASTNodeFactory.createForStatement(iterators, iterable, new ArrayList<>(), body, null, extractPosition(node));
    }

    private LangWhileStatement visitWhileStatement(Tree node) {
        LangASTNode condition = null;
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parenthesized_expression".equals(childType)) {
                condition = visitNode(getChild(child, 1));
            } else if ("statement_block".equals(childType)) {
                body = visitStatementBlock(child);
            }
        }

        return new LangWhileStatement(condition, body, null, extractPosition(node));
    }

    private LangWhileStatement visitDoStatement(Tree node) {
        // TypeScript do-while maps to LangWhileStatement (no separate do-while class)
        LangASTNode condition = null;
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parenthesized_expression".equals(childType)) {
                condition = visitNode(getChild(child, 1));
            } else if ("statement_block".equals(childType)) {
                body = visitStatementBlock(child);
            }
        }

        return new LangWhileStatement(condition, body, null, extractPosition(node));
    }

    private LangSwitchStatement visitSwitchStatement(Tree node) {
        LangASTNode expression = null;
        List<LangCaseStatement> cases = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parenthesized_expression".equals(childType)) {
                expression = visitNode(getChild(child, 1));
            } else if ("switch_body".equals(childType)) {
                for (Tree caseNode : getChildren(child)) {
                    if ("switch_case".equals(getNodeType(caseNode)) || "switch_default".equals(getNodeType(caseNode))) {
                        LangCaseStatement caseStmt = visitSwitchCase(caseNode);
                        if (caseStmt != null) {
                            cases.add(caseStmt);
                        }
                    }
                }
            }
        }

        return new LangSwitchStatement(extractPosition(node), expression, cases);
    }

    private LangCaseStatement visitSwitchCase(Tree node) {
        LangASTNode caseValue = null;
        List<LangASTNode> statements = new ArrayList<>();
        boolean isDefault = "switch_default".equals(getNodeType(node));

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!isDefault && !"case".equals(childType) && !":".equals(childType) && caseValue == null) {
                caseValue = visitNode(child);
            } else if (!":".equals(childType) && !"case".equals(childType) && !"default".equals(childType)) {
                LangASTNode stmt = visitNode(child);
                if (stmt != null) {
                    statements.add(stmt);
                }
            }
        }

        // Create a block from the statements
        LangBlock body = LangASTNodeFactory.createBlock(extractPosition(node), statements);

        return new LangCaseStatement(extractPosition(node), isDefault ? null : caseValue, body);
    }

    private LangTryStatement visitTryStatement(Tree node) {
        LangBlock tryBody = null;
        List<LangCatchClause> catchClauses = new ArrayList<>();
        LangBlock finallyBody = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("statement_block".equals(childType) && tryBody == null) {
                tryBody = visitStatementBlock(child);
            } else if ("catch_clause".equals(childType)) {
                LangCatchClause catchClause = visitCatchClause(child);
                if (catchClause != null) {
                    catchClauses.add(catchClause);
                }
            } else if ("finally_clause".equals(childType)) {
                Tree finallyBlock = findChildByType(child, "statement_block");
                if (finallyBlock != null) {
                    finallyBody = visitStatementBlock(finallyBlock);
                }
            }
        }

        return new LangTryStatement(extractPosition(node), tryBody, catchClauses, null, finallyBody);
    }

    private LangCatchClause visitCatchClause(Tree node) {
        String exceptionVar = "";
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType)) {
                exceptionVar = getNodeText(child);
            } else if ("statement_block".equals(childType)) {
                body = visitStatementBlock(child);
            }
        }

        LangCatchClause clause = new LangCatchClause(extractPosition(node));
        if (!exceptionVar.isEmpty()) {
            LangSimpleName exceptionName = LangASTNodeFactory.createSimpleName(exceptionVar, extractPosition(node));
            clause.setExceptionVariable(exceptionName);
        }
        clause.setBody(body);
        return clause;
    }

    private LangReturnStatement visitReturnStatement(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            if (!"return".equals(getNodeType(child)) && !";".equals(getNodeType(child))) {
                expression = visitNode(child);
                break;
            }
        }
        return LangASTNodeFactory.createReturnStatement(expression, extractPosition(node));
    }

    private LangBreakStatement visitBreakStatement(Tree node) {
        return new LangBreakStatement(extractPosition(node));
    }

    private LangContinueStatement visitContinueStatement(Tree node) {
        return new LangContinueStatement(extractPosition(node));
    }

    private LangThrowStatement visitThrowStatement(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            if (!"throw".equals(getNodeType(child)) && !";".equals(getNodeType(child))) {
                expression = visitNode(child);
                break;
            }
        }
        return new LangThrowStatement(extractPosition(node), expression, null);
    }

    private LangExpressionStatement visitExpressionStatement(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            if (!";".equals(getNodeType(child))) {
                expression = visitNode(child);
                break;
            }
        }
        if (expression != null) {
            return LangASTNodeFactory.createExpressionStatement(expression, extractPosition(node));
        }
        return null;
    }

    private LangASTNode visitVariableDeclarationStatement(Tree node) {
        String kind = "let"; // const, let, var
        String varName = "";
        String varType = "";
        LangASTNode initializer = null;
        boolean foundEquals = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("const".equals(childType) || "let".equals(childType) || "var".equals(childType)) {
                kind = childType;
            } else if ("variable_declarator".equals(childType)) {
                for (Tree declChild : getChildren(child)) {
                    String declChildType = getNodeType(declChild);
                    if ("=".equals(declChildType)) {
                        foundEquals = true;
                    } else if ("identifier".equals(declChildType) && !foundEquals) {
                        // Only set varName from identifier BEFORE the equals sign
                        varName = getNodeText(declChild);
                    } else if ("type_annotation".equals(declChildType)) {
                        Tree typeNode = getChild(declChild, 1);
                        if (typeNode != null) {
                            varType = getNodeText(typeNode);
                        }
                    } else if (foundEquals) {
                        // Everything after the equals sign is the initializer
                        initializer = visitNode(declChild);
                    }
                }
            }
        }

        if (!varName.isEmpty()) {
            LangSingleVariableDeclaration decl = LangASTNodeFactory.createSingleVariableDeclaration(
                    varName, initializer, extractPosition(node));
            if (!varType.isEmpty()) {
                TypeObjectEnum typeEnum = TypeObjectEnum.fromType(varType);
                decl.setTypeAnnotation(typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT);
                decl.setHasTypeAnnotation(true);
            }
            return LangASTNodeFactory.createExpressionStatement(decl, extractPosition(node));
        }

        return null;
    }

    // ===== EXPRESSIONS =====

    private LangAssignment visitAssignmentExpression(Tree node) {
        LangASTNode left = null;
        String operator = "=";
        LangASTNode right = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (left == null) {
                left = visitNode(child);
            } else if ("=".equals(childType) || "+=".equals(childType) || "-=".equals(childType) ||
                    "*=".equals(childType) || "/=".equals(childType)) {
                operator = childType;
            } else {
                right = visitNode(child);
            }
        }

        return LangASTNodeFactory.createAssignment(operator, left, right, extractPosition(node));
    }

    private LangMethodInvocation visitCallExpression(Tree node) {
        LangASTNode function = null;
        List<LangASTNode> arguments = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("arguments".equals(childType)) {
                for (Tree arg : getChildren(child)) {
                    String argType = getNodeType(arg);
                    if (!"(".equals(argType) && !")".equals(argType) && !",".equals(argType)) {
                        LangASTNode argNode = visitNode(arg);
                        if (argNode != null) {
                            arguments.add(argNode);
                        }
                    }
                }
            } else {
                function = visitNode(child);
            }
        }

        String methodName = "";
        LangASTNode expression = null;

        if (function instanceof LangFieldAccess fieldAccess) {
            LangSimpleName name = fieldAccess.getName();
            methodName = name != null ? name.getIdentifier() : "";
            expression = fieldAccess.getExpression();
        } else if (function instanceof LangSimpleName simpleName) {
            methodName = simpleName.getIdentifier();
        }

        LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));
        invocation.setName(methodName);
        invocation.setExpression(expression);
        for (LangASTNode arg : arguments) {
            invocation.addArgument(arg);
        }
        return invocation;
    }

    private LangFieldAccess visitMemberExpression(Tree node) {
        LangASTNode object = null;
        String property = "";

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("property_identifier".equals(childType)) {
                property = getNodeText(child);
            } else if (!".".equals(childType) && !"?.".equals(childType)) {
                object = visitNode(child);
            }
        }

        return LangASTNodeFactory.createFieldAccess(object, property, extractPosition(node));
    }

    private LangIndexAccess visitSubscriptExpression(Tree node) {
        LangASTNode array = null;
        LangASTNode index = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"[".equals(childType) && !"]".equals(childType)) {
                if (array == null) {
                    array = visitNode(child);
                } else {
                    index = visitNode(child);
                }
            }
        }

        return new LangIndexAccess(extractPosition(node), array, index);
    }

    private LangMethodInvocation visitNewExpression(Tree node) {
        String className = "";
        List<LangASTNode> arguments = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "type_identifier".equals(childType)) {
                className = getNodeText(child);
            } else if ("arguments".equals(childType)) {
                for (Tree arg : getChildren(child)) {
                    String argType = getNodeType(arg);
                    if (!"(".equals(argType) && !")".equals(argType) && !",".equals(argType)) {
                        LangASTNode argNode = visitNode(arg);
                        if (argNode != null) {
                            arguments.add(argNode);
                        }
                    }
                }
            }
        }

        LangMethodInvocation creation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));
        creation.setName(className);
        for (LangASTNode arg : arguments) {
            creation.addArgument(arg);
        }
        return creation;
    }

    private LangInfixExpression visitBinaryExpression(Tree node) {
        LangASTNode left = null;
        String operator = "+";
        LangASTNode right = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            LangASTNode childNode = visitNode(child);

            if (left == null && childNode != null) {
                left = childNode;
            } else if (childNode != null) {
                right = childNode;
            }

            // Check for operator - use isOperator to avoid exception
            String text = getNodeText(child);
            if (OperatorEnum.isOperator(text)) {
                operator = text;
            }
        }

        return LangASTNodeFactory.createInfixExpression(left, right, operator, extractPosition(node));
    }

    private LangPrefixExpression visitUnaryExpression(Tree node) {
        OperatorEnum operator = OperatorEnum.NOT;
        LangASTNode operand = null;

        for (Tree child : getChildren(node)) {
            String text = getNodeText(child);
            if (OperatorEnum.isOperator(text)) {
                operator = OperatorEnum.fromSymbol(text);
            } else {
                operand = visitNode(child);
            }
        }

        LangPrefixExpression expr = new LangPrefixExpression(extractPosition(node));
        expr.setOperator(operator);
        expr.setOperand(operand);
        return expr;
    }

    private LangASTNode visitUpdateExpression(Tree node) {
        // Handle ++x, x++, --x, x--
        LangASTNode operand = null;
        String operator = "";
        boolean isPrefix = false;

        List<Tree> children = getChildren(node);
        if (!children.isEmpty()) {
            String firstType = getNodeText(children.get(0));
            if ("++".equals(firstType) || "--".equals(firstType)) {
                isPrefix = true;
                operator = firstType;
                if (children.size() > 1) {
                    operand = visitNode(children.get(1));
                }
            } else {
                operand = visitNode(children.get(0));
                if (children.size() > 1) {
                    operator = getNodeText(children.get(1));
                }
            }
        }

        OperatorEnum op = "++".equals(operator) ? OperatorEnum.PLUS : OperatorEnum.MINUS;
        if (isPrefix) {
            LangPrefixExpression expr = new LangPrefixExpression(extractPosition(node));
            expr.setOperator(op);
            expr.setOperand(operand);
            return expr;
        } else {
            LangPostfixExpression expr = new LangPostfixExpression(extractPosition(node));
            expr.setOperand(operand);
            expr.setOperator(op);
            return expr;
        }
    }

    private LangTernaryExpression visitTernaryExpression(Tree node) {
        LangASTNode condition = null;
        LangASTNode thenExpr = null;
        LangASTNode elseExpr = null;

        int partIndex = 0;
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"?".equals(childType) && !":".equals(childType)) {
                LangASTNode part = visitNode(child);
                if (part != null) {
                    switch (partIndex) {
                        case 0 -> condition = part;
                        case 1 -> thenExpr = part;
                        case 2 -> elseExpr = part;
                    }
                    partIndex++;
                }
            }
        }

        return new LangTernaryExpression(extractPosition(node), condition, thenExpr, elseExpr);
    }

    private LangLambdaExpression visitArrowFunction(Tree node) {
        List<LangSingleVariableDeclaration> params = new ArrayList<>();
        LangASTNode body = null;
        boolean isAsync = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("formal_parameters".equals(childType)) {
                params = extractParameters(child);
            } else if ("identifier".equals(childType) && params.isEmpty()) {
                // Single parameter without parentheses
                LangSingleVariableDeclaration param = LangASTNodeFactory.createSingleVariableDeclaration(
                        getNodeText(child), null, extractPosition(child));
                param.setParameter(true);
                params.add(param);
            } else if ("statement_block".equals(childType)) {
                body = visitStatementBlock(child);
            } else if (!"=>".equals(childType) && !"async".equals(childType)) {
                body = visitNode(child);
            }
            if ("async".equals(childType)) {
                isAsync = true;
            }
        }

        List<LangASTNode> paramNodes = new ArrayList<>(params);
        return new LangLambdaExpression(extractPosition(node), paramNodes, body);
    }

    private LangParenthesizedExpression visitParenthesizedExpression(Tree node) {
        LangASTNode inner = null;
        for (Tree child : getChildren(node)) {
            if (!"(".equals(getNodeType(child)) && !")".equals(getNodeType(child))) {
                inner = visitNode(child);
                break;
            }
        }
        return new LangParenthesizedExpression(extractPosition(node), inner);
    }

    private LangAwaitExpression visitAwaitExpression(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            if (!"await".equals(getNodeType(child))) {
                expression = visitNode(child);
                break;
            }
        }
        return new LangAwaitExpression(extractPosition(node), expression);
    }

    private LangStringLiteral visitTemplateString(Tree node) {
        return LangASTNodeFactory.createStringLiteral(extractPosition(node), getNodeText(node));
    }

    // ===== LITERALS =====

    private LangSimpleName visitIdentifier(Tree node) {
        return LangASTNodeFactory.createSimpleName(getNodeText(node), extractPosition(node));
    }

    private LangNumberLiteral visitNumberLiteral(Tree node) {
        return LangASTNodeFactory.createNumberLiteral(extractPosition(node), getNodeText(node));
    }

    private LangStringLiteral visitStringLiteral(Tree node) {
        String text = getNodeText(node);
        // Remove quotes
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1);
        }
        return LangASTNodeFactory.createStringLiteral(extractPosition(node), text);
    }

    private LangListLiteral visitArrayLiteral(Tree node) {
        List<LangASTNode> elements = new ArrayList<>();
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"[".equals(childType) && !"]".equals(childType) && !",".equals(childType)) {
                LangASTNode element = visitNode(child);
                if (element != null) {
                    elements.add(element);
                }
            }
        }
        return LangASTNodeFactory.createListLiteral(extractPosition(node), elements);
    }

    private LangDictionaryLiteral visitObjectLiteral(Tree node) {
        LangDictionaryLiteral dict = LangASTNodeFactory.createDictionaryLiteral(extractPosition(node));

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("pair".equals(childType)) {
                Tree keyNode = getChild(child, 0);
                Tree valueNode = getChild(child, 2);
                if (keyNode != null && valueNode != null) {
                    dict.addEntry(visitNode(keyNode), visitNode(valueNode));
                }
            }
        }

        return dict;
    }

    // ===== IMPORTS =====

    private LangImportStatement visitImportStatement(Tree node) {
        List<LangImportStatement.LangImportItem> items = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("string".equals(childType)) {
                String path = getNodeText(child);
                if (path.startsWith("\"") || path.startsWith("'")) {
                    path = path.substring(1, path.length() - 1);
                }
                items.add(new LangImportStatement.LangImportItem(path, null, extractPosition(child)));
            } else if ("import_clause".equals(childType)) {
                // Handle named imports like: import { foo, bar } from 'module'
                for (Tree importChild : getChildren(child)) {
                    String importChildType = getNodeType(importChild);
                    if ("identifier".equals(importChildType)) {
                        String name = getNodeText(importChild);
                        items.add(new LangImportStatement.LangImportItem(name, null, extractPosition(importChild)));
                    } else if ("named_imports".equals(importChildType)) {
                        for (Tree namedImport : getChildren(importChild)) {
                            if ("import_specifier".equals(getNodeType(namedImport))) {
                                String name = getNodeText(namedImport);
                                items.add(new LangImportStatement.LangImportItem(name, null, extractPosition(namedImport)));
                            }
                        }
                    }
                }
            }
        }

        return new LangImportStatement(extractPosition(node), items);
    }

    // ===== DECORATORS =====

    private LangAnnotation visitDecorator(Tree node) {
        String name = "";
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "call_expression".equals(childType)) {
                name = getNodeText(child);
                break;
            }
        }
        if (!name.isEmpty()) {
            LangSimpleName simpleName = LangASTNodeFactory.createSimpleName(name, extractPosition(node));
            return new LangAnnotation(simpleName, extractPosition(node));
        }
        return null;
    }

    // ===== HELPERS =====

    private LangASTNode visitDefaultNode(Tree node) {
        // For unhandled nodes, try to return a simple name or null
        String text = getNodeText(node);
        if (text != null && !text.isEmpty() && !text.contains("\n")) {
            return LangASTNodeFactory.createSimpleName(text, extractPosition(node));
        }
        return null;
    }

    @Override
    protected Tree findChildByType(Tree node, String type) {
        for (Tree child : getChildren(node)) {
            if (type.equals(getNodeType(child))) {
                return child;
            }
        }
        return null;
    }
}
