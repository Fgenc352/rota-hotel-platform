package com.hotelbooking.events.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.backoff.BackOffExecution;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class KafkaRetryPolicyTest {
    @Test
    void databaseConnectionAndTransactionStartupFailuresRemainRetryable() {
        for (Throwable failure : new Throwable[] {
            new DataAccessResourceFailureException("DB offline"),
            new CannotCreateTransactionException("Could not open JPA EntityManager"),
            new IllegalStateException("Listener failed", new CannotCreateTransactionException("DB offline"))
        }) {
            var execution = KafkaDeliveryConfig.retryBackOff(failure).start();
            for (int attempt = 0; attempt<12; attempt++) assertThat(execution.nextBackOff()).isEqualTo(1000L);
        }
    }

    @Test
    void malformedEnvelopesHaveBoundedRetriesBeforeQuarantine() {
        var execution = KafkaDeliveryConfig.retryBackOff(new IllegalArgumentException("Invalid event")).start();
        assertThat(execution.nextBackOff()).isEqualTo(1000L);
        assertThat(execution.nextBackOff()).isEqualTo(1000L);
        assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
    }
    @Test
    void springTranslatedInvalidEnvelopeIsNotMistakenForADatabaseOutage() {
        var invalid = org.springframework.orm.jpa.EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(
            new IllegalArgumentException("Broker event does not match the durable outbox"));
        assertThat(invalid).isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class);
        for (Throwable failure : new Throwable[] {
            invalid,
            new IllegalStateException("Listener failed", invalid),
            new org.springframework.transaction.TransactionSystemException("Transaction failed", invalid)
        }) {
            var execution = KafkaDeliveryConfig.retryBackOff(failure).start();
            assertThat(execution.nextBackOff()).isEqualTo(1000L);
            assertThat(execution.nextBackOff()).isEqualTo(1000L);
            assertThat(execution.nextBackOff()).isEqualTo(BackOffExecution.STOP);
        }
    }

}
