/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for JUnit 4.8+ testing of Spring-based Jdbc DAOs. This class uses H2 in-memory
 * database.
 * <br>
 * Tests using this class are very fast because they don't load spring application context although they can be
 * used to test spring DAOs!
 * <br>
 * This class is very simple to use. An example is presented below. Although the example doesn't use
 * spring DAOs it would be very similar if it did. The spring Jdbc DAOs, which extend Spring's
 * <i>JdbcDaoSupport</i>, need a <i>JdbcTemplate</i> which is in fact referenced in the example below
 * (bold fragment in method <i>before</i>).
 * <pre><code>
 *
 * public class TransactionalTest {
 *
 *   <b>&#64;Rule
 *   public JdbcSpringTxMethodRule txContext = new JdbcSpringTxMethodRule();</b>
 *
 *   &#64;Before
 *   public void before() {
 *     // Prepare environment for every test in this class (transaction (new for every test) has already been open):
 *     SimpleJdbcTestUtils.executeSqlScript(
 *         new SimpleJdbcTemplate(<b>txContext.getJdbcTemplate()</b>),
 *         new ClassPathResource("test.sql"),
 *         true);
 *     txContext.getJdbcTemplate().execute(
 *         "INSERT INTO EXAMPLEENTITIES (name) VALUES ('" + firstExampleEntity.getName() + "')");
 *     firstExampleEntity.setId(txContext.getJdbcTemplate().queryForInt("SELECT LAST_INSERT_ID()"));
 *   }
 *
 *   &#64;After
 *   public void after() {
 *     // Clean-up after every test in this class. Transaction for the
 *     // last executed test has not yet been closed if it is needed:
 *     txContext.getJdbcTemplate().execute(
 *         "INSERT INTO EXAMPLEENTITIES (id, name) VALUES (1, 'some other name')"));
 *   }
 *
 *   &#64;Test
 *   public void shoudPersistEntityA() throws Exception {
 *     txContext.getJdbcTemplate().execute(
 *         "INSERT INTO EXAMPLEENTITIES (id, name) VALUES (2, 'some other name')"));
 *   }
 *
 *   &#64;Test
 *   public void shoudPersistEntityB() throws Exception {
 *     txContext.getJdbcTemplate().execute(
 *         "INSERT INTO EXAMPLEENTITIES (id, name) VALUES (2, 'some other name')"));
 *   }
 * }
 * </code></pre>
 *
 * In above example, if the two tests are executed in parallel then each of them will be executed on different,
 * completely independent in-memory H2 databases.
 *
 * Of course an <i>ExampleEntity</i> plain old java bean (POJO) with <i>id</i> and <i>name</i>
 * properties would be needed for the above example to work.
 *
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class JdbcSpringTxMethodRule extends SpringTxMethodRule {

    private final static ConcurrentMap<Thread, JdbcTemplate> jdbcTemplates = new ConcurrentHashMap<Thread, JdbcTemplate>();

    public JdbcTemplate getJdbcTemplate() {
        ensureTemplateInitialized();
        return jdbcTemplates.get(Thread.currentThread());
    }

    protected void ensureTemplateInitialized() {
        if (jdbcTemplates.get(Thread.currentThread()) == null) {
            JdbcTemplate template = new JdbcTemplate(dataSource());
            jdbcTemplates.put(Thread.currentThread(), template);
            txManagers.put(Thread.currentThread(), new DataSourceTransactionManager(template.getDataSource()));
        }
    }

    public static void resetThreadsForCurrentTestClass() {
        Set<Thread> threads = getThreads(findInvokingTestClass());
        if (threads != null && threads.size() > 0) {
            for (Thread t: threads) {
                jdbcTemplates.remove(t);
                txManagers.remove(t);
                txStatuses.remove(t);
            }
        }
    }
}
