package pl.touk.ormtesttest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import pl.touk.ormtesttest.resetmethodtestforsuiterunner.FirstSuite;
import pl.touk.ormtesttest.resetmethodtestforsuiterunner.SecondSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        FirstSuite.class,
        SecondSuite.class,
        HibernateSpringTxTestRuleTest.class,
        IbatisSpringTxTestRuleTest.class,
        JdbcSpringTxTestRuleTest.class,
        MysqlIbatisSpringTxTestRuleTest.class
})
public class AllTests {
}
