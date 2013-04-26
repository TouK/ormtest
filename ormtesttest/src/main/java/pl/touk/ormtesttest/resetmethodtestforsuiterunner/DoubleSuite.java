package pl.touk.ormtesttest.resetmethodtestforsuiterunner;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pl.touk.ormtest.MysqlIbatisSpringTxMethodRule;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        Test1.class,
        Test2.class
})
public class DoubleSuite {

    @BeforeClass
    public static void beforeClass() {
        MysqlIbatisSpringTxMethodRule.setSchemasAndInitScripts(
                "schema1", "resetmethodtestforsuiterunner/mysql-init1.sql",
                "schema2", "resetmethodtestforsuiterunner/mysql-init2.sql");
        MysqlIbatisSpringTxMethodRule.setSqlMapConfig(
                "resetmethodtestforsuiterunner/sqlmap-config1.xml",
                "resetmethodtestforsuiterunner/sqlmap-config2.xml");
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        MysqlIbatisSpringTxMethodRule.stopMysql();
        MysqlIbatisSpringTxMethodRule.resetThreadsForCurrentTestClass();
    }
}
