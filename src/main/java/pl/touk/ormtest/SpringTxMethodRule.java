/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base abstract class for JUnit 4.8+ testing of Jdbc and Ibatis Spring-based DAOs.
 *
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
abstract public class SpringTxMethodRule implements MethodRule {

    private static final Log log = LogFactory.getLog(SpringTxMethodRule.class);

    protected static ConcurrentMap<Thread, TransactionStatus> txStatuses = new ConcurrentHashMap<Thread, TransactionStatus>();
    protected static ConcurrentMap<Thread, DataSourceTransactionManager> txManagers = new ConcurrentHashMap<Thread, DataSourceTransactionManager>();
    protected static ConcurrentMap<String, Set<Thread>> threadsPerTestClass = new ConcurrentHashMap<String, Set<Thread>>();

    public SpringTxMethodRule() {
        String invokerClassName = findInvokingTestClassName();
        threadsPerTestClass.putIfAbsent(invokerClassName, new HashSet<Thread>());
        threadsPerTestClass.get(invokerClassName).add(Thread.currentThread());
    }

    public static String findInvokingTestClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int indexOfStackTraceElementCorrespondingToCurrentMethod = 1;
        for (int i = indexOfStackTraceElementCorrespondingToCurrentMethod; i < stackTrace.length; i++) {
            StackTraceElement el = stackTrace[i];
            Class classOfElement = getStackTraceElementClass(el);
            if (!SpringTxMethodRule.class.isAssignableFrom(classOfElement)) {
                return classOfElement.getName();
            }
        }
        throw new RuntimeException("first test class name not found");
    }

    private static Class getStackTraceElementClass(StackTraceElement el) {
        try {
            return Class.forName(el.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Can be overridden in subclasses and should return a data source. The default implementation of this method
     * returns a data source for in-memory H2 database. This method retuns different data sources if it is invoked
     * in different threads (database name contains hash code of the current thread).
     *
     * @return data source to be used during tests
     */
    protected DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();

        ds.setDriverClassName("org.h2.Driver");
        // If tests are run in parallel, then each thread should have its own database:
        ds.setUrl("jdbc:h2:mem:db" + Thread.currentThread().hashCode() + ";DB_CLOSE_DELAY=-1;AUTOCOMMIT=OFF;MODE=MySQL");
        ds.setUsername("sa");
        ds.setPassword("");

        log.debug(getThreadPrefix() + "creating datasource to " + ds.getUrl());

        return ds;
    }

    abstract protected void ensureTemplateInitialized();

    private void doBeginTransaction() {
        ensureTemplateInitialized();
        if (txStatuses.get(Thread.currentThread()) == null) {
            txStatuses.put(Thread.currentThread(), txManagers.get(Thread.currentThread()).getTransaction(null));
        } else {
            throw new IllegalStateException("transaction already started");
        }
    }

    /**
     * Rollbacks the transaction started in {@link #doBeginTransaction()}.
     */
    private void doRollBackTransaction() {
        if (txStatuses.get(Thread.currentThread()) != null) {
            txManagers.get(Thread.currentThread()).rollback(txStatuses.get(Thread.currentThread()));
            txStatuses.remove(Thread.currentThread());
        } else {
            throw new IllegalStateException("there is no transaction to rollback");
        }
    }

    /**
     * Commits the transaction started in {@link #doBeginTransaction()}.
     */
    private void doCommitTransaction() {
        if (txStatuses.get(Thread.currentThread()) != null) {
            txManagers.get(Thread.currentThread()).commit(txStatuses.get(Thread.currentThread()));
            txStatuses.remove(Thread.currentThread());
        } else {
            throw new IllegalStateException("there is no transaction to commit");
        }
    }

    public final Statement apply(final Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            public void evaluate() throws Throwable {
                log.debug(getThreadPrefix() + "method rule begins");
                beginTransaction();
                try {
                    base.evaluate();
                } finally {
                    rollBackTransaction();
                }
                log.debug(getThreadPrefix() + "method rule ends");
            }
        };
    }

    private void beginTransaction() {
        try {
            doBeginTransaction();
        } catch (RuntimeException e) {
            String s = "failed to begin a transaction";
            log.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private void commitTransaction() {
        try {
            doCommitTransaction();
        } catch (RuntimeException e) {
            String s = "failed to commit the transaction";
            log.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private void rollBackTransaction() {
        try {
            doRollBackTransaction();
        } catch (RuntimeException e) {
            String s = "failed to rollback transaction";
            log.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public void rollBackTransactionAndBeginNewOne() throws SQLException {
        rollBackTransaction();
        beginTransaction();
    }

    public void commitTransactionAndBeginNewOne() throws SQLException {
        commitTransaction();
        beginTransaction();
    }

    private String getThreadPrefix() {
        return "thread " + Thread.currentThread().hashCode() + ": ";
    }
}
