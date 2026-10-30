package com.hotelbooking.support;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** Exercises Spring's real synchronization lifecycle without a database. */
public class TestTransactionManager extends AbstractPlatformTransactionManager {
    @Override protected Object doGetTransaction() {
        return new Object();
    }
    @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
    }
    @Override protected void doCommit(DefaultTransactionStatus status) {
    }
    @Override protected void doRollback(DefaultTransactionStatus status) {
    }
}
