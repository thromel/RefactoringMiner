package org.refactoringminer.rm1;

import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.GitServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHistoryRefactoringMinerImplDefaultPackageTest {

	@TempDir
	Path tempDir;

	@Test
	void detectAtCommitHandlesExtractedOperationsFromDefaultPackageClass() throws Exception {
		DefaultPackageFixture fixture = createDefaultPackageExtractionFixture();
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		List<String> handledCommits = new ArrayList<>();
		List<Refactoring> refactorings = new ArrayList<>();

		try (Repository repository = new GitServiceImpl().openRepository(fixture.repositoryPath().toString())) {
			miner.detectAtCommit(repository, fixture.commitId(), (commitId, detectedRefactorings) -> {
				handledCommits.add(commitId);
				refactorings.addAll(detectedRefactorings);
			});
		}

		assertTrue(handledCommits.contains(fixture.commitId()));
		assertTrue(refactorings.stream()
				.anyMatch(refactoring -> refactoring.getRefactoringType() == RefactoringType.EXTRACT_OPERATION &&
						refactoring.toString().contains("calculateTotal")));
	}

	private DefaultPackageFixture createDefaultPackageExtractionFixture() throws Exception {
		Path repositoryPath = tempDir.resolve("default-package-repo");
		Files.createDirectories(repositoryPath);

		runGit(repositoryPath, "init");
		runGit(repositoryPath, "config", "user.name", "Codex Test");
		runGit(repositoryPath, "config", "user.email", "codex@example.com");

		Path javaFile = repositoryPath.resolve("RefactoringShowcase.java");
		write(javaFile, """
				public class RefactoringShowcase {
				    private final double taxRate = 0.08;

				    public String createStatement(String customerName, int unitPrice, int quantity, int discount) {
				        int subtotal = unitPrice * quantity;
				        int discountedSubtotal = subtotal - discount;
				        int tax = (int) (discountedSubtotal * taxRate);
				        int total = discountedSubtotal + tax;

				        String header = buildHeader(customerName);
				        double dollars = centsToDollars(total);
				        String status = formatPaymentStatus(total);

				        return header + "\\n"
				                + "Subtotal: " + subtotal + "\\n"
				                + "Total: $" + dollars + "\\n"
				                + status;
				    }

				    private String buildHeader(String customerName) {
				        return "Statement for " + customerName.trim().toUpperCase();
				    }

				    private double centsToDollars(int cents) {
				        return cents / 100.0;
				    }

				    private String formatPaymentStatus(int total) {
				        if (total > 0) {
				            return "Payment required";
				        }
				        return "Paid in full";
				    }
				}

				class StatementFormatter {
				}

				class TaxRules {
				}
				""");
		runGit(repositoryPath, "add", ".");
		runGit(repositoryPath, "commit", "-m", "Initial commit");

		write(javaFile, """
				public class RefactoringShowcase {
				    private final StatementFormatter formatter = new StatementFormatter();

				    public String createStatement(String customerName, int unitPrice, int quantity, int discount) {
				        int total = calculateTotal(unitPrice, quantity, discount);
				        int subtotal = unitPrice * quantity;
				        int discountedSubtotal = subtotal - discount;
				        int tax = (int) (discountedSubtotal * new TaxRules().taxRate);

				        String header = formatter.buildHeader(customerName);
				        double dollars = total / 100.0;
				        String status = describePaymentStatus(total);

				        return header + "\\n"
				                + "Subtotal: " + subtotal + "\\n"
				                + "Total: $" + dollars + "\\n"
				                + status;
				    }

				    private int calculateTotal(int unitPrice, int quantity, int discount) {
				        int subtotal = unitPrice * quantity;
				        int discountedSubtotal = subtotal - discount;
				        int tax = (int) (discountedSubtotal * new TaxRules().taxRate);
				        return discountedSubtotal + tax;
				    }

				    private String describePaymentStatus(int total) {
				        if (total > 0) {
				            return "Payment required";
				        }
				        return "Paid in full";
				    }
				}

				class StatementFormatter {
				    public String buildHeader(String customerName) {
				        return "Statement for " + customerName.trim().toUpperCase();
				    }
				}

				class TaxRules {
				    public final double taxRate = 0.08;
				}
				""");
		runGit(repositoryPath, "add", ".");
		runGit(repositoryPath, "commit", "-m", "Apply showcase refactorings");
		return new DefaultPackageFixture(repositoryPath, runGit(repositoryPath, "rev-parse", "HEAD").trim());
	}

	private void write(Path path, String content) throws IOException {
		Files.writeString(path, content, StandardCharsets.UTF_8);
	}

	private String runGit(Path repositoryPath, String... args) throws Exception {
		List<String> command = new ArrayList<>();
		command.add("git");
		for (String arg : args) {
			command.add(arg);
		}
		Process process = new ProcessBuilder(command)
				.directory(repositoryPath.toFile())
				.redirectErrorStream(true)
				.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new IllegalStateException(String.format("git %s failed:%n%s", String.join(" ", args), output));
		}
		return output;
	}

	private record DefaultPackageFixture(Path repositoryPath, String commitId) {
	}
}
