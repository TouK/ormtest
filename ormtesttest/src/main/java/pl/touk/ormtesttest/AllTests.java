package pl.touk.ormtesttest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import pl.touk.ormtesttest.resetmethodtestforsuiterunner.FirstSuite;
import pl.touk.ormtesttest.resetmethodtestforsuiterunner.SecondSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        FirstSuite.class,
        SecondSuite.class,
        HibernateSpringTxMethodRuleTest.class,
        IbatisSpringTxMethodRuleTest.class,
        JdbcSpringTxMethodRuleTest.class,
        MysqlIbatisSpringTxMethodRuleTest.class
})
public class AllTests {
}
