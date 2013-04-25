package pl.touk.ormtesttest.resetmethodtestforsuiterunner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import pl.touk.ormtest.MysqlIbatisSpringTxMethodRule;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Test2.class
})
public class SecondSuite {

    @BeforeClass
    public static void beforeClass() {
        MysqlIbatisSpringTxMethodRule.setSchema("schema2");
        MysqlIbatisSpringTxMethodRule.setInitScript("resetmethodtestforsuiterunner/mysql-init2.sql");
        MysqlIbatisSpringTxMethodRule.setSqlMapConfig("resetmethodtestforsuiterunner/sqlmap-config2.xml");
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        MysqlIbatisSpringTxMethodRule.stopMysql();
        MysqlIbatisSpringTxMethodRule.resetThreadsForCurrentTestClass();
    }
}
