/*
 * Copyright (c) 2010 TouK
 * All rights reserved
 */
package pl.touk.top.ormtest;

import org.junit.rules.MethodRule;
import org.junit.runners.model.Statement;
import org.junit.runners.model.FrameworkMethod;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Class for JUnit 4.8+ tests of Hibernate mappings in projects that use Spring-based DAOs.
 * <p/>
 * This class should be used as follows:
 * <pre><code>
 *
 * public class TransactionalTest {
 *
 *   <b>&#64;Rule
 *   public HibernateSpringTransactionalMethodRule txContext = new HibernateSpringTransactionalMethodRule();</b>
 *
 *   &#64;Before
 *   public void before() {
 *     // Prepare environment for every test in this class. Transaction (new for every test) has already been open:
 *     txContext.getHibernateTemplate().persist(new ExampleEntity(2, "entity created in before()"));
 *   }
 *
 *   &#64;After
 *   public void after() {
 *     // Clean-up after every test in this class. Transaction for the last executed test has not yet been close if it is needed:
 *     txContext.getHibernateTemplate().persist(new ExampleEntity(3, "entity created in after()"));
 *   }
 *
 *   &#64;Test
 *   public void shoudPersistEntity1() throws Exception {
 *       txContext.getHibernateTemplate().persist(new ExampleEntity(1, "name"));
 *   }
 *
 *   &#64;Test
 *   public void shoudPersistEntity2() throws Exception {
 *       txContext.getHibernateTemplate().persist(new ExampleEntity(1, "name"));
 *   }
 * }
 * </code></pre>
 *
 * In above example, if the two tests are executed in parallel then each of them will be executed on different in-memory databases.
 *
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class HibernateSpringTxMethodRule implements MethodRule {

    // Guards assignment of factories and hibernateTemplates:
    private static final Object guard = new Object();
    private static ConcurrentMap<Thread, SessionFactory> factories = new ConcurrentHashMap<Thread, SessionFactory>();
    private static ConcurrentMap<Thread, HibernateTemplate> hibernateTemplates = new ConcurrentHashMap<Thread, HibernateTemplate>();

    private static ConcurrentMap<Thread, Session> sessions = new ConcurrentHashMap<Thread, Session>();

    /**
     * Can be overriden in subclasses and should return a data source. The returned data source is used in the default implementation of {@link #hibernateProperties()}.
     * The default implementation of this method returns a data source for in-memory HSQL database (database name: <code>test</code>, user name: <code>sa</code>, no password).
     *
     * @return data source to be used during tests
     */
    protected DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();

        ds.setDriverClassName("org.hsqldb.jdbcDriver");
        // If tests are run in parallel, then each thread should have its own database:
        ds.setUrl("jdbc:hsqldb:mem:test" + Thread.currentThread().hashCode());
        ds.setUsername("sa");
        ds.setPassword("");

        return ds;
    }

    /**
     * Can be overriden in subclasses and should return Hibernate properties. Returned properties are used in the default implementation of
     * {@link #annotationSessionFactoryBean()}. The default implementation of this method returns following key-value pairs:
     * <dl>
     * <dt><code>hibernate.connection.autocommit</code></dt>
     * <dd><code>false<code></dd>
     * <dt><code>hibernate.hbm2ddl.auto</code></dt>
     * <dd>
     * <code>create-drop</code> (if {@link #dataSource()} returns a data source which has an <code>url</code> property and that property contains <code>hsql</code> substring)<br>
     * <code>validate</code> (otherwise)
     * </dd>
     * </dl>
     *
     * @return Hibernate properties to be used during tests
     */
    protected Properties hibernateProperties() {
        Properties p = new Properties();
        p.setProperty("hibernate.hbm2ddl.auto", "validate");
        String url = getUrlIfAccessorExists(dataSource());
        if (url != null) {
            if (url.startsWith("jdbc:hsqldb")) {
                p.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            }
        }
//        p.setProperty("hibernate.connection.autocommit", "false");
        return p;
    }

    private String getUrlIfAccessorExists(Object o) {
        Method method;
        try {
            method = o.getClass().getMethod("getUrl");
        } catch (NoSuchMethodException e) {
            return null;
        }
        if (method.getReturnType().equals(String.class)) {
            try {
                return (String) method.invoke(o);
            } catch (IllegalAccessException e) {
                return null;
            } catch (InvocationTargetException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Can be overriden in subclasses and should return an {@link org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean}. The returned factory bean is used in the default
     * implementation of {@link #sessionFactory()}. The default implementation of this method returns an <code>AnnotationSessionFactoryBean</code>
     * initialized in the following manner.
     * <ol>
     * <li>The <code>dataSource</code> property is assigned the value returned by {@link #dataSource()}.</li>
     * <li>The <code>hibernateProperties</code> property is assigned the value returned by {@link #hibernateProperties()}.</li>
     * <li>If {@link #annotatedClasses()} returns <code>non-null</code> than the <code>annotatedClasses</code> property is assigned the returned value.
     * Otherwise the <code>packagesToScan</code> property is assigned an one-element array containing the value returned by {@link #packegWithAnnotatedClasses()}.</li>
     * <li>The {@link org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean#afterPropertiesSet afterPropertiesSet()} is invoked.
     * </ol>
     *
     * @return an <code>AnnotationSessionFactoryBean</code>
     */
    protected AnnotationSessionFactoryBean annotationSessionFactoryBean() {
        AnnotationSessionFactoryBean sessionFactoryBean = new AnnotationSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource());
        sessionFactoryBean.setHibernateProperties(hibernateProperties());
        Class[] annotatedClasses = annotatedClasses();
        if (annotatedClasses != null) {
            sessionFactoryBean.setAnnotatedClasses(annotatedClasses);
        } else {
            sessionFactoryBean.setPackagesToScan(new String[]{packegWithAnnotatedClasses()});
        }
        try {
            sessionFactoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return sessionFactoryBean;
    }

    /**
     * Can be overriden in subclasses and should return an array of annotated classes. The returned array is used by the default implementation of {@link #annotationSessionFactoryBean()}.
     * The default implementation of this method returns <code>null</code>.
     *
     * @return annotated classes to be used by Hibernate
     */
    protected Class[] annotatedClasses() {
        return null;
    }

    /**
     * Can be overriden in subclasses and should return a package containing annotated classes to be processed by Hibernate. The default implementation returns
     * a package containing first three parts of the package of this class (for example <code>pl.touk.someproject</code>).
     * The returned package is used in the default implementation of {@link #annotationSessionFactoryBean()} as a search location for annotated classes.
     * The default implementation is suitable in most cases but also not optimal in most cases as annotated classes are probably located in a subpackage of
     * the returned package.
     *
     * @return package containing annotated classes
     */
    protected String packegWithAnnotatedClasses() {
        return "pl.touk.";
    }

    /**
     * Can be overriden in subclasses and should return a session factory that will be used to create a Hibernate session before every JUnit or TestNG test. The default implementation
     * returns the session factory created by invoking {@link #annotationSessionFactoryBean()}.{@link org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean#getObject getObject()}.
     *
     * @return session factory that will be used to create a Hibernate session before every JUnit or TestNG test
     */
    protected SessionFactory sessionFactory() {
        return (SessionFactory) annotationSessionFactoryBean().getObject();
    }

    /**
     * Creates a Hibernate session (invoking {@link #createSession()}) and begins a transaction (invoking {@link #beginTransaction()}).
     * This method is annotated with JUnit's <code>Before</code> and TestNG <code>BeforeMethod</code> annotations.
     *
     * @throws Exception
     */
    public void createSessionAndBeginTransaction() throws Exception {
        createSession();
        beginTransaction();
    }

    /**
     * Rolls back the transaction (invoking {@link #rollBackTransaction()}) started in {@link #beginTransaction()} and closes the
     * Hibernate session (invoking {@link #closeSession()}) created in {@link #createSession()}.
     * This method is annotated with JUnit's <code>After</code> and TestNG <code>AfterMethod</code> annotations.
     */
    public void rollBackTransactionAndCloseSession() {
        rollBackTransaction();
        closeSession();
    }

    private void createSession() {
        if (getSession() == null) {
            ensureSessionFactoryInitialized();
            Session session = SessionFactoryUtils.getSession(factories.get(Thread.currentThread()), true);
            sessions.put(Thread.currentThread(), session);
            TransactionSynchronizationManager.bindResource(factories.get(Thread.currentThread()), new SessionHolder(session));
        }
    }

    private void beginTransaction() {
        if (getSession() != null) {
            getSession().beginTransaction();
        }
    }

    /**
     * Commits the transaction started in {@link #beginTransaction()}. In addition, the Hibernate session (also created in <code>beginTransaction()</code>) is closed.
     * If the Hibernate session was closed prior the invokation of this method nothing is actually done.
     */
    public void rollBackTransaction() {
        if (getSession() != null) {
            if (getSession().getTransaction().isActive()) {
                getSession().getTransaction().rollback();
            }
        }
    }

    /**
     * Commits the transaction started in {@link #beginTransaction()}. In addition, the Hibernate session (also created in <code>beginTransaction()</code>) is closed.
     * If the Hibernate session was closed prior the invokation of this method nothing is actually done.
     */
    public void commitTransaction() {
        if (getSession() != null) {
            if (getSession().getTransaction().isActive()) {
                getSession().getTransaction().commit();
            }
        }
    }

    private Session getSession() {
        return sessions.get(Thread.currentThread());
    }

    private void closeSession() {
        if (getSession() != null) {
            TransactionSynchronizationManager.unbindResource(factories.get(Thread.currentThread()));
            getSession().close();
            removeSession();
//            hibernateTemplates.remove(Thread.currentThread());
        }
    }

    private void removeSession() {
        sessions.remove(Thread.currentThread());
    }

    /**
     * Flashes Hibernate session created in {@link #beginTransaction()}. Some Hibernate mapping errors can be detected only after flush, i.e. after actual database operations are invoked.
     */
    public void flush() {
        if (getSession() != null) {
            getSession().flush();
            getSession().clear();
        }
    }

    public HibernateTemplate getHibernateTemplate() {
        ensureSessionFactoryInitialized();
        return hibernateTemplates.get(Thread.currentThread());
    }

    private void ensureSessionFactoryInitialized() {
        if (factories.get(Thread.currentThread()) == null) {
            synchronized (guard) {
                if (factories.get(Thread.currentThread()) == null) {
                    factories.put(Thread.currentThread(), sessionFactory());
                    hibernateTemplates.put(Thread.currentThread(), new HibernateTemplate(factories.get(Thread.currentThread())));
                }
            }
        }
    }

    public final Statement apply(final Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            public void evaluate() throws Throwable {
                try {
                    createSessionAndBeginTransaction();
                    base.evaluate();
                } finally {
                    rollBackTransactionAndCloseSession();
                }
            }
        };
    }
}