package extension.treesitter;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import extension.ast.node.LangASTNode;
import extension.ast.node.PositionInfo;
import gr.uom.java.xmi.Constants;

import java.util.Set;

/**
 * Abstract base class for Tree-sitter AST builders.
 * Provides common functionality for traversing Tree-sitter AST and
 * extracting position information.
 *
 * Subclasses should implement {@link #getDialect()} to provide
 * language-specific node type mappings.
 */
public abstract class AbstractTreeSitterASTBuilder implements TreeSitterASTBuilder {

    protected TreeContext treeContext;
    protected String sourceCode;
    protected String[] sourceLines;

    /**
     * Get the language-specific dialect for this builder.
     * The dialect provides node type mappings and language-specific behaviors.
     *
     * @return the TreeSitterDialect for this builder's language
     */
    protected abstract TreeSitterDialect getDialect();

    /**
     * Get the language constants for refactoring detection.
     *
     * @return the Constants enum for this language
     */
    protected Constants getConstants() {
        return getDialect().getConstants();
    }

    @Override
    public LangASTNode build(TreeContext treeContext, String sourceCode) {
        this.treeContext = treeContext;
        this.sourceCode = sourceCode;
        this.sourceLines = sourceCode.split("\n", -1);

        Tree root = treeContext.getRoot();
        return buildFromRoot(root);
    }

    /**
     * Build the LangASTNode tree starting from the root Tree-sitter node.
     *
     * @param root the root Tree node
     * @return the root LangASTNode (typically a LangCompilationUnit)
     */
    protected abstract LangASTNode buildFromRoot(Tree root);

    /**
     * Visit a Tree-sitter node and convert it to a LangASTNode.
     * Implementations should dispatch based on node type.
     *
     * @param node the Tree-sitter node
     * @return the corresponding LangASTNode, or null if the node should be skipped
     */
    protected abstract LangASTNode visitNode(Tree node);

    /**
     * Get the type name of a Tree-sitter node.
     *
     * @param node the Tree-sitter node
     * @return the node type name
     */
    protected String getNodeType(Tree node) {
        return node.getType().name;
    }

    /**
     * Get the label (text content) of a Tree-sitter node.
     *
     * @param node the Tree-sitter node
     * @return the node label, or empty string if not a leaf
     */
    protected String getNodeLabel(Tree node) {
        return node.getLabel() != null ? node.getLabel() : "";
    }

    /**
     * Get the source text for a Tree-sitter node.
     *
     * @param node the Tree-sitter node
     * @return the source text spanned by this node
     */
    protected String getNodeText(Tree node) {
        int start = node.getPos();
        int end = node.getEndPos();

        if (start < 0 || end < 0 || start >= sourceCode.length()) {
            return "";
        }

        end = Math.min(end, sourceCode.length());
        return sourceCode.substring(start, end);
    }

    /**
     * Extract position information from a Tree-sitter node.
     *
     * @param node the Tree-sitter node
     * @return PositionInfo with line/column/offset information
     */
    protected PositionInfo extractPosition(Tree node) {
        int startOffset = node.getPos();
        int endOffset = node.getEndPos();

        // Tree-sitter provides row/column via metadata, but we can calculate from offsets
        int[] startLineCol = offsetToLineColumn(startOffset);
        int[] endLineCol = offsetToLineColumn(endOffset);

        return new PositionInfo(
                startLineCol[0],  // startLine (1-based)
                endLineCol[0],    // endLine (1-based)
                startOffset,      // startChar
                endOffset - 1,    // endChar (exclusive to inclusive)
                startLineCol[1],  // startColumn
                endLineCol[1]     // endColumn
        );
    }

    /**
     * Convert a character offset to line and column numbers.
     *
     * @param offset the character offset
     * @return int array with [line (1-based), column (0-based)]
     */
    protected int[] offsetToLineColumn(int offset) {
        if (offset < 0 || sourceCode == null || sourceCode.isEmpty()) {
            return new int[]{1, 0};
        }

        int line = 1;
        int column = 0;
        int currentOffset = 0;

        for (int i = 0; i < sourceCode.length() && currentOffset < offset; i++) {
            if (sourceCode.charAt(i) == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            currentOffset++;
        }

        return new int[]{line, column};
    }

    /**
     * Check if a Tree-sitter node is a leaf node.
     *
     * @param node the Tree-sitter node
     * @return true if the node has no children
     */
    protected boolean isLeaf(Tree node) {
        return node.isLeaf();
    }

    /**
     * Get all children of a Tree-sitter node.
     *
     * @param node the Tree-sitter node
     * @return list of child nodes
     */
    protected java.util.List<Tree> getChildren(Tree node) {
        return node.getChildren();
    }

    /**
     * Get a child node at a specific index.
     *
     * @param node  the parent Tree-sitter node
     * @param index the child index
     * @return the child node, or null if index out of bounds
     */
    protected Tree getChild(Tree node, int index) {
        java.util.List<Tree> children = node.getChildren();
        if (index >= 0 && index < children.size()) {
            return children.get(index);
        }
        return null;
    }

    /**
     * Find the first child with a specific type.
     *
     * @param node     the parent Tree-sitter node
     * @param typeName the type name to look for
     * @return the first matching child, or null if not found
     */
    protected Tree findChildByType(Tree node, String typeName) {
        for (Tree child : node.getChildren()) {
            if (typeName.equals(getNodeType(child))) {
                return child;
            }
        }
        return null;
    }

    /**
     * Find all children with a specific type.
     *
     * @param node     the parent Tree-sitter node
     * @param typeName the type name to look for
     * @return list of matching children
     */
    protected java.util.List<Tree> findChildrenByType(Tree node, String typeName) {
        java.util.List<Tree> result = new java.util.ArrayList<>();
        for (Tree child : node.getChildren()) {
            if (typeName.equals(getNodeType(child))) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Check if a node has a child with the specified type.
     *
     * @param node     the parent Tree-sitter node
     * @param typeName the type name to look for
     * @return true if a child with the type exists
     */
    protected boolean hasChildOfType(Tree node, String typeName) {
        return findChildByType(node, typeName) != null;
    }

    // ===== DIALECT-BASED UTILITY METHODS =====

    /**
     * Check if a node represents a class declaration based on the dialect.
     *
     * @param node the Tree-sitter node
     * @return true if this is a class declaration
     */
    protected boolean isClassDeclaration(Tree node) {
        return getDialect().getClassDeclarationTypes().contains(getNodeType(node));
    }

    /**
     * Check if a node represents a method declaration based on the dialect.
     *
     * @param node the Tree-sitter node
     * @return true if this is a method declaration
     */
    protected boolean isMethodDeclaration(Tree node) {
        String nodeType = getNodeType(node);
        String methodType = getDialect().getMethodDeclarationType();
        String funcType = getDialect().getFunctionDeclarationType();
        return nodeType.equals(methodType) || (funcType != null && nodeType.equals(funcType));
    }

    /**
     * Check if a node represents a variable declaration based on the dialect.
     *
     * @param node the Tree-sitter node
     * @return true if this is a variable declaration
     */
    protected boolean isVariableDeclaration(Tree node) {
        return getDialect().getVariableDeclarationTypes().contains(getNodeType(node));
    }

    /**
     * Check if a node represents an identifier based on the dialect.
     *
     * @param node the Tree-sitter node
     * @return true if this is an identifier
     */
    protected boolean isIdentifier(Tree node) {
        return getDialect().getIdentifierTypes().contains(getNodeType(node));
    }

    /**
     * Check if a node represents a block statement based on the dialect.
     *
     * @param node the Tree-sitter node
     * @return true if this is a block
     */
    protected boolean isBlock(Tree node) {
        String blockType = getDialect().getBlockType();
        return blockType != null && blockType.equals(getNodeType(node));
    }

    /**
     * Check if a method name represents a constructor based on the dialect.
     *
     * @param node       the method node
     * @param methodName the method name
     * @return true if this is a constructor
     */
    protected boolean isConstructor(Tree node, String methodName) {
        return getDialect().isConstructor(node, methodName);
    }

    /**
     * Find the class body within a class declaration node.
     *
     * @param classNode the class declaration node
     * @return the class body node, or null if not found
     */
    protected Tree findClassBody(Tree classNode) {
        String bodyType = getDialect().getClassBodyType();
        if (bodyType != null) {
            return findChildByType(classNode, bodyType);
        }
        return null;
    }

    /**
     * Find the parameter list within a method declaration node.
     *
     * @param methodNode the method declaration node
     * @return the parameter list node, or null if not found
     */
    protected Tree findParameterList(Tree methodNode) {
        String paramListType = getDialect().getParameterListType();
        if (paramListType != null) {
            return findChildByType(methodNode, paramListType);
        }
        return null;
    }

    /**
     * Check if a node is a parameter based on the dialect.
     *
     * @param node the Tree-sitter node
     * @return true if this is a parameter
     */
    protected boolean isParameter(Tree node) {
        return getDialect().getParameterTypes().contains(getNodeType(node));
    }
}
