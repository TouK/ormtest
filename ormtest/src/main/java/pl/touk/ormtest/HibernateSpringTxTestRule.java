/*
* Copyright (c) 2011 TouK
* All rights reserved
*/
package pl.touk.ormtest;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Class for JUnit testing of Spring-based Hibernate DAOs. Such DAOs extend Spring's {@link HibernateDaoSupport}
 * which in turn needs a {@link HibernateTemplate}. Such template can be obtained through
 * {@link #getHibernateTemplate()} like in the example below.
 * <pre>
 * public class ExampleTransactionalTest {
 *   <b>&#64;Rule
 *   public HibernateSpringTxTestRule txContext = new HibernateSpringTxTestRule();</b><br/>
 *   private ExampleHibernateDao dao = new ExampleHibernateDao(<b>txContext.getHibernateTemplate()</b>);<br/>
 *   &#64;Before
 *   public void before() {
 *     // Transaction (new for every test method) has already been open:
 *     dao.save(new ExampleEntity(2, "entity created in before()"));
 *   }<br/>
 *   &#64;After
 *   public void after() {
 *     // Transaction for the last executed test has not yet been closed - if it is needed:
 *     dao.save(new ExampleEntity(3, "entity created in after()"));
 *   }<br/>
 *   &#64;Test
 *   public void should_persist_entity() throws Exception {
 *       dao.save(new ExampleEntity(1, "name"));
 *   }<br/>
 *   &#64;Test
 *   public void should_persist_entity_too() throws Exception {
 *       dao.save(new ExampleEntity(1, "name"));
 *   }
 * }
 * </pre>
 * In above example, if the two tests are executed in parallel then each of them will be executed on a different
 * in-memory database.
 * <p>
 * By default <code>HibernateSpringTxTestRule</code> scans for entity classes so every
 * class marked with <code>&#64;Entity</code> will be available during tests.
 *
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class HibernateSpringTxTestRule implements TestRule {
    private final static ThreadLocal<SessionFactory> FACTORY = new ThreadLocal<SessionFactory>();
    private final static ThreadLocal<HibernateTemplate> HIBERNATE_TEMPLATE = new ThreadLocal<HibernateTemplate>();
    private final static ThreadLocal<Session> SESSION = new ThreadLocal<Session>();
    private final static ThreadLocal<Class<?>> LAST_TEST_CLASS = new ThreadLocal<Class<?>>();

    /**
     * Returns a data source. The returned data source is used in the default
     * implementation of {@link #annotationSessionFactoryBean()}.
     * <p>
     * The default implementation of this method returns a data
     * source for an in-memory HSQL database (with sid being <code>"test"</code> followed by the current thread's
     * hash code, with user name <code>sa</code> and no password).
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
     * Returns Hibernate properties. Returned properties are used in the
     * default implementation of {@link #annotationSessionFactoryBean()}.
     * <p>
     * The default implementation of this method returns following key-value pairs:
     * <dl>
     * <dt><code>hibernate.connection.autocommit</code></dt>
     * <dd><code>false<code></dd>
     * <dt><code>hibernate.hbm2ddl.auto</code></dt>
     * <dd>
     * <code>create-drop</code> (if {@link #dataSource()} returns a data source which has an <code>url</code> property
     * starting with <code>jdbc:hsqldb:mem:</code> or <code>jdbc:h2:mem:</code>)<br>
     * <code>validate</code> (otherwise)
     * </dd>
     * </dl>
     * <p>
     * Can be overridden in subclasses.
     *
     * @return Hibernate properties to be used during tests
     */
    protected Properties hibernateProperties() {
        Properties properties = new Properties();
        String url = detectUrl(dataSource());
        properties.setProperty(
                "hibernate.hbm2ddl.auto",
                url != null && (url.startsWith("jdbc:hsqldb:mem:") || url.startsWith("jdbc:h2:mem:"))
                        ? "create-drop" : "validate");
        properties.setProperty("hibernate.connection.autocommit", "false");
        return properties;
    }

    /**
     * Returns an {@link AnnotationSessionFactoryBean}. The returned factory
     * is used in the default implementation of {@link #sessionFactory()}.
     * <p>
     * The default implementation of this method
     * returns an <code>AnnotationSessionFactoryBean</code> initialized in the following manner.
     * <ol>
     * <li>The <code>dataSource</code> property is assigned the value returned by {@link #dataSource()}.</li>
     * <li>The <code>hibernateProperties</code> property is assigned the value returned by
     * {@link #hibernateProperties()}.</li>
     * <li>If {@link #annotatedClasses()} returns a <code>non-null</code> value than it is assigned to
     * <code>annotatedClasses</code> property.
     * Otherwise the <code>packagesToScan</code> property is assigned an one-element array containing the value
     * returned by {@link #packageWithAnnotatedClasses()}.</li>
     * <li>The
     * {@link AnnotationSessionFactoryBean#afterPropertiesSet afterPropertiesSet()}
     * is invoked.
     * </ol>
     * <p>
     * Can be overridden in subclasses.
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
            String pkg = packageWithAnnotatedClasses();
            if (pkg == null) {
                pkg = "";
            } else if (pkg.length() > 0 && !pkg.endsWith(".")) {
                pkg += ".";
            }
            sessionFactoryBean.setPackagesToScan(new String[]{pkg});
        }
        try {
            sessionFactoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return sessionFactoryBean;
    }

    /**
     * Returns an array of annotated classes to be used by Hibernate. The returned array is used by the default
     * implementation of {@link #annotationSessionFactoryBean()}.
     * <p>
     * The default implementation of this method returns <code>null</code>.
     * <p>
     * Can be overridden in subclasses.
     *
     * @return annotated classes to be used by Hibernate
     */
    protected Class[] annotatedClasses() {
        return null;
    }

    /**
     * Returns a package containing annotated classes to be processed by Hibernate.
     * <p>
     * The default implementation returns an empty string which means that all packages will be scanned.
     * <p>
     * The returned package is used in the default implementation
     * of {@link #annotationSessionFactoryBean()} as a search location for annotated classes. The default
     * implementation is suitable in most cases but also not optimal in most cases as annotated classes are probably
     * located in some specific package.
     *
     * @return package containing annotated classes
     */
    protected String packageWithAnnotatedClasses() {
        return "";
    }

    /**
     * Returns a session factory that will be used to create a Hibernate session before every JUnit or TestNG test.
     * <p>
     * The default implementation returns the session factory created by invoking
     * {@link #annotationSessionFactoryBean()}.{@link AnnotationSessionFactoryBean#getObject getObject()}.
     * <p>
     * Can be overridden in subclasses.
     *
     * @return session factory that will be used to create a Hibernate session before every JUnit or TestNG test
     */
    protected SessionFactory sessionFactory() {
        return (SessionFactory) annotationSessionFactoryBean().getObject();
    }

    /**
     * Begins a new transaction.
     * <p>
     * This method is idempotent - it will create only one transaction even if invoked more than once.
     */
    public void beginTransaction() {
        if (SESSION.get() != null) {
            SESSION.get().beginTransaction();
        }
    }

    /**
     * Rollbacks the current transaction.
     * <p>
     * This method can rollback the transaction started by this
     * rule. It can also rollback any transaction started manually through {@link #beginTransaction()}.
     */
    public void rollback() {
        if (SESSION.get() != null && SESSION.get().getTransaction().isActive()) {
            SESSION.get().getTransaction().rollback();
        }
    }

    /**
     * Commits the current transaction.
     * <p>
     * This method can commit the transaction started by this
     * rule. It can also commit any transaction started manually through {@link #beginTransaction()}.
     */
    public void commit() {
        if (SESSION.get() != null && SESSION.get().getTransaction().isActive()) {
            SESSION.get().getTransaction().commit();
        }
    }

    /**
     * Flashes the current Hibernate session.
     * <p>
     * Some Hibernate mapping errors can be detected only after a
     * flush, i.e. after actual database operations are invoked.
     */
    public void flush() {
        if (SESSION.get() != null) {
            SESSION.get().flush();
            SESSION.get().clear();
        }
    }

    /**
     * Closes underlying Hibernate session.
     * <p>
     * From now on it will be impossible to begin a new transaction within the current test.
     */
    public void close() {
        closeAndRemoveSession();
    }

    public void rollbackAndClose() {
        rollback();
        close();
    }

    public void commitAndClose() {
        commit();
        close();
    }

    public HibernateTemplate getHibernateTemplate() {
        return new ProxyHibernateTemplate(HIBERNATE_TEMPLATE);
    }

    public SessionFactory getSessionFactory() {
        return new ProxySessionFactory(FACTORY);
    }

    private String detectUrl(DataSource ds) {
        try {
            Method method = ds.getClass().getMethod("getUrl");
            if (method.getReturnType().equals(String.class)) {
                return (String) method.invoke(ds);
            } else {
                return null;
            }
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    private void createSession() {
        if (SESSION.get() == null) {
            ensureSessionFactoryInitialized();
            Session session = SessionFactoryUtils.getSession(FACTORY.get(), true);
            HibernateSpringTxTestRule.SESSION.set(session);
            TransactionSynchronizationManager.bindResource(FACTORY.get(), new SessionHolder(session));
        } else {
            throw new IllegalStateException("non-null session unexpected");
        }
    }

    private void detectAndHandleTestClassChangeForCurrentThread(Class<?> currentTest) {
        if (LAST_TEST_CLASS.get() != null && LAST_TEST_CLASS.get() != currentTest) {
            FACTORY.remove();
            HIBERNATE_TEMPLATE.remove();
        }
        LAST_TEST_CLASS.set(currentTest);
    }

    private void closeAndRemoveSession() {
        if (SESSION.get() != null) {
            TransactionSynchronizationManager.unbindResource(FACTORY.get());
            SESSION.get().close();
            SESSION.remove();
        }
    }

    private void ensureSessionFactoryInitialized() {
        if (FACTORY.get() == null) {
            FACTORY.set(sessionFactory());
            HIBERNATE_TEMPLATE.set(new HibernateTemplate(FACTORY.get()));
        }
    }

    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {
            public void evaluate() throws Throwable {
                try {
                    detectAndHandleTestClassChangeForCurrentThread(description.getTestClass());
                    createSession();
                    beginTransaction();
                    statement.evaluate();
                } finally {
                    rollback();
                    closeAndRemoveSession();
                }
            }
        };
    }
}
