/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtesttest;

import pl.touk.ormtest.IbatisSpringTxMethodRule;

/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class CustomSqlMapConfigIbatisSpringTxMethodRuleTest extends IbatisSpringTxMethodRuleTest {
    @Override
    protected IbatisSpringTxMethodRule createIbatisSpringTxMethodRule() {
        return new IbatisSpringTxMethodRule("classpath*:sqlmap-config.xml");
    }
}