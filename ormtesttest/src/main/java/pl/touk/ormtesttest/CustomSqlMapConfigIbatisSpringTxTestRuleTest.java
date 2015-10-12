/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtesttest;

import pl.touk.ormtest.IbatisSpringTxTestRule;

/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class CustomSqlMapConfigIbatisSpringTxTestRuleTest extends IbatisSpringTxTestRuleTest {
    @Override
    protected IbatisSpringTxTestRule createIbatisSpringTxTestRule() {
        return new IbatisSpringTxTestRule("classpath*:sqlmap-config.xml");
    }
}