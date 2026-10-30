package com.hotelbooking.shared.logging;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Success messages describe committed state, including when a handler joins another transaction. */
public final class TransactionLog {
    private TransactionLog() {
    }

    public static void afterCommit(Runnable message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            message.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                message.run();
            }
        });
    }
}
