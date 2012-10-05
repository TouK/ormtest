/*
 * Copyright (c) 2011 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import org.junit.*;


/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
public class HibernateSpringTxMethodRuleTest {

    @Rule
    public HibernateSpringTxMethodRule txContext = new HibernateSpringTxMethodRule();

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