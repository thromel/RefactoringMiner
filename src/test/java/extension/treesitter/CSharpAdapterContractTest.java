package extension.treesitter;

/**
 * Contract validation test for the C# language adapter.
 * Extends AbstractLanguageAdapterTest to verify that the C# adapter
 * fulfills all requirements for refactoring detection.
 *
 * @see docs/LANGUAGE_ADAPTER_CONTRACT.md for the full contract specification
 */
class CSharpAdapterContractTest extends AbstractLanguageAdapterTest {

    @Override
    protected String getSampleClassCode() {
        return """
            public class Calculator
            {
                private int value;

                public int Add(int x)
                {
                    int result = value + x;
                    return result;
                }
            }
            """;
    }

    @Override
    protected String getFileExtension() {
        return ".cs";
    }

    @Override
    protected String getSampleCodeWithMethodCall() {
        return """
            public class Calculator
            {
                private int value;

                public int Calculate(int x)
                {
                    int result = Helper(x);
                    return result;
                }

                private int Helper(int x)
                {
                    return value + x;
                }
            }
            """;
    }
}
