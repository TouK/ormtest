package pl.touk.ormtesttest;

import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import pl.touk.ormtest.MysqlIbatisSpringTxMethodRule;
import pl.touk.ormtesttest.resetmethodtestforsuiterunner.DoubleSuite;
import pl.touk.ormtesttest.resetmethodtestforsuiterunner.FirstSuite;
import pl.touk.ormtesttest.resetmethodtestforsuiterunner.SecondSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        DoubleSuite.class,
        FirstSuite.class,
        SecondSuite.class,
        HibernateSpringTxMethodRuleTest.class,
        IbatisSpringTxMethodRuleTest.class,
        CustomSqlMapConfigIbatisSpringTxMethodRuleTest.class,
        JdbcSpringTxMethodRuleTest.class,
        MysqlIbatisSpringTxMethodRuleTest.class
})
public class Test {
    @AfterClass
    public static void afterClass() {
        MysqlIbatisSpringTxMethodRule.stopMysql();
    }
}
