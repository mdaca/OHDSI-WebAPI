package org.ohdsi.webapi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseTestCleanupService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Empties a table populated by a test by executing a native query.
     * @param tableName the table having rows deleted
     */
    @Transactional
    public void truncateTable(String tableName) {
        String truncateCommand =
                "TRUNCATE TABLE %s RESTART IDENTITY CASCADE".formatted(tableName);
//        e.g., "TRUNCATE TABLE public.concept_set RESTART IDENTITY CASCADE";
//      jdbcTemplate.execute("TRUNCATE %s CASCADE".formatted(tableName));
        entityManager.createNativeQuery(truncateCommand).executeUpdate();
    }

    protected void resetSequence(String sequenceName) {
        entityManager.createNativeQuery("ALTER SEQUENCE %s RESTART WITH 1".formatted(sequenceName));
    }

}

