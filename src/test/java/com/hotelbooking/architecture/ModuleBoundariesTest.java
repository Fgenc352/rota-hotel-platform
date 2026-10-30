package com.hotelbooking.architecture;

import com.sun.source.util.JavacTask;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Uses the JDK Java parser, not regex, to enforce the declared source dependency boundaries. */
@Tag("unit")
class ModuleBoundariesTest {
    private static final String ROOT = "com.hotelbooking.";
    private record Source(String name, String packageName, List<String> imports) {
    }

    private List<Source> sources() throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("Architecture tests require a JDK").isNotNull();
        try (var paths = Files.walk(Path.of("src/main/java"));
            var files = compiler.getStandardFileManager(null, null, null)) {
            var java = paths.filter(path -> path.toString().endsWith(".java")).toList();
            var task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
                files.getJavaFileObjectsFromPaths(java));
            var sources = new ArrayList<Source>();
            for (var unit : task.parse()) {
                sources.add(new Source(unit.getSourceFile().getName(), unit.getPackageName().toString(),
                    unit.getImports().stream().map(value -> value.getQualifiedIdentifier().toString()).toList()));
            }
            return sources;
        }
    }

    @Test
    void domainAndContractsDoNotDependOnFrameworkOrAdapters() throws Exception {
        for (var source : sources()) {
            if (!source.packageName().contains(".domain") && !source.packageName().startsWith(ROOT + "contracts")) {
                continue;
            }
            for (var dependency : source.imports()) {
                assertThat(dependency).as(source.name()).doesNotStartWith("org.springframework.").doesNotStartWith("jakarta.servlet.").doesNotStartWith("jakarta.persistence.").doesNotStartWith("org.hibernate.")
                    .doesNotContain(".infrastructure.", ".api.", ".application.", ".persistence.");
            }
        }
    }

    @Test
    void applicationCodeDependsOnPortsRatherThanInfrastructureOrHttp() throws Exception {
        for (var source : sources()) {
            if (!source.packageName().contains(".application.")) {
                continue;
            }
            for (var dependency : source.imports()) {
                assertThat(dependency).as(source.name()).doesNotContain(".infrastructure.", ".api.", ".persistence.")
                    .doesNotStartWith("org.springframework.jdbc.").doesNotStartWith("org.springframework.http.").doesNotStartWith("jakarta.servlet.").doesNotStartWith("jakarta.persistence.").doesNotStartWith("org.hibernate.");
            }
        }
    }

    @Test
    void apiCodeUsesApplicationModelsRatherThanPersistenceEntities() throws Exception {
        for (var source : sources()) {
            if (!source.packageName().contains(".api")) {
                continue;
            }
            for (var dependency : source.imports()) {
                assertThat(dependency).as(source.name()).doesNotContain(".infrastructure.", ".persistence.");
            }
        }
    }

    @Test
    void modulesDoNotAccessOtherModulesInfrastructure() throws Exception {
        for (var source : sources()) {
            String module = module(source.packageName());
            for (var dependency : source.imports()) {
                if (dependency.startsWith(ROOT) && dependency.contains(".infrastructure.")) {
                    assertThat(module(dependency)).as(source.name() + " -> " + dependency).isEqualTo(module);
                }
            }
        }
    }

    @Test
    void moduleDependencyGraphHasNoCycles() throws Exception {
        Map<String, Set<String>> graph = new HashMap<>();
        for (var source : sources()) {
            String owner = module(source.packageName());
            graph.computeIfAbsent(owner, key -> new HashSet<>());
            for (var dependency : source.imports()) {
                if (dependency.startsWith(ROOT) && !module(dependency).equals(owner)) {
                    graph.get(owner).add(module(dependency));
                }
            }
        }
        for (String node : graph.keySet()) {
            visit(node, graph, new ArrayList<>());
        }
    }

    private void visit(String node, Map<String, Set<String>> graph, List<String> path) {
        assertThat(path).as("Module dependency cycle ending at " + node).doesNotContain(node);
        var next = new ArrayList<>(path);
        next.add(node);
        for (String dependency : graph.getOrDefault(node, Set.of())) {
            visit(dependency, graph, next);
        }
    }

    private String module(String name) {
        return name.equals("com.hotelbooking") ? "bootstrap" : name.substring(ROOT.length()).split("\\.")[0];
    }
}
