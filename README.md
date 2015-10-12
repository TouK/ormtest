#OrmTest - Object Relation Mapping Testing

[![Build Status](https://img.shields.io/travis/TouK/ormtest/master.svg?style=flat)](https://travis-ci.org/TouK/ormtest)

##What is it?

OrmTest is a java framework for easy JUnit 4.9+ testing of object-relation mappings in projects that use Spring-based DAOs. OrmTest eliminates the need to start a spring context during transactional tests. This way tests run much faster and are much simpler.

##Hibernate, iBATIS, JDBC 

Currently OrmTest supports testing of DAOs that use Hibernate, iBATIS or JDBC. 

##Usage

For Hibernate see [HibernateSpringTxTestRule](http://touk.github.io/ormtest/apidocs/pl/touk/ormtest/HibernateSpringTxTestRule.html).

For iBATIS see [IbatisSpringTxTestRule](http://touk.github.io/ormtest/apidocs/pl/touk/ormtest/IbatisSpringTxTestRule.html).

For JDBC see [JdbcSpringTxTestRule](http://touk.github.io/ormtest/apidocs/pl/touk/ormtest/JdbcSpringTxTestRule.html).

##Javadoc

http://touk.github.io/ormtest/apidocs/

##Project license

OrmTest is an open source project released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

##Where it can be found

OrmTest is hosted on [GitHub](http://touk.github.com/ormtest/).

##Author

OrmTest was created in [TouK](http://touk.pl) by [Michał Sokołowski](mailto:msk@touk.pl).
