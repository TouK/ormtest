package pl.touk.ormtesttest.resetmethodtestforsuiterunner;

/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import pl.touk.ormtest.MysqlIbatisSpringTxTestRule;
import pl.touk.ormtesttest.ExampleEntity;

import java.util.List;

/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class Test1 {

    private ExampleEntity firstExampleEntity = null;

    @Rule
    public MysqlIbatisSpringTxTestRule txContext = new MysqlIbatisSpringTxTestRule();

    @Before
    public void before() {
        // The following isn't needed as mysql-init.sql is executed by default when MysqlIbatisSpringTxTestRuleTest is used:
        // SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(txContext.getSqlMapClientTemplate().getDataSource()), new ClassPathResource("mysql-init.sql"), true);
        firstExampleEntity = new ExampleEntity(0, "nameInBefore");
        txContext.getSqlMapClientTemplate().insert("insert1", firstExampleEntity);
    }

    @Test
    public void should_persist_and_load_entity_in_transaction_in_junit1() throws Exception {
        txContext.getSqlMapClientTemplate().insert("insert1", new ExampleEntity(0, "name"));

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getSqlMapClientTemplate().queryForObject("select1", firstExampleEntity.getId());

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(firstExampleEntity.getId(), exampleEntity.getId());
        Assert.assertEquals(firstExampleEntity.getName(), exampleEntity.getName());

        txContext.rollBackTransactionAndBeginNewOne();

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        exampleEntity = (ExampleEntity) txContext.getSqlMapClientTemplate().queryForObject("select1", Integer.valueOf(1));
        Assert.assertNull(exampleEntity);

        txContext.getSqlMapClientTemplate().insert("insert1", new ExampleEntity(0, "nameInTest1"));

        txContext.commitTransactionAndBeginNewOne();

        txContext.getSqlMapClientTemplate().insert("insert1", new ExampleEntity(0, "nameInTest2"));
        List list = txContext.getSqlMapClientTemplate().queryForList("selectAll1");
        Assert.assertEquals(2, list.size());

        txContext.rollBackTransactionAndBeginNewOne();
        list = txContext.getSqlMapClientTemplate().queryForList("selectAll1");
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void should_persist_and_load_entity_in_transaction_in_junit2() throws Exception {
        txContext.getSqlMapClientTemplate().insert("insert1", new ExampleEntity(1, "name"));

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getSqlMapClientTemplate().queryForObject("select1", firstExampleEntity.getId());

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(firstExampleEntity.getId(), exampleEntity.getId());
        Assert.assertEquals(firstExampleEntity.getName(), exampleEntity.getName());
    }
}
