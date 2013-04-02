/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import com.ibatis.sqlmap.client.SqlMapClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for JUnit 4.8+ testing of Ibatis mappings in projects that use Spring-based DAOs. This class uses H2 in-memory
 * database. By default it searches <i>sqlmap-config.xml</i> file on the classpath to configure Ibatis.
 * <br>
 * Tests using this class are very fast because they don't load spring application context although they can be
 * used to test spring DAOs!
 * <br>
 * This class is very simple to use. An example is presented below. Although the example doesn't use
 * spring DAOs it would be very similar if it did. The spring DAOs, which extend Spring's
 * <i>SqlMapClientDaoSupport</i>, need an <i>SqlMapClientTemplate</i> which is in fact referenced in the example below
 * (bold fragment in method <i>before</i>).
 * <pre><code>
 *
 * public class TransactionalTest {
 *
 *   <b>&#64;Rule
 *   public IbatisSpringTxMethodRule txContext = new IbatisSpringTxMethodRule();</b>
 *
 *   &#64;Before
 *   public void before() {
 *     // Prepare environment for every test in this class (transaction (new for every test) has already been open):
 *     SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(<b>txContext.getSqlMapClientTemplate()</b>.getDataSource()),
 *                                          new ClassPathResource("some-script-creating-database.sql"),
 *                                          false);
 *     <b>txContext</b>.getSqlMapClientTemplate().insert("insert", new ExampleEntity(1, "some name"));
 *   }
 *
 *   &#64;After
 *   public void after() {
 *     // Clean-up after every test in this class. Transaction for the last executed test has not yet been closed if it is needed:
 *     <b>txContext.getSqlMapClientTemplate()</b>.insert("insert", new ExampleEntity(1, "some other name"));
 *   }
 *
 *   &#64;Test
 *   public void shoudPersistEntityA() throws Exception {
 *     <b>txContext.getSqlMapClientTemplate()</b>.insert("insert", new ExampleEntity(2, "name"));
 *   }
 *
 *   &#64;Test
 *   public void shoudPersistEntityB() throws Exception {
 *     <b>txContext.getSqlMapClientTemplate()</b>.insert("insert", new ExampleEntity(2, "name"));
 *   }
 * }
 * </code></pre>
 *
 * In above example, if the two tests are executed in parallel then each of them will be executed on different,
 * completely independent in-memory H2 databases. For the above example to work a file <i>sqlmap-config.xml</i>
 * must be on the classpath. This file can look for example like this:
 *
 * <code><pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!DOCTYPE sqlMapConfig PUBLIC "-//iBATIS.com//DTD SQL Map Config 2.0//EN"
 *   "http://www.ibatis.com/dtd/sql-map-config-2.dtd"&gt;
 * &lt;sqlMapConfig&gt;
 *   &lt;sqlMap resource="example-entity.xml"/&gt;
 * &lt;/sqlMapConfig&gt;
 * </pre></code>
 * 
 * The above sqlmap configuration references one sql map file,
 * <i>example-entity.xml</i>, which can look for example like this:
 *
 * <code><pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!DOCTYPE sqlMap PUBLIC "-//iBATIS.com//DTD SQL Map 2.0//EN" "http://www.ibatis.com/dtd/sql-map-2.dtd"&gt;
 * &lt;sqlMap namespace="exampleEntity"&gt;
 *
 *   &lt;resultMap class="pl.touk.ormtest.ExampleEntity" id="exampleEntityResult"&gt;
 *     &lt;result property="id" column="id" /&gt;
 *     &lt;result property="name" column="name" /&gt;
 *   &lt;/resultMap&gt;
 *
 *   &lt;select id="selectAll" resultMap="exampleEntity.exampleEntityResult"&gt;
 *     SELECT * FROM EXAMPLEENTITIES
 *   &lt;/select&gt;
 *
 *   &lt;select id="select" resultMap="exampleEntity.exampleEntityResult"&gt;
 *     SELECT * FROM EXAMPLEENTITIES WHERE id = #id#
 *   &lt;/select&gt;
 *
 *   &lt;insert id="insert" parameterClass="pl.touk.ormtest.ExampleEntity"&gt;
 *     INSERT INTO EXAMPLEENTITIES (name) VALUES (#name#)
 *     &lt;selectKey keyProperty="id" resultClass="int"&gt;
 *       SELECT LAST_INSERT_ID();
 *     &lt;/selectKey&gt;
 *   &lt;/insert&gt;
 * &lt;/sqlMap&gt;
 * </pre></code>
 *
 * And of course an <i>ExampleEntity</i> plain old java bean (POJO) with <i>id</i> and <i>name</i>
 * properties would be needed for the above example to work.
 *
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class IbatisSpringTxMethodRule extends SpringTxMethodRule {

    private static volatile String sqlMapConfig = "/sqlmap-config.xml";

    // Guards assignment of sqlMapClient:
    private static final Object guard = new Object();
    private static volatile SqlMapClient sqlMapClient = null;

    private static ConcurrentMap<Thread, SqlMapClientTemplate> sqlMapClientTemplates = new ConcurrentHashMap<Thread, SqlMapClientTemplate>();

    public static void setSqlMapConfig(String sqlMapConfig) {
        IbatisSpringTxMethodRule.sqlMapConfig = sqlMapConfig;
    }

    public static String getSqlMapConfig() {
        return sqlMapConfig;
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
     * {@link #sqlMapClientFactoryBean()}.{@link org.springframework.orm.ibatis.SqlMapClientFactoryBean#getObject getObject()}.
     *
     * @return sqlMapClient that will be used during tests
     */
    protected SqlMapClient sqlMapClient() {
        return (SqlMapClient) sqlMapClientFactoryBean().getObject();
    }

    public SqlMapClientTemplate getSqlMapClientTemplate() {
        ensureTemplateInitialized();
        return sqlMapClientTemplates.get(Thread.currentThread());
    }

    protected void ensureTemplateInitialized() {
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

    public static void resetThreadsForCurrentTestClass() {
        synchronized (guard) {
            sqlMapClient = null;
        }
        Set<Thread> threads = getThreads(findInvokingTestClass());
        if (threads != null && threads.size() > 0) {
            for (Thread t: threads) {
                sqlMapClientTemplates.remove(t);
                txManagers.remove(t);
                txStatuses.remove(t);
            }
        }
    }
}
