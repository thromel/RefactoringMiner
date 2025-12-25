package extension.treesitter;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.treesitterng.PythonTreeSitterNgTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.CSharpTreeSitterNgTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.TypeScriptTreeSitterNgTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.JavaScriptTreeSitterNgTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.GoTreeSitterNgTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.RustTreeSitterNgTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.RubyTreeSitterNgTreeGenerator;
import com.github.gumtreediff.tree.TreeContext;
import extension.base.LangSupportedEnum;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Central registry for Tree-sitter parsers.
 * Provides a unified interface for parsing source code using Tree-sitter
 * across all supported languages.
 */
public class TreeSitterParserRegistry {

    private static final Map<LangSupportedEnum, Supplier<TreeGenerator>> GENERATOR_FACTORIES = new EnumMap<>(LangSupportedEnum.class);

    static {
        GENERATOR_FACTORIES.put(LangSupportedEnum.PYTHON, PythonTreeSitterNgTreeGenerator::new);
        GENERATOR_FACTORIES.put(LangSupportedEnum.CSHARP, CSharpTreeSitterNgTreeGenerator::new);
        GENERATOR_FACTORIES.put(LangSupportedEnum.TYPESCRIPT, TypeScriptTreeSitterNgTreeGenerator::new);
        GENERATOR_FACTORIES.put(LangSupportedEnum.JAVASCRIPT, JavaScriptTreeSitterNgTreeGenerator::new);
        GENERATOR_FACTORIES.put(LangSupportedEnum.GO, GoTreeSitterNgTreeGenerator::new);
        GENERATOR_FACTORIES.put(LangSupportedEnum.RUST, RustTreeSitterNgTreeGenerator::new);
        GENERATOR_FACTORIES.put(LangSupportedEnum.RUBY, RubyTreeSitterNgTreeGenerator::new);
    }

    private TreeSitterParserRegistry() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if Tree-sitter parsing is supported for the given language.
     *
     * @param language the language to check
     * @return true if Tree-sitter parsing is available for this language
     */
    public static boolean isSupported(LangSupportedEnum language) {
        return language != null && GENERATOR_FACTORIES.containsKey(language);
    }

    /**
     * Parse source code content using Tree-sitter for the given language.
     *
     * @param language the programming language
     * @param content  the source code content
     * @return TreeContext containing the parsed AST
     * @throws IOException if parsing fails
     * @throws UnsupportedOperationException if the language is not supported
     */
    public static TreeContext parse(LangSupportedEnum language, String content) throws IOException {
        if (!isSupported(language)) {
            throw new UnsupportedOperationException(
                    "Tree-sitter parser not available for language: " + language);
        }

        Supplier<TreeGenerator> factory = GENERATOR_FACTORIES.get(language);
        TreeGenerator generator = factory.get();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8));

        return generator.generateFrom().stream(inputStream);
    }

    /**
     * Get a new TreeGenerator instance for the given language.
     *
     * @param language the programming language
     * @return a new TreeGenerator instance
     * @throws UnsupportedOperationException if the language is not supported
     */
    public static TreeGenerator getGenerator(LangSupportedEnum language) {
        if (!isSupported(language)) {
            throw new UnsupportedOperationException(
                    "Tree-sitter parser not available for language: " + language);
        }
        return GENERATOR_FACTORIES.get(language).get();
    }
}
