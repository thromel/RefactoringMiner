package extension.treesitter;

import com.github.gumtreediff.tree.TreeContext;
import extension.ast.node.LangASTNode;
import extension.ast.node.declaration.LangMethodDeclaration;
import extension.ast.node.declaration.LangTypeDeclaration;
import extension.ast.node.unit.LangCompilationUnit;
import extension.base.LangSupportedEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CSharpTreeSitterTest {

    @Test
    void testSimpleClass() throws Exception {
        String code = """
            using System;

            namespace MyApp
            {
                public class Calculator
                {
                    private int value;

                    public Calculator(int initial)
                    {
                        this.value = initial;
                    }

                    public int Add(int x)
                    {
                        return value + x;
                    }

                    public int Subtract(int x)
                    {
                        return value - x;
                    }
                }
            }
            """;

        TreeContext treeContext = TreeSitterParserRegistry.parse(LangSupportedEnum.CSHARP, code);
        assertNotNull(treeContext, "TreeContext should not be null");

        LangASTNode ast = TreeSitterASTBuilderFactory.build(LangSupportedEnum.CSHARP, treeContext, code);
        assertNotNull(ast, "AST should not be null");
        assertInstanceOf(LangCompilationUnit.class, ast, "Root should be LangCompilationUnit");

        LangCompilationUnit cu = (LangCompilationUnit) ast;
        assertEquals(LangSupportedEnum.CSHARP, cu.getLanguage());

        // Check imports
        assertFalse(cu.getImports().isEmpty(), "Should have using directives");
        assertEquals("System", cu.getImports().get(0).getImports().get(0).getName());

        // Check types
        assertFalse(cu.getTypes().isEmpty(), "Should have type declarations");
        LangTypeDeclaration calcClass = cu.getTypes().get(0);
        assertEquals("Calculator", calcClass.getName());

        // Check methods
        assertEquals(3, calcClass.getMethods().size(), "Should have 3 methods (constructor + 2 methods)");

        // Check constructor
        LangMethodDeclaration constructor = calcClass.getMethods().stream()
                .filter(LangMethodDeclaration::isConstructor)
                .findFirst()
                .orElse(null);
        assertNotNull(constructor, "Should have a constructor");
        assertEquals("Calculator", constructor.getName());
        assertEquals(1, constructor.getParameters().size());

        // Check Add method
        LangMethodDeclaration addMethod = calcClass.getMethods().stream()
                .filter(m -> "Add".equals(m.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(addMethod, "Should have Add method");
        assertEquals(1, addMethod.getParameters().size());
        assertNotNull(addMethod.getBody(), "Add method should have a body");

        System.out.println("✓ Simple class test passed!");
    }

    @Test
    void testControlFlow() throws Exception {
        String code = """
            public class FlowTest
            {
                public int TestFlow(int x)
                {
                    if (x > 0)
                    {
                        return x * 2;
                    }
                    else if (x < 0)
                    {
                        return x * -1;
                    }
                    else
                    {
                        return 0;
                    }
                }

                public int TestLoop(int n)
                {
                    int sum = 0;
                    for (int i = 0; i < n; i++)
                    {
                        sum += i;
                    }
                    return sum;
                }

                public void TestWhile(int max)
                {
                    int i = 0;
                    while (i < max)
                    {
                        i++;
                    }
                }
            }
            """;

        TreeContext treeContext = TreeSitterParserRegistry.parse(LangSupportedEnum.CSHARP, code);
        LangASTNode ast = TreeSitterASTBuilderFactory.build(LangSupportedEnum.CSHARP, treeContext, code);

        assertInstanceOf(LangCompilationUnit.class, ast);
        LangCompilationUnit cu = (LangCompilationUnit) ast;

        LangTypeDeclaration flowClass = cu.getTypes().get(0);
        assertEquals("FlowTest", flowClass.getName());
        assertEquals(3, flowClass.getMethods().size());

        System.out.println("✓ Control flow test passed!");
    }

    @Test
    void testExpressions() throws Exception {
        String code = """
            public class ExprTest
            {
                public void TestExpressions()
                {
                    int a = 5;
                    int b = 10;
                    int c = a + b * 2;
                    bool flag = a > b && c < 100;
                    string msg = flag ? "yes" : "no";

                    var list = new List<int>();
                    list.Add(a);
                    list.Add(b);

                    int first = list[0];
                }
            }
            """;

        TreeContext treeContext = TreeSitterParserRegistry.parse(LangSupportedEnum.CSHARP, code);
        LangASTNode ast = TreeSitterASTBuilderFactory.build(LangSupportedEnum.CSHARP, treeContext, code);

        assertInstanceOf(LangCompilationUnit.class, ast);
        LangCompilationUnit cu = (LangCompilationUnit) ast;

        LangTypeDeclaration exprClass = cu.getTypes().get(0);
        assertEquals("ExprTest", exprClass.getName());
        assertEquals(1, exprClass.getMethods().size());

        LangMethodDeclaration method = exprClass.getMethods().get(0);
        assertEquals("TestExpressions", method.getName());
        assertNotNull(method.getBody());
        assertFalse(method.getBody().getStatements().isEmpty());

        System.out.println("✓ Expressions test passed!");
    }

    @Test
    void testTryCatch() throws Exception {
        String code = """
            public class ErrorHandler
            {
                public void HandleErrors()
                {
                    try
                    {
                        DoSomething();
                    }
                    catch (ArgumentException ex)
                    {
                        Console.WriteLine(ex.Message);
                    }
                    catch (Exception ex)
                    {
                        throw;
                    }
                    finally
                    {
                        Cleanup();
                    }
                }

                private void DoSomething() { }
                private void Cleanup() { }
            }
            """;

        TreeContext treeContext = TreeSitterParserRegistry.parse(LangSupportedEnum.CSHARP, code);
        LangASTNode ast = TreeSitterASTBuilderFactory.build(LangSupportedEnum.CSHARP, treeContext, code);

        assertInstanceOf(LangCompilationUnit.class, ast);
        LangCompilationUnit cu = (LangCompilationUnit) ast;

        LangTypeDeclaration errorClass = cu.getTypes().get(0);
        assertEquals("ErrorHandler", errorClass.getName());

        System.out.println("✓ Try-catch test passed!");
    }

    @Test
    void testAsync() throws Exception {
        String code = """
            using System.Threading.Tasks;

            public class AsyncService
            {
                public async Task<string> FetchDataAsync(string url)
                {
                    var client = new HttpClient();
                    var response = await client.GetAsync(url);
                    return await response.Content.ReadAsStringAsync();
                }
            }
            """;

        TreeContext treeContext = TreeSitterParserRegistry.parse(LangSupportedEnum.CSHARP, code);
        LangASTNode ast = TreeSitterASTBuilderFactory.build(LangSupportedEnum.CSHARP, treeContext, code);

        assertInstanceOf(LangCompilationUnit.class, ast);
        LangCompilationUnit cu = (LangCompilationUnit) ast;

        LangTypeDeclaration asyncClass = cu.getTypes().get(0);
        assertEquals("AsyncService", asyncClass.getName());

        LangMethodDeclaration asyncMethod = asyncClass.getMethods().get(0);
        assertEquals("FetchDataAsync", asyncMethod.getName());
        assertTrue(asyncMethod.isAsync(), "Method should be marked as async");

        System.out.println("✓ Async test passed!");
    }

    @Test
    void testLambdaAndLinq() throws Exception {
        String code = """
            using System.Linq;

            public class LinqExample
            {
                public void ProcessData()
                {
                    var numbers = new int[] { 1, 2, 3, 4, 5 };
                    var doubled = numbers.Select(x => x * 2);
                    var filtered = numbers.Where(x => x > 2);

                    Func<int, int> square = n => n * n;
                    Action<string> print = msg => Console.WriteLine(msg);
                }
            }
            """;

        TreeContext treeContext = TreeSitterParserRegistry.parse(LangSupportedEnum.CSHARP, code);
        LangASTNode ast = TreeSitterASTBuilderFactory.build(LangSupportedEnum.CSHARP, treeContext, code);

        assertInstanceOf(LangCompilationUnit.class, ast);
        LangCompilationUnit cu = (LangCompilationUnit) ast;

        LangTypeDeclaration linqClass = cu.getTypes().get(0);
        assertEquals("LinqExample", linqClass.getName());

        System.out.println("✓ Lambda and LINQ test passed!");
    }
}
