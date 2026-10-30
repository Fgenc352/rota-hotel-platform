package com.hotelbooking.shared.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaQueries {
    private final EntityManager entityManager;

    public List<Tuple> tuples(String sql, Map<String, ?> parameters) {
        NativeQuery<Tuple> query = entityManager.unwrap(Session.class).createNativeQuery(sql, Tuple.class);
        bind(query, parameters);
        return query.getResultList();
    }

    public long count(String sql, Map<String, ?> parameters) {
        var query = entityManager.createNativeQuery(sql);
        bind(query, parameters);
        return ((Number) query.getSingleResult()).longValue();
    }

    public int execute(String sql, Map<String, ?> parameters) {
        entityManager.flush();
        var query = entityManager.createNativeQuery(sql);
        bind(query, parameters);
        return query.executeUpdate();
    }

    private void bind(jakarta.persistence.Query query, Map<String, ?> values) {
        for (var parameter : query.getParameters()) {
            String name = parameter.getName();
            if (!values.containsKey(name)) {
                throw new IllegalArgumentException("Missing persistence parameter: " + name);
            }
            query.setParameter(name, values.get(name));
        }
    }
}
