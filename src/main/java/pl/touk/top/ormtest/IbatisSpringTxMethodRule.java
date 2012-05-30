/*
 * Copyright (c) 2010 TouK
 * All rights reserved
 */
package pl.touk.top.ormtest;

import org.junit.rules.MethodRule;
import org.junit.runners.model.Statement;
import org.junit.runners.model.FrameworkMethod;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.orm.ibatis.SqlMapClientTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.TransactionStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.SQLException;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Class for JUnit 4.8+ tests of Ibatis mappings in projects that use Spring-based DAOs.
 * <p/>
 * This class should be used as follows:
 * <pre><code>
 * <p/>
 * public class TransactionalTest {
 * <p/>
 *   <b>&#64;Rule
 *   public IbatisSpringTxMethodRule txContext = new IbatisSpringTxMethodRule();</b>
 * <p/>
 *   &#64;Before
 *   public void before() {
 *     // Prepare environment for every test in this class (transaction (new for every test) has already been open):
 *     SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(txContext.getSqlMapClientTemplate().getDataSource()), new ClassPathResource("<name of a script that creates database>"), false);
 *     txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(1, "nameInBefore"));
 *   }
 * <p/>
 *   &#64;After
 *   public void after() {
 *     // Clean-up after every test in this class. Transaction for the last executed test has not yet been closed if it is needed:
 *     txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(1, "nameInAfter"));
 *   }
 * <p/>
 *   &#64;Test
 *   public void shoudPersistEntity1() throws Exception {
 *     txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(2, "name"));
 *   }
 * <p/>
 *   &#64;Test
 *   public void shoudPersistEntity2() throws Exception {
 *     txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(2, "name"));
 *   }
 * }
 * </code></pre>
 * <p/>
 * In above example, if the two tests are executed in parallel then each of them will be executed on different
 * in-memory databases.
 *
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class IbatisSpringTxMethodRule implements MethodRule {

    private static final Log log = LogFactory.getLog(IbatisSpringTxMethodRule.class);

    private static volatile String sqlMapConfig = "/sqlmap-config.xml";

    // Guards assignment of sqlMapClient:
    private static final Object guard = new Object();
    private static volatile SqlMapClient sqlMapClient = null;

    private static ConcurrentMap<Thread, SqlMapClientTemplate> sqlMapClientTemplates = new ConcurrentHashMap<Thread, SqlMapClientTemplate>();
    private static ConcurrentMap<Thread, TransactionStatus> txStatuses = new ConcurrentHashMap<Thread, TransactionStatus>();
    private static ConcurrentMap<Thread, DataSourceTransactionManager> txManagers = new ConcurrentHashMap<Thread, DataSourceTransactionManager>();
    private static ConcurrentMap<String, Set<Thread>> threadsPerTestClass = new ConcurrentHashMap<String, Set<Thread>>();

    public IbatisSpringTxMethodRule() {
        String invokerClassName = findInvokingTestClassName();
        threadsPerTestClass.putIfAbsent(invokerClassName, new HashSet<Thread>());
        threadsPerTestClass.get(invokerClassName).add(Thread.currentThread());
    }

    private static String findInvokingTestClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int indexOfStackTraceElementCorrespondingToCurrentMethod = 1;
        for (int i = indexOfStackTraceElementCorrespondingToCurrentMethod; i < stackTrace.length; i++) {
            StackTraceElement el = stackTrace[i];
            Class classOfElement = getStackTraceElementClass(el);
            if (!IbatisSpringTxMethodRule.class.isAssignableFrom(classOfElement)) {
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

    public static void setSqlMapConfig(String sqlMapConfig) {
        IbatisSpringTxMethodRule.sqlMapConfig = sqlMapConfig;
    }

    public static String getSqlMapConfig() {
        return sqlMapConfig;
    }

    /**
     * Can be overriden in subclasses and should return a data source. The default implementation of this method
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

        log.info(getThreadPrefix() + "creating datasource to " + ds.getUrl());

        return ds;
    }

    /**
     * Can be overriden in subclasses and should return an {@link org.springframework.orm.ibatis.SqlMapClientFactoryBean}.
     * The returned factory bean is used in the default implementation of {@link #sqlMapClient()}. The default
     * implementation of this method returns an <code>SqlMapClientFactoryBean</code> initialized in the following
     * manner.
     * <ol>
     * <li>The <code>dataSource</code> property is assigned the value returned by {@link #dataSource()}.</li>
     * <li>The <code>configLocation</code> property is assigned the value "/sqlmap-config.xml".</li>
     * <li>The {@link org.springframework.orm.ibatis.SqlMapClientFactoryBean#afterPropertiesSet()} is invoked.
     * </ol>
     *
     * @return an <code>SqlMapClientFactoryBean</code>
     */
    protected SqlMapClientFactoryBean sqlMapClientFactoryBean() {
        SqlMapClientFactoryBean sqlMapClientFactoryBean = new SqlMapClientFactoryBean();
        sqlMapClientFactoryBean.setConfigLocation(new ClassPathResource(sqlMapConfig));
        try {
            sqlMapClientFactoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return sqlMapClientFactoryBean;
    }

    /**
     * Can be overriden in subclasses and should return an <code>SqlMapClient</code> that will be used to create an
     * {@link org.springframework.orm.ibatis.SqlMapClientTemplate}. The default implementation
     * returns <code>SqlMapClient</code> created by invoking
     * {@link #sqlMapClientFactoryBean()}.{@link SqlMapClientFactoryBean#getObject getObject()}.
     *
     * @return sqlMapClient that will be used during tests
     */
    protected SqlMapClient sqlMapClient() {
        return (SqlMapClient) sqlMapClientFactoryBean().getObject();
    }

    private void doBeginTransaction() {
        ensureSqlMapClientTemplateInitialized();
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

    public SqlMapClientTemplate getSqlMapClientTemplate() {
        ensureSqlMapClientTemplateInitialized();
        return sqlMapClientTemplates.get(Thread.currentThread());
    }

    private void ensureSqlMapClientTemplateInitialized() {
        if (sqlMapClient == null) {
            synchronized (guard) {
                if (sqlMapClient == null) {
                    sqlMapClient = sqlMapClient();
                }
            }
        }
        if (sqlMapClientTemplates.get(Thread.currentThread()) == null) {
            SqlMapClientTemplate template = new SqlMapClientTemplate(dataSource(), sqlMapClient);
            sqlMapClientTemplates.put(Thread.currentThread(), template);
            txManagers.put(Thread.currentThread(), new DataSourceTransactionManager(template.getDataSource()));
        }
    }

    public final Statement apply(final Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            public void evaluate() throws Throwable {
                log.info(getThreadPrefix() + "method rule begins");
                beginTransaction();
                try {
                    base.evaluate();
                } finally {
                    rollBackTransaction();
                }
                log.info(getThreadPrefix() + "method rule ends");
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

    public static void resetThreadsForCurrentTestClass() {
        synchronized (guard) {
            sqlMapClient = null;
        }
        String invokerClassName = findInvokingTestClassName();
        Set<Thread> threads = threadsPerTestClass.get(invokerClassName);
        if (threads != null) {
            for (Thread t: threads) {
                sqlMapClientTemplates.remove(t);
                txManagers.remove(t);
                txStatuses.remove(t);
            }
        }
    }
}
