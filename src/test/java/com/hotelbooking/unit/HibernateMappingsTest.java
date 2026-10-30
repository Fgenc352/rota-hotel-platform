package com.hotelbooking.unit;

import com.hotelbooking.reservation.infrastructure.entity.ReservationEntity;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.tools.ToolProvider;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class HibernateMappingsTest {
    @Test
    void everyEntityAndDeclaredJpaQueryBuildsWithoutADatabase() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .build();
        try {
            var sources = new MetadataSources(registry);
            var repositories = new ArrayList<Class<?>>();
            try (var files = Files.walk(Path.of("src/main/java"))) {
                for (var path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String name = Path.of("src/main/java").relativize(path).toString()
                        .replace(java.io.File.separatorChar, '.').replaceAll("\\.java$", "");
                    if (name.contains(".infrastructure.entity.")) {
                        var type = Class.forName(name);
                        if (type.isAnnotationPresent(Entity.class)) {
                            sources.addAnnotatedClass(type);
                        }
                    }
                    if (name.contains(".infrastructure.jpa.")) {
                        repositories.add(Class.forName(name));
                    }
                }
            }
            var metadata = (MetadataImplementor) sources.buildMetadata();
            metadata.validate();
            assertThat(metadata.getEntityBindings()).isNotEmpty();
            try (var factory = metadata.buildSessionFactory();
                var entityManager = factory.createEntityManager()) {
                for (String hql : adapterQueries()) {
                    entityManager.createQuery(hql);
                }
                for (var repository : repositories) {
                    for (var method : repository.getDeclaredMethods()) {
                        var query = method.getAnnotation(Query.class);
                        if (query != null && !query.nativeQuery()) {
                            entityManager.createQuery(query.value());
                        }
                    }
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private List<String> adapterQueries() throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var hql = new ArrayList<String>();
        try (var paths = Files.walk(Path.of("src/main/java"));
            var manager = compiler.getStandardFileManager(null, null, null)) {
            var task = (JavacTask) compiler.getTask(null, manager, null, List.of("-proc:none"), null,
                manager.getJavaFileObjectsFromPaths(paths.filter(path -> path.toString().endsWith(".java")).toList()));
            for (var unit : task.parse()) {
                new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree method, Void ignored) {
                        if (method.getMethodSelect().toString().endsWith(".createQuery") && !method.getArguments().isEmpty()
                            && method.getArguments().getFirst() instanceof LiteralTree literal && literal.getValue() instanceof String query) {
                            hql.add(query);
                        }
                        return super.visitMethodInvocation(method, ignored);
                    }
                }.scan(unit, null);
            }
        }
        assertThat(hql).hasSizeGreaterThan(5);
        return hql;
    }

    @Test
    void aggregateEventVersionIsSeparateFromHibernateOptimisticVersion() throws Exception {
        var entity = ReservationEntity.builder().version(1).status("CONFIRMED").build();
        assertThat(ReservationEntity.class.getDeclaredField("lockVersion").isAnnotationPresent(Version.class)).isTrue();
        assertThat(ReservationEntity.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isFalse();
        assertThat(entity.getLockVersion()).isZero();
        entity.changeStatus("CHECKED_IN", Instant.now());
        assertThat(entity.getVersion()).isEqualTo(2);
        assertThat(entity.getLockVersion()).isZero();
    }
}
