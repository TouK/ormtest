/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtesttest;

import org.junit.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import pl.touk.ormtest.JdbcSpringTxMethodRule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class JdbcSpringTxMethodRuleTest {

    private ExampleEntity firstExampleEntity = null;
    private ExampleEntityRowMapper rowMapper = new ExampleEntityRowMapper();

    @Rule
    public JdbcSpringTxMethodRule txContext = new JdbcSpringTxMethodRule();

    @Before                            
    public void before() throws SQLException {
        SimpleJdbcTestUtils.executeSqlScript(
                new SimpleJdbcTemplate(txContext.getJdbcTemplate()),
                new ClassPathResource("test.sql"),
                true);
        txContext.commitTransactionAndBeginNewOne();
        firstExampleEntity = new ExampleEntity(0, "nameInBefore");
        txContext.getJdbcTemplate().execute("INSERT INTO EXAMPLEENTITIES (name) VALUES ('" + firstExampleEntity.getName() + "')");
        firstExampleEntity.setId(txContext.getJdbcTemplate().queryForInt("SELECT LAST_INSERT_ID()"));
    }

    @After
    public void after() {
        txContext.getJdbcTemplate().execute("INSERT INTO EXAMPLEENTITIES (name) VALUES ('nameInAfter')");
    }

    @AfterClass
    public static void afterClass() {
        // This class interferes with class MysqlIbatisSpringTxMethodRuleTest: if tests from this class are run first
        // then threads used during these tests can be reused to run tests in MysqlIbatisSpringTxMethodRuleTest. The
        // other class has different database (data source) so we must clean any thread specific data (for example data
        // source) created by threads running this class.
        JdbcSpringTxMethodRule.resetThreadsForCurrentTestClass();
    }

    private class ExampleEntityRowMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            return new ExampleEntity(resultSet.getInt("id"), resultSet.getString("name"));
        }
    }

    @Test
    public void shoudPersistAndLoadEntityInTransactionInJunit1() throws Exception {
        txContext.getJdbcTemplate().execute("INSERT INTO EXAMPLEENTITIES (name) VALUES ('name')");

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getJdbcTemplate().queryForObject(
                "SELECT * FROM EXAMPLEENTITIES WHERE id = " + firstExampleEntity.getId(),
                rowMapper
        );

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(firstExampleEntity.getId(), exampleEntity.getId());
        Assert.assertEquals(firstExampleEntity.getName(), exampleEntity.getName());

        txContext.rollBackTransactionAndBeginNewOne();

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        List list = txContext.getJdbcTemplate().query("SELECT * FROM EXAMPLEENTITIES WHERE id = 1", rowMapper);
        Assert.assertEquals(0, list.size());

        txContext.getJdbcTemplate().execute("INSERT INTO EXAMPLEENTITIES (name) VALUES ('nameInTest1')");

        txContext.commitTransactionAndBeginNewOne();

        txContext.getJdbcTemplate().execute("INSERT INTO EXAMPLEENTITIES (name) VALUES ('nameInTest2')");
        list = txContext.getJdbcTemplate().query("SELECT * FROM EXAMPLEENTITIES", rowMapper);
        Assert.assertEquals(2, list.size());

        txContext.rollBackTransactionAndBeginNewOne();
        list = txContext.getJdbcTemplate().query("SELECT * FROM EXAMPLEENTITIES", rowMapper);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void shoudPersistAndLoadEntityInTransactionInJunit2() throws Exception {
        txContext.getJdbcTemplate().execute("INSERT INTO EXAMPLEENTITIES (name) VALUES ('name')");

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getJdbcTemplate().queryForObject(
                "SELECT * FROM EXAMPLEENTITIES WHERE id = " + firstExampleEntity.getId(), rowMapper);

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(firstExampleEntity.getId(), exampleEntity.getId());
        Assert.assertEquals(firstExampleEntity.getName(), exampleEntity.getName());
    }
}