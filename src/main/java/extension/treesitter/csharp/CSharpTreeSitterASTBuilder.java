package extension.treesitter.csharp;

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
import extension.treesitter.TreeSitterDialect;
import gr.uom.java.xmi.Visibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-sitter AST builder for C#.
 * Converts Tree-sitter C# AST into LangASTNode hierarchy.
 */
public class CSharpTreeSitterASTBuilder extends AbstractTreeSitterASTBuilder {

    private static final CSharpDialect DIALECT = new CSharpDialect();

    @Override
    protected TreeSitterDialect getDialect() {
        return DIALECT;
    }

    @Override
    public String getLanguage() {
        return DIALECT.getLanguageName();
    }

    @Override
    protected LangASTNode buildFromRoot(Tree root) {
        return visitCompilationUnit(root);
    }

    private LangCompilationUnit visitCompilationUnit(Tree node) {
        PositionInfo positionInfo = extractPosition(node);
        LangCompilationUnit compilationUnit = LangASTNodeFactory.createCompilationUnit(positionInfo);
        compilationUnit.setLanguage(LangSupportedEnum.CSHARP);

        for (Tree child : getChildren(node)) {
            processTopLevelDeclaration(child, compilationUnit);
        }

        return compilationUnit;
    }

    private void processTopLevelDeclaration(Tree node, LangCompilationUnit compilationUnit) {
        String nodeType = getNodeType(node);

        switch (nodeType) {
            case "namespace_declaration", "file_scoped_namespace_declaration" -> {
                // Process namespace contents
                for (Tree child : getChildren(node)) {
                    processTopLevelDeclaration(child, compilationUnit);
                }
            }
            case "class_declaration", "struct_declaration", "interface_declaration",
                 "record_declaration", "enum_declaration" -> {
                LangTypeDeclaration typeDecl = visitTypeDeclaration(node);
                if (typeDecl != null) {
                    compilationUnit.addType(typeDecl);
                }
            }
            case "using_directive" -> {
                LangImportStatement importStmt = visitUsingDirective(node);
                if (importStmt != null) {
                    compilationUnit.addImport(importStmt);
                }
            }
            case "global_statement" -> {
                LangASTNode stmt = visitNode(node);
                if (stmt != null) {
                    compilationUnit.addStatement(stmt);
                }
            }
            case "declaration_list" -> {
                for (Tree child : getChildren(node)) {
                    processTopLevelDeclaration(child, compilationUnit);
                }
            }
            default -> {
                // Try to visit as generic node
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
            case "class_declaration", "struct_declaration", "interface_declaration",
                 "record_declaration" -> visitTypeDeclaration(node);
            case "enum_declaration" -> visitEnumDeclaration(node);

            // Member declarations
            case "method_declaration" -> visitMethodDeclaration(node);
            case "constructor_declaration" -> visitConstructorDeclaration(node);
            case "property_declaration" -> visitPropertyDeclaration(node);
            case "field_declaration" -> visitFieldDeclaration(node);

            // Statements
            case "block" -> visitBlock(node);
            case "if_statement" -> visitIfStatement(node);
            case "for_statement" -> visitForStatement(node);
            case "foreach_statement" -> visitForEachStatement(node);
            case "while_statement" -> visitWhileStatement(node);
            case "do_statement" -> visitDoStatement(node);
            case "switch_statement" -> visitSwitchStatement(node);
            case "try_statement" -> visitTryStatement(node);
            case "return_statement" -> visitReturnStatement(node);
            case "break_statement" -> visitBreakStatement(node);
            case "continue_statement" -> visitContinueStatement(node);
            case "throw_statement" -> visitThrowStatement(node);
            case "expression_statement" -> visitExpressionStatement(node);
            case "local_declaration_statement" -> visitLocalDeclarationStatement(node);
            case "using_statement" -> visitUsingStatement(node);
            case "lock_statement" -> visitLockStatement(node);

            // Expressions
            case "assignment_expression" -> visitAssignmentExpression(node);
            case "invocation_expression" -> visitInvocationExpression(node);
            case "member_access_expression" -> visitMemberAccessExpression(node);
            case "element_access_expression" -> visitElementAccessExpression(node);
            case "object_creation_expression" -> visitObjectCreationExpression(node);
            case "binary_expression" -> visitBinaryExpression(node);
            case "prefix_unary_expression" -> visitPrefixUnaryExpression(node);
            case "postfix_unary_expression" -> visitPostfixUnaryExpression(node);
            case "conditional_expression" -> visitConditionalExpression(node);
            case "lambda_expression" -> visitLambdaExpression(node);
            case "parenthesized_expression" -> visitParenthesizedExpression(node);
            case "cast_expression" -> visitCastExpression(node);
            case "await_expression" -> visitAwaitExpression(node);
            case "interpolated_string_expression" -> visitInterpolatedString(node);

            // Literals
            case "identifier" -> visitIdentifier(node);
            case "integer_literal" -> visitIntegerLiteral(node);
            case "real_literal" -> visitRealLiteral(node);
            case "string_literal", "verbatim_string_literal", "raw_string_literal" -> visitStringLiteral(node);
            case "character_literal" -> visitCharacterLiteral(node);
            case "boolean_literal" -> visitBooleanLiteral(node);
            case "null_literal" -> visitNullLiteral(node);

            // Collections
            case "array_creation_expression" -> visitArrayCreation(node);
            case "collection_expression" -> visitCollectionExpression(node);
            case "initializer_expression" -> visitInitializerExpression(node);

            // Special
            case "this_expression" -> visitThisExpression(node);
            case "base_expression" -> visitBaseExpression(node);
            case "typeof_expression" -> visitTypeofExpression(node);
            case "default_expression" -> visitDefaultExpression(node);
            case "nameof_expression" -> visitNameofExpression(node);

            // Ignore or pass through
            case "attribute_list", "attribute" -> visitAttribute(node);
            case "argument_list", "argument" -> visitArgument(node);
            case "parameter_list", "parameter" -> null;
            case "type_argument_list", "type_parameter_list" -> null;

            default -> handleUnknownNode(node);
        };
    }

    // ===== TYPE DECLARATIONS =====

    private LangTypeDeclaration visitTypeDeclaration(Tree node) {
        String className = "";
        List<LangSimpleName> baseTypes = new ArrayList<>();
        List<LangAnnotation> attributes = new ArrayList<>();
        Tree body = null;
        Visibility visibility = Visibility.PRIVATE;
        boolean isAbstract = false;
        boolean isStatic = false;
        boolean isSealed = false;
        boolean isPartial = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> className = getNodeText(child);
                case "declaration_list" -> body = child;
                case "base_list" -> baseTypes = extractBaseTypes(child);
                case "attribute_list" -> {
                    LangAnnotation attr = visitAttribute(child);
                    if (attr != null) attributes.add(attr);
                }
                case "modifier" -> {
                    String mod = getNodeText(child).toLowerCase();
                    switch (mod) {
                        case "public" -> visibility = Visibility.PUBLIC;
                        case "private" -> visibility = Visibility.PRIVATE;
                        case "protected" -> visibility = Visibility.PROTECTED;
                        case "internal" -> visibility = Visibility.PACKAGE;
                        case "abstract" -> isAbstract = true;
                        case "static" -> isStatic = true;
                        case "sealed" -> isSealed = true;
                        case "partial" -> isPartial = true;
                    }
                }
            }
        }

        PositionInfo positionInfo = extractPosition(node);
        LangTypeDeclaration typeDecl = LangASTNodeFactory.createTypeDeclaration(className, positionInfo);

        String typeKind = getNodeType(node).replace("_declaration", "");
        typeDecl.setActualSignature(typeKind + " " + className);
        typeDecl.setVisibility(visibility);
        typeDecl.setAbstract(isAbstract);
        typeDecl.setStatic(isStatic);
        typeDecl.setFinal(isSealed);
        typeDecl.setTopLevel(true);
        typeDecl.setSuperClassNames(baseTypes);
        typeDecl.setLangAnnotations(attributes);

        // Process body
        if (body != null) {
            for (Tree member : getChildren(body)) {
                LangASTNode memberNode = visitNode(member);
                if (memberNode instanceof LangMethodDeclaration method) {
                    typeDecl.addMethod(method);
                } else if (memberNode instanceof LangTypeDeclaration nestedType) {
                    typeDecl.addChild(nestedType);
                } else if (memberNode instanceof LangAssignment assignment) {
                    // Direct LangAssignment from visitFieldDeclaration
                    typeDecl.addAssignment(assignment);
                } else if (memberNode instanceof LangExpressionStatement exprStmt) {
                    if (exprStmt.getExpression() instanceof LangAssignment assignment) {
                        typeDecl.addAssignment(assignment);
                    } else {
                        typeDecl.addStatement(memberNode);
                    }
                } else if (memberNode != null) {
                    typeDecl.addStatement(memberNode);
                }
            }
        }

        return typeDecl;
    }

    private LangTypeDeclaration visitEnumDeclaration(Tree node) {
        String enumName = "";
        List<LangAnnotation> attributes = new ArrayList<>();
        Visibility visibility = Visibility.PRIVATE;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> enumName = getNodeText(child);
                case "attribute_list" -> {
                    LangAnnotation attr = visitAttribute(child);
                    if (attr != null) attributes.add(attr);
                }
                case "modifier" -> {
                    String mod = getNodeText(child).toLowerCase();
                    if ("public".equals(mod)) visibility = Visibility.PUBLIC;
                    else if ("private".equals(mod)) visibility = Visibility.PRIVATE;
                    else if ("protected".equals(mod)) visibility = Visibility.PROTECTED;
                    else if ("internal".equals(mod)) visibility = Visibility.PACKAGE;
                }
            }
        }

        PositionInfo positionInfo = extractPosition(node);
        LangTypeDeclaration typeDecl = LangASTNodeFactory.createTypeDeclaration(enumName, positionInfo);
        typeDecl.setActualSignature("enum " + enumName);
        typeDecl.setVisibility(visibility);
        typeDecl.setEnum(true);
        typeDecl.setLangAnnotations(attributes);

        return typeDecl;
    }

    private List<LangSimpleName> extractBaseTypes(Tree baseList) {
        List<LangSimpleName> baseTypes = new ArrayList<>();
        for (Tree child : getChildren(baseList)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "generic_name".equals(childType) ||
                    "qualified_name".equals(childType)) {
                String name = getNodeText(child);
                baseTypes.add(LangASTNodeFactory.createSimpleName(name, extractPosition(child)));
            }
        }
        return baseTypes;
    }

    // ===== METHOD DECLARATIONS =====

    private LangMethodDeclaration visitMethodDeclaration(Tree node) {
        String methodName = "";
        String returnType = null;
        List<LangSingleVariableDeclaration> parameters = new ArrayList<>();
        List<LangAnnotation> attributes = new ArrayList<>();
        Tree body = null;
        Visibility visibility = Visibility.PRIVATE;
        boolean isAbstract = false;
        boolean isStatic = false;
        boolean isVirtual = false;
        boolean isOverride = false;
        boolean isSealed = false;
        boolean isAsync = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> methodName = getNodeText(child);
                case "predefined_type", "nullable_type", "array_type", "generic_name",
                     "qualified_name" -> {
                    if (returnType == null) returnType = getNodeText(child);
                }
                case "parameter_list" -> parameters = extractParameters(child);
                case "block" -> body = child;
                case "arrow_expression_clause" -> {
                    // Expression-bodied method
                    Tree expr = findChildByType(child, null); // Get first child expression
                    if (expr != null) {
                        body = child;
                    }
                }
                case "attribute_list" -> {
                    LangAnnotation attr = visitAttribute(child);
                    if (attr != null) attributes.add(attr);
                }
                case "modifier" -> {
                    String mod = getNodeText(child).toLowerCase();
                    switch (mod) {
                        case "public" -> visibility = Visibility.PUBLIC;
                        case "private" -> visibility = Visibility.PRIVATE;
                        case "protected" -> visibility = Visibility.PROTECTED;
                        case "internal" -> visibility = Visibility.PACKAGE;
                        case "abstract" -> isAbstract = true;
                        case "static" -> isStatic = true;
                        case "virtual" -> isVirtual = true;
                        case "override" -> isOverride = true;
                        case "sealed" -> isSealed = true;
                        case "async" -> isAsync = true;
                    }
                }
                case "void_keyword" -> returnType = "void";
            }
        }

        LangBlock bodyBlock = null;
        if (body != null) {
            String bodyType = getNodeType(body);
            if ("block".equals(bodyType)) {
                bodyBlock = visitBlock(body);
            } else if ("arrow_expression_clause".equals(bodyType)) {
                // Convert expression body to block with return
                List<LangASTNode> stmts = new ArrayList<>();
                for (Tree exprChild : getChildren(body)) {
                    LangASTNode expr = visitNode(exprChild);
                    if (expr != null) {
                        LangReturnStatement ret = LangASTNodeFactory.createReturnStatement(expr, extractPosition(body));
                        stmts.add(ret);
                        break;
                    }
                }
                bodyBlock = LangASTNodeFactory.createBlock(extractPosition(body), stmts);
            }
        }

        PositionInfo positionInfo = extractPosition(node);
        LangMethodDeclaration methodDecl = LangASTNodeFactory.createMethodDeclaration(
                methodName, positionInfo, parameters, bodyBlock);

        methodDecl.setVisibility(visibility);
        methodDecl.setAbstract(isAbstract);
        methodDecl.setStatic(isStatic);
        methodDecl.setFinal(isSealed);
        methodDecl.setAsync(isAsync);
        methodDecl.setConstructor(false);
        methodDecl.setLangAnnotations(attributes);

        if (returnType != null) {
            methodDecl.setReturnTypeAnnotation(returnType);
        }

        return methodDecl;
    }

    private LangMethodDeclaration visitConstructorDeclaration(Tree node) {
        String className = "";
        List<LangSingleVariableDeclaration> parameters = new ArrayList<>();
        List<LangAnnotation> attributes = new ArrayList<>();
        Tree body = null;
        Visibility visibility = Visibility.PRIVATE;
        boolean isStatic = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> className = getNodeText(child);
                case "parameter_list" -> parameters = extractParameters(child);
                case "block" -> body = child;
                case "attribute_list" -> {
                    LangAnnotation attr = visitAttribute(child);
                    if (attr != null) attributes.add(attr);
                }
                case "modifier" -> {
                    String mod = getNodeText(child).toLowerCase();
                    switch (mod) {
                        case "public" -> visibility = Visibility.PUBLIC;
                        case "private" -> visibility = Visibility.PRIVATE;
                        case "protected" -> visibility = Visibility.PROTECTED;
                        case "internal" -> visibility = Visibility.PACKAGE;
                        case "static" -> isStatic = true;
                    }
                }
            }
        }

        LangBlock bodyBlock = body != null ? visitBlock(body) : null;

        PositionInfo positionInfo = extractPosition(node);
        LangMethodDeclaration methodDecl = LangASTNodeFactory.createMethodDeclaration(
                className, positionInfo, parameters, bodyBlock);

        methodDecl.setVisibility(visibility);
        methodDecl.setStatic(isStatic);
        methodDecl.setConstructor(true);
        methodDecl.setLangAnnotations(attributes);
        methodDecl.setReturnTypeAnnotation("void");

        return methodDecl;
    }

    private LangASTNode visitPropertyDeclaration(Tree node) {
        // Properties are treated as attributes for UMLAttribute creation
        // This is CRITICAL for Rename Attribute and Change Attribute Type detection
        String name = "";
        String type = "";
        LangASTNode initializer = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType)) {
                name = getNodeText(child);
            } else if (List.of("predefined_type", "nullable_type", "array_type",
                    "generic_name", "qualified_name").contains(childType)) {
                if (type.isEmpty()) type = getNodeText(child);
            } else if ("equals_value_clause".equals(childType)) {
                // Property initializer: public int Value { get; set; } = 10;
                Tree valueNode = getChild(child, 1);
                if (valueNode != null) {
                    initializer = visitNode(valueNode);
                }
            }
        }

        if (!name.isEmpty()) {
            // Create LangAssignment so UMLModelAdapter can create UMLAttribute
            // Use LangSingleVariableDeclaration to carry type information
            PositionInfo positionInfo = extractPosition(node);
            LangSingleVariableDeclaration varDecl = LangASTNodeFactory.createSingleVariableDeclaration(
                    name, initializer, positionInfo);
            varDecl.setAttribute(true);
            if (!type.isEmpty()) {
                TypeObjectEnum typeEnum = TypeObjectEnum.fromType(type);
                varDecl.setTypeAnnotation(typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT);
                varDecl.setHasTypeAnnotation(true);
            }
            return LangASTNodeFactory.createAssignment("=", varDecl, initializer, positionInfo);
        }

        return null;
    }

    private LangASTNode visitFieldDeclaration(Tree node) {
        // Extract field as LangAssignment for UMLAttribute creation
        // This is CRITICAL for Rename Attribute and Change Attribute Type detection
        String fieldType = "";
        String fieldName = "";
        LangASTNode initializer = null;
        Visibility visibility = Visibility.PRIVATE;
        boolean isStatic = false;
        boolean isReadonly = false;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "modifier" -> {
                    String mod = getNodeText(child).toLowerCase();
                    switch (mod) {
                        case "public" -> visibility = Visibility.PUBLIC;
                        case "private" -> visibility = Visibility.PRIVATE;
                        case "protected" -> visibility = Visibility.PROTECTED;
                        case "internal" -> visibility = Visibility.PACKAGE;
                        case "static" -> isStatic = true;
                        case "readonly" -> isReadonly = true;
                    }
                }
                case "variable_declaration" -> {
                    // Extract type and variable declarators
                    for (Tree varChild : getChildren(child)) {
                        String varChildType = getNodeType(varChild);
                        if (List.of("predefined_type", "implicit_type", "nullable_type",
                                "array_type", "generic_name", "qualified_name").contains(varChildType)) {
                            fieldType = getNodeText(varChild);
                        } else if ("variable_declarator".equals(varChildType)) {
                            for (Tree declChild : getChildren(varChild)) {
                                String declChildType = getNodeType(declChild);
                                if ("identifier".equals(declChildType)) {
                                    fieldName = getNodeText(declChild);
                                } else if ("equals_value_clause".equals(declChildType)) {
                                    Tree valueNode = getChild(declChild, 1);
                                    if (valueNode != null) {
                                        initializer = visitNode(valueNode);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!fieldName.isEmpty()) {
            // Create LangAssignment so UMLModelAdapter can create UMLAttribute
            // Use LangSingleVariableDeclaration to carry type information
            PositionInfo positionInfo = extractPosition(node);
            LangSingleVariableDeclaration varDecl = LangASTNodeFactory.createSingleVariableDeclaration(
                    fieldName, initializer, positionInfo);
            varDecl.setAttribute(true);
            if (!fieldType.isEmpty()) {
                TypeObjectEnum typeEnum = TypeObjectEnum.fromType(fieldType);
                varDecl.setTypeAnnotation(typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT);
                varDecl.setHasTypeAnnotation(true);
            }

            // Create assignment with variable declaration as left side
            LangAssignment assignment = LangASTNodeFactory.createAssignment("=", varDecl, initializer, positionInfo);

            return assignment;
        }

        return null;
    }

    private List<LangSingleVariableDeclaration> extractParameters(Tree paramList) {
        List<LangSingleVariableDeclaration> params = new ArrayList<>();

        for (Tree child : getChildren(paramList)) {
            String childType = getNodeType(child);
            if ("parameter".equals(childType)) {
                String paramName = "";
                String paramType = "";
                LangASTNode defaultValue = null;

                for (Tree paramChild : getChildren(child)) {
                    String pChildType = getNodeType(paramChild);
                    if ("identifier".equals(pChildType)) {
                        paramName = getNodeText(paramChild);
                    } else if (List.of("predefined_type", "nullable_type", "array_type",
                            "generic_name", "qualified_name").contains(pChildType)) {
                        if (paramType.isEmpty()) paramType = getNodeText(paramChild);
                    } else if ("equals_value_clause".equals(pChildType)) {
                        Tree valueNode = getChild(paramChild, 1);
                        if (valueNode != null) {
                            defaultValue = visitNode(valueNode);
                        }
                    }
                }

                if (!paramName.isEmpty()) {
                    LangSingleVariableDeclaration param = LangASTNodeFactory.createSingleVariableDeclaration(
                            paramName, defaultValue, extractPosition(child));
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

    // ===== STATEMENTS =====

    private LangBlock visitBlock(Tree node) {
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
            switch (childType) {
                case "parenthesized_expression" -> {
                    // Extract condition from parentheses
                    for (Tree inner : getChildren(child)) {
                        if (!"(".equals(getNodeType(inner)) && !")".equals(getNodeType(inner))) {
                            condition = visitNode(inner);
                            break;
                        }
                    }
                }
                case "block" -> {
                    if (thenBlock == null) {
                        thenBlock = visitBlock(child);
                    } else {
                        elseBlock = visitBlock(child);
                    }
                }
                case "else_clause" -> {
                    Tree elseChild = getChild(child, 1);
                    if (elseChild != null) {
                        String elseChildType = getNodeType(elseChild);
                        if ("if_statement".equals(elseChildType)) {
                            elseBlock = visitIfStatement(elseChild);
                        } else if ("block".equals(elseChildType)) {
                            elseBlock = visitBlock(elseChild);
                        }
                    }
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

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "variable_declaration" -> {
                    LangASTNode varDecl = visitVariableDeclaration(child);
                    if (varDecl instanceof LangSingleVariableDeclaration svd) {
                        initializers.add(svd);
                    }
                }
                case "block" -> body = visitBlock(child);
                default -> {
                    // Try to extract condition and updates from expression list
                    if (condition == null && !List.of("for", "(", ")", ";", "{", "}").contains(childType)) {
                        LangASTNode expr = visitNode(child);
                        if (expr != null) {
                            if (condition == null) {
                                condition = expr;
                            } else {
                                updates.add(expr);
                            }
                        }
                    }
                }
            }
        }

        return LangASTNodeFactory.createForStatement(initializers, condition, updates, body, null, extractPosition(node));
    }

    private LangForStatement visitForEachStatement(Tree node) {
        List<LangSingleVariableDeclaration> iterators = new ArrayList<>();
        LangASTNode iterable = null;
        LangBlock body = null;

        String varName = "";
        String varType = "";

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> {
                    if (varName.isEmpty()) varName = getNodeText(child);
                }
                case "predefined_type", "var_pattern", "implicit_type" -> {
                    if (varType.isEmpty()) varType = getNodeText(child);
                }
                case "block" -> body = visitBlock(child);
                default -> {
                    if (iterable == null && !List.of("foreach", "(", ")", "in", "{", "}").contains(childType)) {
                        iterable = visitNode(child);
                    }
                }
            }
        }

        if (!varName.isEmpty()) {
            LangSingleVariableDeclaration iter = LangASTNodeFactory.createSingleVariableDeclaration(
                    varName, null, extractPosition(node));
            iterators.add(iter);
        }

        return LangASTNodeFactory.createForStatement(iterators, iterable, new ArrayList<>(), body, null, extractPosition(node));
    }

    private LangWhileStatement visitWhileStatement(Tree node) {
        LangASTNode condition = null;
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parenthesized_expression".equals(childType)) {
                for (Tree inner : getChildren(child)) {
                    if (!"(".equals(getNodeType(inner)) && !")".equals(getNodeType(inner))) {
                        condition = visitNode(inner);
                        break;
                    }
                }
            } else if ("block".equals(childType)) {
                body = visitBlock(child);
            }
        }

        return LangASTNodeFactory.createWhileStatement(condition, body, null, extractPosition(node));
    }

    private LangWhileStatement visitDoStatement(Tree node) {
        LangASTNode condition = null;
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parenthesized_expression".equals(childType)) {
                for (Tree inner : getChildren(child)) {
                    if (!"(".equals(getNodeType(inner)) && !")".equals(getNodeType(inner))) {
                        condition = visitNode(inner);
                        break;
                    }
                }
            } else if ("block".equals(childType)) {
                body = visitBlock(child);
            }
        }

        // Do-while is modeled as while for simplicity
        return LangASTNodeFactory.createWhileStatement(condition, body, null, extractPosition(node));
    }

    private LangSwitchStatement visitSwitchStatement(Tree node) {
        LangASTNode expression = null;
        List<LangCaseStatement> cases = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parenthesized_expression".equals(childType)) {
                for (Tree inner : getChildren(child)) {
                    if (!"(".equals(getNodeType(inner)) && !")".equals(getNodeType(inner))) {
                        expression = visitNode(inner);
                        break;
                    }
                }
            } else if ("switch_body".equals(childType)) {
                for (Tree section : getChildren(child)) {
                    if ("switch_section".equals(getNodeType(section))) {
                        LangCaseStatement caseStmt = visitSwitchSection(section);
                        if (caseStmt != null) cases.add(caseStmt);
                    }
                }
            }
        }

        return new LangSwitchStatement(extractPosition(node), expression, cases);
    }

    private LangCaseStatement visitSwitchSection(Tree node) {
        LangASTNode label = null;
        List<LangASTNode> statements = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("case_switch_label".equals(childType) || "default_switch_label".equals(childType)) {
                for (Tree labelChild : getChildren(child)) {
                    if (!"case".equals(getNodeType(labelChild)) &&
                            !"default".equals(getNodeType(labelChild)) &&
                            !":".equals(getNodeType(labelChild))) {
                        label = visitNode(labelChild);
                        break;
                    }
                }
            } else {
                LangASTNode stmt = visitNode(child);
                if (stmt != null) statements.add(stmt);
            }
        }

        LangBlock body = LangASTNodeFactory.createBlock(extractPosition(node), statements);
        return new LangCaseStatement(extractPosition(node), label, body);
    }

    private LangTryStatement visitTryStatement(Tree node) {
        LangBlock tryBlock = null;
        List<LangCatchClause> catchClauses = new ArrayList<>();
        LangBlock finallyBlock = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "block" -> {
                    if (tryBlock == null) tryBlock = visitBlock(child);
                }
                case "catch_clause" -> {
                    LangCatchClause clause = visitCatchClause(child);
                    if (clause != null) catchClauses.add(clause);
                }
                case "finally_clause" -> {
                    Tree finallyBlockTree = findChildByType(child, "block");
                    if (finallyBlockTree != null) {
                        finallyBlock = visitBlock(finallyBlockTree);
                    }
                }
            }
        }

        return new LangTryStatement(extractPosition(node), tryBlock, catchClauses, null, finallyBlock);
    }

    private LangCatchClause visitCatchClause(Tree node) {
        List<LangASTNode> exceptionTypes = new ArrayList<>();
        LangSimpleName exceptionVar = null;
        LangASTNode body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("catch_declaration".equals(childType)) {
                for (Tree declChild : getChildren(child)) {
                    String declType = getNodeType(declChild);
                    if ("identifier".equals(declType)) {
                        String name = getNodeText(declChild);
                        if (exceptionTypes.isEmpty()) {
                            exceptionTypes.add(LangASTNodeFactory.createSimpleName(name, extractPosition(declChild)));
                        } else {
                            exceptionVar = LangASTNodeFactory.createSimpleName(name, extractPosition(declChild));
                        }
                    } else if (List.of("predefined_type", "qualified_name", "generic_name").contains(declType)) {
                        exceptionTypes.add(LangASTNodeFactory.createSimpleName(getNodeText(declChild), extractPosition(declChild)));
                    }
                }
            } else if ("block".equals(childType)) {
                body = visitBlock(child);
            }
        }

        LangCatchClause clause = new LangCatchClause(extractPosition(node));
        exceptionTypes.forEach(clause::addExceptionType);
        clause.setExceptionVariable(exceptionVar);
        clause.setBody(body);
        return clause;
    }

    private LangReturnStatement visitReturnStatement(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"return".equals(childType) && !";".equals(childType)) {
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
            String childType = getNodeType(child);
            if (!"throw".equals(childType) && !";".equals(childType)) {
                expression = visitNode(child);
                break;
            }
        }
        return new LangThrowStatement(extractPosition(node), expression, null);
    }

    private LangExpressionStatement visitExpressionStatement(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!";".equals(childType)) {
                expression = visitNode(child);
                break;
            }
        }
        return LangASTNodeFactory.createExpressionStatement(expression, extractPosition(node));
    }

    private LangASTNode visitLocalDeclarationStatement(Tree node) {
        Tree varDecl = findChildByType(node, "variable_declaration");
        if (varDecl != null) {
            return visitVariableDeclaration(varDecl);
        }
        return null;
    }

    private LangASTNode visitVariableDeclaration(Tree node) {
        String typeName = "";
        String varName = "";
        LangASTNode initializer = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (List.of("predefined_type", "implicit_type", "nullable_type",
                    "array_type", "generic_name", "qualified_name", "var_pattern").contains(childType)) {
                typeName = getNodeText(child);
            } else if ("variable_declarator".equals(childType)) {
                for (Tree declChild : getChildren(child)) {
                    String declChildType = getNodeType(declChild);
                    if ("identifier".equals(declChildType)) {
                        varName = getNodeText(declChild);
                    } else if ("equals_value_clause".equals(declChildType)) {
                        Tree valueNode = getChild(declChild, 1);
                        if (valueNode != null) {
                            initializer = visitNode(valueNode);
                        }
                    }
                }
            }
        }

        if (!varName.isEmpty()) {
            LangSingleVariableDeclaration decl = LangASTNodeFactory.createSingleVariableDeclaration(
                    varName, initializer, extractPosition(node));
            if (!typeName.isEmpty()) {
                TypeObjectEnum typeEnum = TypeObjectEnum.fromType(typeName);
                decl.setTypeAnnotation(typeEnum != null ? typeEnum : TypeObjectEnum.OBJECT);
                decl.setHasTypeAnnotation(true);
            }
            return LangASTNodeFactory.createExpressionStatement(decl, extractPosition(node));
        }

        return null;
    }

    private LangWithStatement visitUsingStatement(Tree node) {
        List<LangASTNode> resources = new ArrayList<>();
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("block".equals(childType)) {
                body = visitBlock(child);
            } else if ("variable_declaration".equals(childType) || "parenthesized_expression".equals(childType)) {
                LangASTNode resource = visitNode(child);
                if (resource != null) {
                    resources.add(new LangWithContextItem(extractPosition(child), resource, null));
                }
            }
        }

        return new LangWithStatement(extractPosition(node), resources, body);
    }

    private LangWithStatement visitLockStatement(Tree node) {
        List<LangASTNode> resources = new ArrayList<>();
        LangBlock body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("block".equals(childType)) {
                body = visitBlock(child);
            } else if (!"lock".equals(childType) && !"(".equals(childType) && !")".equals(childType)) {
                LangASTNode expr = visitNode(child);
                if (expr != null) {
                    resources.add(new LangWithContextItem(extractPosition(child), expr, null));
                }
            }
        }

        return new LangWithStatement(extractPosition(node), resources, body);
    }

    private LangImportStatement visitUsingDirective(Tree node) {
        String namespace = "";
        String alias = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "qualified_name".equals(childType)) {
                if (alias == null && namespace.isEmpty()) {
                    namespace = getNodeText(child);
                }
            } else if ("name_equals".equals(childType)) {
                Tree aliasNode = getChild(child, 0);
                if (aliasNode != null && "identifier".equals(getNodeType(aliasNode))) {
                    alias = getNodeText(aliasNode);
                }
            }
        }

        List<LangImportStatement.LangImportItem> items = new ArrayList<>();
        items.add(new LangImportStatement.LangImportItem(namespace, alias, extractPosition(node)));
        return new LangImportStatement(extractPosition(node), items);
    }

    // ===== EXPRESSIONS =====

    private LangAssignment visitAssignmentExpression(Tree node) {
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

    private LangMethodInvocation visitInvocationExpression(Tree node) {
        LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            switch (childType) {
                case "identifier" -> invocation.setName(getNodeText(child));
                case "member_access_expression" -> {
                    Tree objNode = getChild(child, 0);
                    Tree nameNode = null;
                    for (Tree mac : getChildren(child)) {
                        if ("identifier".equals(getNodeType(mac)) || "generic_name".equals(getNodeType(mac))) {
                            nameNode = mac;
                        }
                    }
                    if (objNode != null) {
                        invocation.setExpression(visitNode(objNode));
                    }
                    if (nameNode != null) {
                        invocation.setName(getNodeText(nameNode));
                    }
                }
                case "argument_list" -> {
                    for (Tree arg : getChildren(child)) {
                        if ("argument".equals(getNodeType(arg))) {
                            for (Tree argChild : getChildren(arg)) {
                                LangASTNode argExpr = visitNode(argChild);
                                if (argExpr != null) {
                                    invocation.addArgument(argExpr);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return invocation;
    }

    private LangFieldAccess visitMemberAccessExpression(Tree node) {
        LangASTNode expression = null;
        String memberName = "";

        List<Tree> children = getChildren(node);
        for (int i = 0; i < children.size(); i++) {
            Tree child = children.get(i);
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "generic_name".equals(childType)) {
                if (i == children.size() - 1 || ".".equals(getNodeType(children.get(i)))) {
                    memberName = getNodeText(child);
                } else if (expression == null) {
                    expression = visitNode(child);
                }
            } else if (!".".equals(childType) && expression == null) {
                expression = visitNode(child);
            }
        }

        return LangASTNodeFactory.createFieldAccess(expression, memberName, extractPosition(node));
    }

    private LangIndexAccess visitElementAccessExpression(Tree node) {
        LangASTNode target = null;
        LangASTNode index = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("bracketed_argument_list".equals(childType)) {
                for (Tree bracketChild : getChildren(child)) {
                    if ("argument".equals(getNodeType(bracketChild))) {
                        for (Tree argChild : getChildren(bracketChild)) {
                            index = visitNode(argChild);
                            if (index != null) break;
                        }
                    }
                }
            } else if (!"[".equals(childType) && !"]".equals(childType)) {
                if (target == null) target = visitNode(child);
            }
        }

        return new LangIndexAccess(extractPosition(node), target, index);
    }

    private LangMethodInvocation visitObjectCreationExpression(Tree node) {
        LangMethodInvocation creation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("identifier".equals(childType) || "generic_name".equals(childType) ||
                    "qualified_name".equals(childType)) {
                creation.setName(getNodeText(child));
            } else if ("argument_list".equals(childType)) {
                for (Tree arg : getChildren(child)) {
                    if ("argument".equals(getNodeType(arg))) {
                        for (Tree argChild : getChildren(arg)) {
                            LangASTNode argExpr = visitNode(argChild);
                            if (argExpr != null) {
                                creation.addArgument(argExpr);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return creation;
    }

    private LangInfixExpression visitBinaryExpression(Tree node) {
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

    private LangPrefixExpression visitPrefixUnaryExpression(Tree node) {
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

    private LangPostfixExpression visitPostfixUnaryExpression(Tree node) {
        LangASTNode operand = null;
        String operator = "";

        List<Tree> children = getChildren(node);
        if (children.size() >= 2) {
            operand = visitNode(children.get(0));
            operator = getNodeText(children.get(1));
        }

        LangPostfixExpression expr = new LangPostfixExpression(extractPosition(node));
        expr.setOperand(operand);
        expr.setOperator(OperatorEnum.fromSymbol(operator));
        return expr;
    }

    private LangTernaryExpression visitConditionalExpression(Tree node) {
        LangASTNode condition = null;
        LangASTNode thenExpr = null;
        LangASTNode elseExpr = null;

        List<Tree> children = getChildren(node);
        int exprCount = 0;
        for (Tree child : children) {
            String childType = getNodeType(child);
            if (!"?".equals(childType) && !":".equals(childType)) {
                LangASTNode expr = visitNode(child);
                if (expr != null) {
                    switch (exprCount) {
                        case 0 -> condition = expr;
                        case 1 -> thenExpr = expr;
                        case 2 -> elseExpr = expr;
                    }
                    exprCount++;
                }
            }
        }

        return new LangTernaryExpression(extractPosition(node), condition, thenExpr, elseExpr);
    }

    private LangLambdaExpression visitLambdaExpression(Tree node) {
        List<LangASTNode> params = new ArrayList<>();
        LangASTNode body = null;

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("parameter_list".equals(childType)) {
                for (Tree paramChild : getChildren(child)) {
                    if ("parameter".equals(getNodeType(paramChild))) {
                        Tree idNode = findChildByType(paramChild, "identifier");
                        if (idNode != null) {
                            params.add(LangASTNodeFactory.createSimpleName(getNodeText(idNode), extractPosition(idNode)));
                        }
                    }
                }
            } else if ("identifier".equals(childType)) {
                // Simple lambda: x => ...
                params.add(LangASTNodeFactory.createSimpleName(getNodeText(child), extractPosition(child)));
            } else if ("block".equals(childType)) {
                body = visitBlock(child);
            } else if (!"=>".equals(childType) && body == null) {
                body = visitNode(child);
            }
        }

        return new LangLambdaExpression(extractPosition(node), params, body);
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

    private LangASTNode visitCastExpression(Tree node) {
        LangASTNode expression = null;
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"(".equals(childType) && !")".equals(childType) &&
                    !List.of("predefined_type", "nullable_type", "array_type",
                            "generic_name", "qualified_name").contains(childType)) {
                expression = visitNode(child);
                if (expression != null) break;
            }
        }
        return expression != null ? expression : LangASTNodeFactory.createSimpleName("", extractPosition(node));
    }

    private LangAwaitExpression visitAwaitExpression(Tree node) {
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

    private LangStringLiteral visitInterpolatedString(Tree node) {
        return LangASTNodeFactory.createStringLiteral(extractPosition(node), getNodeText(node));
    }

    // ===== LITERALS =====

    private LangSimpleName visitIdentifier(Tree node) {
        return LangASTNodeFactory.createSimpleName(getNodeText(node), extractPosition(node));
    }

    private LangNumberLiteral visitIntegerLiteral(Tree node) {
        return LangASTNodeFactory.createNumberLiteral(extractPosition(node), getNodeText(node));
    }

    private LangNumberLiteral visitRealLiteral(Tree node) {
        return LangASTNodeFactory.createNumberLiteral(extractPosition(node), getNodeText(node));
    }

    private LangStringLiteral visitStringLiteral(Tree node) {
        String text = getNodeText(node);
        // Remove quotes
        if (text.startsWith("@\"") && text.endsWith("\"")) {
            text = text.substring(2, text.length() - 1);
        } else if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        return LangASTNodeFactory.createStringLiteral(extractPosition(node), text);
    }

    private LangStringLiteral visitCharacterLiteral(Tree node) {
        String text = getNodeText(node);
        if (text.startsWith("'") && text.endsWith("'")) {
            text = text.substring(1, text.length() - 1);
        }
        return LangASTNodeFactory.createStringLiteral(extractPosition(node), text);
    }

    private LangBooleanLiteral visitBooleanLiteral(Tree node) {
        String text = getNodeText(node).toLowerCase();
        return LangASTNodeFactory.createBooleanLiteral(extractPosition(node), "true".equals(text));
    }

    private LangNullLiteral visitNullLiteral(Tree node) {
        return LangASTNodeFactory.createNullLiteral(extractPosition(node));
    }

    // ===== COLLECTIONS =====

    private LangListLiteral visitArrayCreation(Tree node) {
        List<LangASTNode> elements = new ArrayList<>();
        Tree initNode = findChildByType(node, "initializer_expression");
        if (initNode != null) {
            for (Tree child : getChildren(initNode)) {
                String childType = getNodeType(child);
                if (!"{".equals(childType) && !"}".equals(childType) && !",".equals(childType)) {
                    LangASTNode elem = visitNode(child);
                    if (elem != null) elements.add(elem);
                }
            }
        }
        return LangASTNodeFactory.createListLiteral(extractPosition(node), elements);
    }

    private LangListLiteral visitCollectionExpression(Tree node) {
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

    private LangListLiteral visitInitializerExpression(Tree node) {
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

    // ===== SPECIAL EXPRESSIONS =====

    private LangSimpleName visitThisExpression(Tree node) {
        return LangASTNodeFactory.createSimpleName("this", extractPosition(node));
    }

    private LangSimpleName visitBaseExpression(Tree node) {
        return LangASTNodeFactory.createSimpleName("base", extractPosition(node));
    }

    private LangMethodInvocation visitTypeofExpression(Tree node) {
        LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));
        invocation.setName("typeof");
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"typeof".equals(childType) && !"(".equals(childType) && !")".equals(childType)) {
                LangASTNode arg = visitNode(child);
                if (arg != null) invocation.addArgument(arg);
            }
        }
        return invocation;
    }

    private LangMethodInvocation visitDefaultExpression(Tree node) {
        LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));
        invocation.setName("default");
        return invocation;
    }

    private LangMethodInvocation visitNameofExpression(Tree node) {
        LangMethodInvocation invocation = LangASTNodeFactory.createMethodInvocation(extractPosition(node));
        invocation.setName("nameof");
        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if (!"nameof".equals(childType) && !"(".equals(childType) && !")".equals(childType)) {
                LangASTNode arg = visitNode(child);
                if (arg != null) invocation.addArgument(arg);
            }
        }
        return invocation;
    }

    private LangAnnotation visitAttribute(Tree node) {
        String attrName = "";
        List<LangASTNode> args = new ArrayList<>();

        for (Tree child : getChildren(node)) {
            String childType = getNodeType(child);
            if ("attribute".equals(childType)) {
                for (Tree attrChild : getChildren(child)) {
                    String attrChildType = getNodeType(attrChild);
                    if ("identifier".equals(attrChildType) || "qualified_name".equals(attrChildType)) {
                        attrName = getNodeText(attrChild);
                    } else if ("attribute_argument_list".equals(attrChildType)) {
                        for (Tree argChild : getChildren(attrChild)) {
                            if ("attribute_argument".equals(getNodeType(argChild))) {
                                for (Tree exprChild : getChildren(argChild)) {
                                    LangASTNode expr = visitNode(exprChild);
                                    if (expr != null) {
                                        args.add(expr);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if ("identifier".equals(childType) || "qualified_name".equals(childType)) {
                attrName = getNodeText(child);
            }
        }

        if (!attrName.isEmpty()) {
            LangSimpleName name = LangASTNodeFactory.createSimpleName(attrName, extractPosition(node));
            LangAnnotation annotation = new LangAnnotation(name, extractPosition(node));
            annotation.setArguments(args);
            return annotation;
        }

        return null;
    }

    private LangASTNode visitArgument(Tree node) {
        for (Tree child : getChildren(node)) {
            LangASTNode result = visitNode(child);
            if (result != null) return result;
        }
        return null;
    }

    // ===== HELPERS =====

    private LangASTNode handleUnknownNode(Tree node) {
        // Try to visit children and return first non-null result
        for (Tree child : getChildren(node)) {
            LangASTNode result = visitNode(child);
            if (result != null) return result;
        }
        // Return identifier for leaf nodes with labels
        if (isLeaf(node)) {
            String label = getNodeLabel(node);
            if (!label.isEmpty()) {
                return LangASTNodeFactory.createSimpleName(label, extractPosition(node));
            }
        }
        return null;
    }
}
