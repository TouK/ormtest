package pl.touk.ormtesttest;

import pl.touk.ormtest.HibernateSpringTxTestRule;

public class HibernateSpringTxTestRuleWithExampleEntity extends HibernateSpringTxTestRule {
    @Override
    protected Class[] annotatedClasses() {
        return new Class[]{ExampleEntity.class};
    }
}
