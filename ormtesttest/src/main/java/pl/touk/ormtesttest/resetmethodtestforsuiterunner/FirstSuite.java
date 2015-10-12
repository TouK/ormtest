package pl.touk.ormtesttest.resetmethodtestforsuiterunner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import pl.touk.ormtest.MysqlIbatisSpringTxTestRule;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Test1.class
})
public class FirstSuite {

    @BeforeClass
    public static void beforeClass() {
        MysqlIbatisSpringTxTestRule.setSchema("schema1");
        MysqlIbatisSpringTxTestRule.setInitScript("resetmethodtestforsuiterunner/mysql-init1.sql");
        MysqlIbatisSpringTxTestRule.setSqlMapConfig("resetmethodtestforsuiterunner/sqlmap-config1.xml");
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        MysqlIbatisSpringTxTestRule.stopMysql();
        MysqlIbatisSpringTxTestRule.resetThreadsForCurrentTestClass();
    }
}
