package extension.treesitter;

import com.github.gumtreediff.tree.TreeContext;
import extension.ast.node.LangASTNode;
import extension.base.LangSupportedEnum;
import extension.treesitter.csharp.CSharpTreeSitterASTBuilder;
import extension.treesitter.python.PythonTreeSitterASTBuilder;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for creating language-specific Tree-sitter AST builders.
 * Provides a unified interface for obtaining builders for different languages.
 */
public class TreeSitterASTBuilderFactory {

    private static final Map<LangSupportedEnum, Supplier<TreeSitterASTBuilder>> BUILDER_FACTORIES =
            new EnumMap<>(LangSupportedEnum.class);

    static {
        // Register Python builder
        BUILDER_FACTORIES.put(LangSupportedEnum.PYTHON, PythonTreeSitterASTBuilder::new);

        // Register C# builder
        BUILDER_FACTORIES.put(LangSupportedEnum.CSHARP, CSharpTreeSitterASTBuilder::new);

        // Future builders will be registered here:
        // BUILDER_FACTORIES.put(LangSupportedEnum.TYPESCRIPT, TypeScriptTreeSitterASTBuilder::new);
        // BUILDER_FACTORIES.put(LangSupportedEnum.JAVASCRIPT, JavaScriptTreeSitterASTBuilder::new);
        // BUILDER_FACTORIES.put(LangSupportedEnum.GO, GoTreeSitterASTBuilder::new);
        // BUILDER_FACTORIES.put(LangSupportedEnum.RUST, RustTreeSitterASTBuilder::new);
        // BUILDER_FACTORIES.put(LangSupportedEnum.RUBY, RubyTreeSitterASTBuilder::new);
    }

    private TreeSitterASTBuilderFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if a Tree-sitter AST builder is available for the given language.
     *
     * @param language the language to check
     * @return true if a builder is available
     */
    public static boolean hasBuilder(LangSupportedEnum language) {
        return language != null && BUILDER_FACTORIES.containsKey(language);
    }

    /**
     * Create a new Tree-sitter AST builder for the given language.
     *
     * @param language the target language
     * @return a new TreeSitterASTBuilder instance
     * @throws UnsupportedOperationException if no builder is available for the language
     */
    public static TreeSitterASTBuilder create(LangSupportedEnum language) {
        if (!hasBuilder(language)) {
            throw new UnsupportedOperationException(
                    "No Tree-sitter AST builder available for language: " + language);
        }
        return BUILDER_FACTORIES.get(language).get();
    }

    /**
     * Build a LangASTNode tree from Tree-sitter TreeContext for the given language.
     *
     * @param language    the source language
     * @param treeContext the Tree-sitter parsed tree context
     * @param sourceCode  the original source code
     * @return the root LangASTNode
     * @throws UnsupportedOperationException if no builder is available for the language
     */
    public static LangASTNode build(LangSupportedEnum language, TreeContext treeContext, String sourceCode) {
        TreeSitterASTBuilder builder = create(language);
        return builder.build(treeContext, sourceCode);
    }
}
