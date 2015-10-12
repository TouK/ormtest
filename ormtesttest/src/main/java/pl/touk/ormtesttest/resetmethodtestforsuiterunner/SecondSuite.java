package pl.touk.ormtesttest.resetmethodtestforsuiterunner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import pl.touk.ormtest.MysqlIbatisSpringTxTestRule;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Test2.class
})
public class SecondSuite {

    @BeforeClass
    public static void beforeClass() {
        MysqlIbatisSpringTxTestRule.setSchema("schema2");
        MysqlIbatisSpringTxTestRule.setInitScript("resetmethodtestforsuiterunner/mysql-init2.sql");
        MysqlIbatisSpringTxTestRule.setSqlMapConfig("resetmethodtestforsuiterunner/sqlmap-config2.xml");
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        MysqlIbatisSpringTxTestRule.stopMysql();
        MysqlIbatisSpringTxTestRule.resetThreadsForCurrentTestClass();
    }
}
