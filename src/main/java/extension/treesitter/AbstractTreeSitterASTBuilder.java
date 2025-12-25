package extension.treesitter;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import extension.ast.node.LangASTNode;
import extension.ast.node.PositionInfo;

/**
 * Abstract base class for Tree-sitter AST builders.
 * Provides common functionality for traversing Tree-sitter AST and
 * extracting position information.
 */
public abstract class AbstractTreeSitterASTBuilder implements TreeSitterASTBuilder {

    protected TreeContext treeContext;
    protected String sourceCode;
    protected String[] sourceLines;

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
}
