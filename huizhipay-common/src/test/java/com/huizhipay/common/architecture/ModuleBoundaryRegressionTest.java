package com.huizhipay.common.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleBoundaryRegressionTest {

    @Test
    void userAndLedgerDependOnCommonPortsInsteadOfSiblingImplementations() throws IOException {
        Path root = repositoryRoot();

        assertThat(Files.readString(root.resolve("huizhipay-user/pom.xml")))
                .doesNotContain("<artifactId>huizhipay-ledger</artifactId>");
        assertThat(Files.readString(root.resolve("huizhipay-ledger/pom.xml")))
                .doesNotContain("<artifactId>huizhipay-settlement</artifactId>");
        assertSourcesDoNotImport(root.resolve("huizhipay-user/src/main/java"), "com.huizhipay.ledger");
        assertSourcesDoNotImport(root.resolve("huizhipay-ledger/src/main/java"), "com.huizhipay.settlement");
    }

    private void assertSourcesDoNotImport(Path sourceRoot, String forbiddenPackage) throws IOException {
        try (var files = Files.walk(sourceRoot)) {
            List<Path> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains("import " + forbiddenPackage))
                    .toList();
            assertThat(violations).as("forbidden cross-module imports").isEmpty();
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return current.getFileName().toString().equals("huizhipay-common")
                ? current.getParent() : current;
    }
}
