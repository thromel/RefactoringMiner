package extension.treesitter;

import com.github.gumtreediff.tree.TreeContext;
import extension.ast.node.LangASTNode;

/**
 * Interface for building LangASTNode trees from Tree-sitter parsed TreeContext.
 *
 * Implementations of this interface transform Tree-sitter AST nodes into
 * the RefactoringMiner's internal LangASTNode representation.
 */
public interface TreeSitterASTBuilder {

    /**
     * Build a LangASTNode tree from the given TreeContext.
     *
     * @param treeContext the Tree-sitter parsed tree context
     * @param sourceCode  the original source code (for extracting text)
     * @return the root LangASTNode representing the compilation unit
     */
    LangASTNode build(TreeContext treeContext, String sourceCode);

    /**
     * Get the language this builder supports.
     *
     * @return the supported language identifier
     */
    String getLanguage();
}
