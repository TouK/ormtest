/*
 * Copyright (c) 2011 TouK
 * All rights reserved
 */
package pl.touk.ormtesttest.test;

import pl.touk.ormtest.HibernateSpringTxTestRule;
import pl.touk.ormtesttest.HibernateSpringTxTestRuleTest;


/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class HibernateSpringTxTestRuleWithEmptyPackageToScanTest extends HibernateSpringTxTestRuleTest {

    @Override
    protected HibernateSpringTxTestRule createHibernateSpringTxTestRule() {
        return new HibernateSpringTxTestRule() {
            @Override
            protected String packageWithAnnotatedClasses() {
                return "";
            }
        };
    }
}