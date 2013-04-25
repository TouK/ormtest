/*
 * Copyright (c) 2011 TouK
 * All rights reserved
 */
package pl.touk.ormtesttest.test;

import pl.touk.ormtest.HibernateSpringTxMethodRule;
import pl.touk.ormtesttest.HibernateSpringTxMethodRuleTest;


/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class HibernateSpringTxMethodRuleWithNullPackageToScanTest extends HibernateSpringTxMethodRuleTest {

    @Override
    protected HibernateSpringTxMethodRule createHibernateSpringTxMethodRule() {
        return new HibernateSpringTxMethodRule() {
            @Override
            protected String packageWithAnnotatedClasses() {
                return null;
            }
        };
    }
}