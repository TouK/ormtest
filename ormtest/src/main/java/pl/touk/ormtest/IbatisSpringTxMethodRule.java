/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import com.google.common.base.Preconditions;
import com.ibatis.sqlmap.client.SqlMapClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class for JUnit 4.8+ testing of Ibatis mappings in projects that use Spring-based DAOs. This class uses H2 in-memory
 * database. By default it searches <i>sqlmap-config.xml</i> file on the classpath to configure Ibatis.
 * <br>
 * Tests using this class are very fast because they don't load spring application context although they can be
 * used to test spring DAOs.
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

    private final static String[] sqlMapConfigDefault = new String[]{"/sqlmap-config.xml"};
    private static volatile Object[] sqlMapConfig = sqlMapConfigDefault;

    // Guards assignment of sqlMapClient:
    private final static Object guard = new Object();
    private static volatile SqlMapClient sqlMapClient = null;

    private final static ConcurrentMap<Thread, SqlMapClientTemplate> sqlMapClientTemplates = new ConcurrentHashMap<Thread, SqlMapClientTemplate>();

    private final PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public static void setSqlMapConfig(Object... sqlMapConfigs) {
        sqlMapConfigs = Preconditions.checkNotNull(sqlMapConfigs, "sqlMapConfigs must not be null");
        Preconditions.checkArgument(sqlMapConfigs.length > 0, "sqlMapConfigs must not be empty");
        for (int i = 0; i < sqlMapConfigs.length; i++) {
            Preconditions.checkNotNull(sqlMapConfigs[i], "sqlMapConfigs[%s] is null", i);
            if (sqlMapConfigs[i] instanceof String) {
                Preconditions.checkArgument(!((String) sqlMapConfigs[i]).isEmpty(), "sqlMapConfigs[%s] is an empty string", i);
            } else if (sqlMapConfigs[i] instanceof List) {
                validatePathAndAncestorDirectory(sqlMapConfigs, i);
            } else {
                Preconditions.checkArgument(sqlMapConfigs[i] instanceof Resource, "sqlMapConfigs[%s] must be one of: String, Resource, List of two Strings", i);
            }
        }
        IbatisSpringTxMethodRule.sqlMapConfig = sqlMapConfigs;
    }

    private static void validatePathAndAncestorDirectory(Object[] sqlMapConfig, int index) {
        List pathAndAncestorDirectory = (List) sqlMapConfig[index];
        Preconditions.checkArgument(pathAndAncestorDirectory.size() == 2,
                "sqlMapConfig[%s] is a string list of invalid size (%s but should be 2)", index, pathAndAncestorDirectory.size());
        Preconditions.checkNotNull(pathAndAncestorDirectory.get(0),
                "sqlMapConfig[%s] is a two-element list with invalid first element (null)", index);
        Preconditions.checkArgument(pathAndAncestorDirectory.get(0) instanceof String,
                "sqlMapConfig[%s] is a two-element list with invalid first element (%s)", index, pathAndAncestorDirectory.get(0).getClass());
        Preconditions.checkArgument(!(pathAndAncestorDirectory.get(0).toString().isEmpty()),
                "sqlMapConfig[%s] is a two-element list with invalid first element (an empty string)", index);
        Preconditions.checkNotNull(pathAndAncestorDirectory.get(1),
                "sqlMapConfig[%s] is a two-element list with invalid second element (null)", index);
        Preconditions.checkArgument(pathAndAncestorDirectory.get(1) instanceof String,
                "sqlMapConfig[%s] is a two-element list with invalid second element (%s)", index, pathAndAncestorDirectory.get(1).getClass());
        Preconditions.checkArgument(!(pathAndAncestorDirectory.get(1).toString().isEmpty()),
                "sqlMapConfig[%s] is a two-element list with invalid second element (an empty string)", index);
    }

    public static Object[] getSqlMapConfig() {
        return sqlMapConfig;
    }

    /**
     * Constructs an IbatisSpringTxMethodRule that reads the Ibatis configuration from the default location
     * i.e. from the classpath resource "/sqlmap-config.xml".
     */
    public IbatisSpringTxMethodRule() {
    }

    /**
     * Constructs an IbatisSpringTxMethodRule that reads the Ibatis configuration from the given Resource.
     * 
     * @param sqlMapConfig a Resource containing Ibatis configuration
     */
    public IbatisSpringTxMethodRule(Resource sqlMapConfig) {
        setSqlMapConfig(sqlMapConfig);
    }

    /**
     * Constructs an IbatisSpringTxMethodRule that reads the Ibatis configuration from the given path. If the given
     * path is an Ant pattern (i.e. 
     * {@link org.springframework.util.AntPathMatcher#isPattern(String) AntPathMatcher.isPattern(String)}
     * returns true for this path)
     * then it is resolved by {@link PathMatchingResourcePatternResolver#getResources(String)}.
     * Otherwise it is resolved by {@link PathMatchingResourcePatternResolver#getResource(String)}.
     *
     * @param sqlMapConfigPath a path pointing to an Ibatis configuration
     */
    public IbatisSpringTxMethodRule(String sqlMapConfigPath) {
        setSqlMapConfig(sqlMapConfigPath);
    }

    /**
     * Constructs an IbatisSpringTxMethodRule that reads the Ibatis configuration from the given path
     * (<code>fileSystemSqlMapConfigPath</code>) that is a descendant of <code>ancestorDirectory</code>
     * and sets the H2 compatibility mode to the provided one.
     * <p>
     * If the given
     * path is an Ant pattern (i.e.
     * {@link org.springframework.util.AntPathMatcher#isPattern(String) AntPathMatcher.isPattern(String)}
     * returns true for this path)
     * then it is resolved by {@link PathMatchingResourcePatternResolver#getResources(String)}.
     * Otherwise it is resolved by {@link PathMatchingResourcePatternResolver#getResource(String)}.
     * After the above resolution only one resource should be a descendant of the given <code>ancestorDirectory</code>
     * (here "descendant" means that absolute path of {@link org.springframework.core.io.Resource#getFile()}
     * {@link String#contains(CharSequence) contains}
     * <code>ancestorDirectory</code>) and if this is the case it will be used as the Ibatis configuration.
     * Otherwise a <code>RuntimeException</code> is thrown.
     * </p>
     *
     * @param fileSystemSqlMapConfigPath a path pointing to an Ibatis configuration
     * @param ancestorDirectory file which is descendant of this directory will be used as Ibatis configuration
     * @param h2CompatibilityMode H2 compatibility mode to be used (for example "Oracle", "MySQL" etc.)
     */
    public IbatisSpringTxMethodRule(String fileSystemSqlMapConfigPath, String ancestorDirectory, String h2CompatibilityMode) {
        super(h2CompatibilityMode);
        setSqlMapConfig(Arrays.asList(fileSystemSqlMapConfigPath, ancestorDirectory));
    }

    /**
     * Constructs an IbatisSpringTxMethodRule that reads the Ibatis configuration from the given Resource and
     * sets the H2 compatibility mode to the provided one.
     *
     * @param sqlMapConfig a Resource containing Ibatis configuration
     * @param h2CompatibilityMode H2 compatibility mode to be used (for example "Oracle", "MySQL" etc.)
     */
    public IbatisSpringTxMethodRule(Resource sqlMapConfig, String h2CompatibilityMode) {
        super(h2CompatibilityMode);
        setSqlMapConfig(sqlMapConfig);
    }

    /**
     * Constructs an IbatisSpringTxMethodRule that reads the Ibatis configuration from the given path and
     * sets the H2 compatibility mode to the provided one. If the given
     * path is an Ant pattern (i.e.
     * {@link org.springframework.util.AntPathMatcher#isPattern(String) AntPathMatcher.isPattern(String)}
     * returns true for this path)
     * then it is resolved by {@link PathMatchingResourcePatternResolver#getResources(String)}.
     * Otherwise it is resolved by {@link PathMatchingResourcePatternResolver#getResource(String)}.
     *
     * @param sqlMapConfigPath a path pointing to an Ibatis configuration
     * @param h2CompatibilityMode H2 compatibility mode to be used (for example "Oracle", "MySQL" etc.)
     */
    public IbatisSpringTxMethodRule(String sqlMapConfigPath, String h2CompatibilityMode) {
        super(h2CompatibilityMode);
        setSqlMapConfig(sqlMapConfigPath);
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
        sqlMapClientFactoryBean.setConfigLocations(createSqlMapConfigResourceArray());
        try {
            sqlMapClientFactoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return sqlMapClientFactoryBean;
    }

    Resource[] createSqlMapConfigResourceArray() {
        Resource[] array = new Resource[sqlMapConfig.length];
        for (int i = 0; i < sqlMapConfig.length; i++) {
            array[i] = loadSqlMapConfig(sqlMapConfig[i]);
        }
        return array;
    }

    private Resource loadSqlMapConfig(Object sqlMapConfig) {
        if (sqlMapConfig instanceof String) {
            return loadSqlMapConfigFromPath(sqlMapConfig.toString(), null);
        } else if (sqlMapConfig instanceof Resource) {
            return (Resource) sqlMapConfig;
        } else if (sqlMapConfig instanceof List) {
            return loadSqlMapConfigFromPath(((List) sqlMapConfig).get(0).toString(), ((List) sqlMapConfig).get(1).toString());
        } else {
            throw new IllegalStateException("invalid sqlMapConfig: " + sqlMapConfig.getClass().getName());
        }
    }

    private Resource loadSqlMapConfigFromPath(String sqlMapConfig, String ancestorDirectory) {
        if (!resourcePatternResolver.getPathMatcher().isPattern(sqlMapConfig)) {
            Resource resource = resourcePatternResolver.getResource(sqlMapConfig);
            validateResource(sqlMapConfig, ancestorDirectory, resource);
            return resource;
        } else {
            try {
                Resource[] resources = resourcePatternResolver.getResources(sqlMapConfig);
                switch (resources.length) {
                    case 0: throw new RuntimeException("can't find resource: " + sqlMapConfig);
                    default: return validateResource(sqlMapConfig, ancestorDirectory, resources);
                }
            } catch (IOException e) {
                throw new RuntimeException("can't find resource: " + sqlMapConfig);
            }
        }
    }

    private Resource validateResource(String sqlMapConfig, String ancestorDirectory, Resource... resources) {
        Preconditions.checkArgument(resources.length > 0);
        if (ancestorDirectory != null) {
            Resource descendantResource = null;
            for (Resource resource : resources) {
                try {
                    String resourcePath = resource.getFile().getAbsolutePath();
                    if (resourcePath.contains(ancestorDirectory)) {
                        if (descendantResource != null) {
                            throw new RuntimeException("'" + sqlMapConfig + "' resolved to at least two resources containing '"
                                    + ancestorDirectory + "': " + descendantResource.getFile().getAbsolutePath() + ", " + resourcePath);
                        } else {
                            descendantResource = resource;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (descendantResource != null) {
                return descendantResource;
            } else {
                throw new RuntimeException("no descendant of '" + ancestorDirectory + "' among resources: " + Arrays.toString(resources));
            }
        } else {
            if (resources.length == 1) {
                return resources[0];
            } else {
                throw new RuntimeException("more than one sqlMapConfig resource found by the given name: " + sqlMapConfig);
            }
        }
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
        // In Spring 2.5 return type of sqlMapClientFactoryBean().getObject() is Object. In Spring 3.0, 3.1 and 3.2 -
        // SqlMapClient. So if we compiled OrmTest against Spring 3.0, 3.1 or 3.2 then Spring 2.5 couldn't be used at
        // runtime because of the exception:
        // java.lang.NoSuchMethodError:
        // org.springframework.orm.ibatis.SqlMapClientFactoryBean.getObject()Lcom/ibatis/sqlmap/client/SqlMapClient.
        // That's why Spring 2.5 is used during compilation.
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
        resetThreadsForCurrentTestClass(true);
    }

    protected static void resetThreadsForCurrentTestClass(boolean hardReset) {
        synchronized (guard) {
            sqlMapClient = null;
        }
        if (hardReset) {
            sqlMapConfig = sqlMapConfigDefault;
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
