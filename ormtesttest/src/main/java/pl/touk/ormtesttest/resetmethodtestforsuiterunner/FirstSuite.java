package pl.touk.ormtesttest.resetmethodtestforsuiterunner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import pl.touk.ormtest.MysqlIbatisSpringTxMethodRule;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Test1.class
})
public class FirstSuite {

    @BeforeClass
    public static void beforeClass() {
        MysqlIbatisSpringTxMethodRule.setSchema("schema1");
        MysqlIbatisSpringTxMethodRule.setInitScript("resetmethodtestforsuiterunner/mysql-init1.sql");
        MysqlIbatisSpringTxMethodRule.setSqlMapConfig("resetmethodtestforsuiterunner/sqlmap-config1.xml");
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        MysqlIbatisSpringTxMethodRule.stopMysql();
        MysqlIbatisSpringTxMethodRule.resetThreadsForCurrentTestClass();
    }
}
