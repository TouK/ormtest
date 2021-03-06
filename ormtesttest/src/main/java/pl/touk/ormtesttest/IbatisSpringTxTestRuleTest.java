/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtesttest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import pl.touk.ormtest.IbatisSpringTxTestRule;

import java.sql.SQLException;
import java.util.List;

/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class IbatisSpringTxTestRuleTest {

    private ExampleEntity firstExampleEntity = null;

    @Rule
    public IbatisSpringTxTestRule txContext = createIbatisSpringTxTestRule();

    protected IbatisSpringTxTestRule createIbatisSpringTxTestRule() {
        return new IbatisSpringTxTestRule();
    }

    @Before
    public void before() throws SQLException {
        SimpleJdbcTestUtils.executeSqlScript(
                new SimpleJdbcTemplate(txContext.getSqlMapClientTemplate().getDataSource()),
                new ClassPathResource("test.sql"), true);
        txContext.commitTransactionAndBeginNewOne();
        firstExampleEntity = new ExampleEntity(0, "nameInBefore");
        txContext.getSqlMapClientTemplate().insert("insert", firstExampleEntity);
    }

    @After
    public void after() {
        txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(0, "nameInAfter"));
    }

    @AfterClass
    public static void afterClass() {
        // This class interferes with class MysqlIbatisSpringTxTestRuleTest: if tests from this class are run first
        // then threads used during these tests can be reused to run tests in MysqlIbatisSpringTxTestRuleTest. The
        // other class has different database (data source) so we must clean any thread specific data (for example data
        // source) created by threads running this class.
        IbatisSpringTxTestRule.resetThreadsForCurrentTestClass();
    }

    @Test
    public void shoudPersistAndLoadEntityInTransactionInJunit1() throws Exception {
        txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(0, "name"));

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getSqlMapClientTemplate().queryForObject("select", firstExampleEntity.getId());

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(firstExampleEntity.getId(), exampleEntity.getId());
        Assert.assertEquals(firstExampleEntity.getName(), exampleEntity.getName());

        txContext.rollBackTransactionAndBeginNewOne();

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        exampleEntity = (ExampleEntity) txContext.getSqlMapClientTemplate().queryForObject("select", Integer.valueOf(1));
        Assert.assertNull(exampleEntity);

        txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(0, "nameInTest1"));

        txContext.commitTransactionAndBeginNewOne();

        txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(0, "nameInTest2"));
        List list = txContext.getSqlMapClientTemplate().queryForList("selectAll");
        Assert.assertEquals(2, list.size());

        txContext.rollBackTransactionAndBeginNewOne();
        list = txContext.getSqlMapClientTemplate().queryForList("selectAll");
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void shoudPersistAndLoadEntityInTransactionInJunit2() throws Exception {
        txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(1, "name"));

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getSqlMapClientTemplate().queryForObject("select", firstExampleEntity.getId());

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(firstExampleEntity.getId(), exampleEntity.getId());
        Assert.assertEquals(firstExampleEntity.getName(), exampleEntity.getName());
    }
}