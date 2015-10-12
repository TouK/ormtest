/*
 * Copyright (c) 2011 TouK
 * All rights reserved
 */
package pl.touk.ormtesttest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import pl.touk.ormtest.HibernateSpringTxTestRule;


/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class HibernateSpringTxTestRuleTest {

    @Rule
    public HibernateSpringTxTestRule txContext = createHibernateSpringTxTestRule();

    protected HibernateSpringTxTestRule createHibernateSpringTxTestRule() {
        // This maven module (ormtesttest) is used by modules spring2_0_0, spring2_5_6 etc. as a jar on a classpath
        // during tests. This module contains hibernate entity class (ExampleEntity) so the jar also contains it.
        // Searching entity classes through package scanning mechanism (by default HibernateSpringTxTestRule uses
        // this mechanism) doesn't work in case of jars (at least not always; for instance see
        // http://www.carbonrider.com/2011/02/27/spring-hibernate-annotation-entity-classes-in-jar-not-recognised-xxx-is-not-mapped/).
        // Hence explicitly list entity classes:
        return new HibernateSpringTxTestRuleWithExampleEntity();
    }

    @Before
    public void before() {
        txContext.getHibernateTemplate().persist(new ExampleEntity(2, "nameInBefore"));
    }

    @After
    public void after() {
        txContext.getHibernateTemplate().persist(new ExampleEntity(3, "nameInAfter"));
    }

    @Test
    public void shoudPersistAndLoadEntityInTransactionInJunit1() throws Exception {
        txContext.getHibernateTemplate().persist(new ExampleEntity(1, "name"));

        txContext.flush();

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getHibernateTemplate().load(ExampleEntity.class, 1);

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(Integer.valueOf(1), exampleEntity.getId());
        Assert.assertEquals("name", exampleEntity.getName());
    }

    @Test
    public void shoudPersistAndLoadEntityInTransactionInJunit2() throws Exception {
        txContext.getHibernateTemplate().persist(new ExampleEntity(1, "name"));

        txContext.flush();

        // Casting below is redundant in spring 3.0.5.RELEASE (and maybe in some earlier releases) but needed in spring 2.5.6:
        ExampleEntity exampleEntity = (ExampleEntity) txContext.getHibernateTemplate().load(ExampleEntity.class, 1);

        Assert.assertNotNull(exampleEntity);
        Assert.assertEquals(Integer.valueOf(1), exampleEntity.getId());
        Assert.assertEquals("name", exampleEntity.getName());
    }
}