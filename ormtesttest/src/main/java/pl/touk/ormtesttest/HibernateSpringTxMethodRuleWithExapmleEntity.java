package pl.touk.ormtesttest;

import pl.touk.ormtest.HibernateSpringTxMethodRule;

public class HibernateSpringTxMethodRuleWithExapmleEntity extends HibernateSpringTxMethodRule {
    @Override
    protected Class[] annotatedClasses() {
        return new Class[]{ExampleEntity.class};
    }
}
