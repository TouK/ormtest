/*
 * Copyright (c) 2011 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;

/**
 * @author <a href="mailto:msk@touk.pl">Michał Sokołowski</a>
 */
@Entity
public class ExampleEntity {
    private Integer id;
    private String name;

    public ExampleEntity() {
    }

    public ExampleEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + "(" + id + ")";
    }
}
