package extension.base;

import com.github.gumtreediff.tree.TreeContext;
import extension.ast.builder.csharp.CSharpASTBuilder;
import extension.ast.builder.python.PyASTBuilder;
import extension.ast.node.LangASTNode;
import extension.base.lang.csharp.CSharpParser;
import extension.base.lang.python.Python3Lexer;
import extension.base.lang.python.Python3Parser;
import extension.treesitter.TreeSitterASTBuilderFactory;
import extension.treesitter.TreeSitterParserRegistry;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Utility class to create Language AST parsers
 * based on file extensions and supported programming languages.
 *
 * Use system property -Drefactoringminer.use.treesitter=false to disable Tree-sitter
 * and fall back to ANTLR parsing.
 */
public class LangASTUtil {

    /**
     * Feature flag to enable/disable Tree-sitter parsing.
     * Set to false to fall back to ANTLR.
     */
    private static final boolean USE_TREESITTER =
            Boolean.parseBoolean(System.getProperty("refactoringminer.use.treesitter", "true"));

    public static LangASTNode getLangAST(LangSupportedEnum language, String content) throws IOException {

        if (language == null) {
            throw new UnsupportedOperationException("Language not supported for file");
        }

        // Try Tree-sitter path first (if enabled and supported)
        if (USE_TREESITTER && TreeSitterASTBuilderFactory.hasBuilder(language)) {
            try {
                TreeContext treeContext = TreeSitterParserRegistry.parse(language, content);
                return TreeSitterASTBuilderFactory.build(language, treeContext, content);
            } catch (Exception e) {
                // Fall back to ANTLR on Tree-sitter failure
                System.err.println("Tree-sitter parsing failed for " + language + ", falling back to ANTLR: " + e.getMessage());
            }
        }

        // ANTLR fallback path
        return getLegacyANTLRAST(language, content);
    }

    /**
     * Legacy ANTLR parsing path. Used as fallback when Tree-sitter is disabled
     * or not available for a language.
     */
    private static LangASTNode getLegacyANTLRAST(LangSupportedEnum language, String content) throws IOException {
        switch (language) {
            case PYTHON:
                return getCustomPythonAST(new StringReader(content));
            case CSHARP:
                return getCustomCSharpAST(new StringReader(content));
            default:
                throw new UnsupportedOperationException("Parser not implemented for language: " + language);
        }
    }

    public static LangASTNode getCustomPythonAST(Reader r) throws IOException {

        // Parse the Python code
        CharStream input = CharStreams.fromReader(r);
        Python3Lexer lexer = new Python3Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);

        // Get the parse tree
        Python3Parser.File_inputContext parseTree = parser.file_input();

        // Build custom AST
        PyASTBuilder astBuilder = new PyASTBuilder();

        return astBuilder.build(parseTree);
    }


    public static LangASTNode getCustomCSharpAST(Reader r) throws IOException {

        // Parse the C# code
        CharStream input = CharStreams.fromReader(r);
        extension.base.lang.csharp.CSharpLexer lexer = new extension.base.lang.csharp.CSharpLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CSharpParser parser = new CSharpParser(tokens);

        // Get the parse tree
        CSharpParser.Compilation_unitContext parseTree = parser.compilation_unit();

        // Build custom AST
        CSharpASTBuilder astBuilder = new CSharpASTBuilder();

        return astBuilder.build(parseTree);
    }

}
