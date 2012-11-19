/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import org.junit.*;

import java.util.List;

/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class MysqlIbatisSpringTxMethodRuleTest {

    private ExampleEntity firstExampleEntity = null;

    @Rule
    public MysqlIbatisSpringTxMethodRule txContext = new MysqlIbatisSpringTxMethodRule();

    @Before
    public void before() {
        // The following isn't needed as mysql-init.sql is executed by default when MysqlIbatisSpringTxMethodRuleTest is used:
        // SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(txContext.getSqlMapClientTemplate().getDataSource()), new ClassPathResource("mysql-init.sql"), true);
        firstExampleEntity = new ExampleEntity(0, "nameInBefore");
        txContext.getSqlMapClientTemplate().insert("insert", firstExampleEntity);
    }

    @After
    public void after() {
        txContext.getSqlMapClientTemplate().insert("insert", new ExampleEntity(0, "nameInAfter"));
    }

    @AfterClass
    public static void afterClass() {
        // This class interferes with class IbatisSpringTxMethodRuleTest: if tests from this class are run first
        // then threads used during these tests can be reused to run tests in IbatisSpringTxMethodRuleTest. The other
        // class has different database (data source) so we must clean any thread specific data (for example data
        // source) created by threads running this class.
        MysqlIbatisSpringTxMethodRule.resetThreadsForCurrentTestClass();
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