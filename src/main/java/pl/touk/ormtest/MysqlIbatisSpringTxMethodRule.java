/*
 * Copyright (c) 2012 TouK
 * All rights reserved
 */
package pl.touk.ormtest;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.core.io.ClassPathResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import com.mysql.management.MysqldResource;
import com.mysql.management.MysqldResourceI;

public class MysqlIbatisSpringTxMethodRule extends IbatisSpringTxMethodRule {

    private static final Log logger = LogFactory.getLog(MysqlIbatisSpringTxMethodRule.class);

    private static volatile String schema = "s";
    private static volatile String initScript = "mysql-init.sql";

    private static final String user = "u";
    private static final String pass = "p";
    private static final int port = 3336;
    private static final File mysqlDir = new File(new File(System.getProperty("java.io.tmpdir")), "db" + System.currentTimeMillis());

    public MysqlIbatisSpringTxMethodRule(String schema, String initScript, String sqlMapConfig) {
        if (schema != null) {
            setSchema(schema);
        }
        if (initScript != null) {
            setInitScript(initScript);
        }
        if (sqlMapConfig != null) {
            setSqlMapConfig(sqlMapConfig);
        }
    }

    public MysqlIbatisSpringTxMethodRule() {
    }

    public static void setSchema(String schema) {
        if (schema == null || schema.length() == 0) {
            throw new IllegalArgumentException("schema must not be null or empty");
        }
        if (!schema.equals(MysqlIbatisSpringTxMethodRule.schema)) {
            MysqlIbatisSpringTxMethodRule.schema = schema;
            stopMysqlAndReset();
        }
    }

    public static void setInitScript(String initScript) {
        if (initScript == null || initScript.length() == 0) {
            throw new IllegalArgumentException("initScript must not be null or empty");
        }
        if (!initScript.equals(MysqlIbatisSpringTxMethodRule.initScript)) {
            MysqlIbatisSpringTxMethodRule.initScript = initScript;
            stopMysqlAndReset();
        }
    }

    public static void setSqlMapConfig(String sqlMapConfig) {
        if (sqlMapConfig == null || sqlMapConfig.length() == 0) {
            throw new IllegalArgumentException("sqlMapConfig must not be null or empty");
        }
        if (!sqlMapConfig.equals(getSqlMapConfig())) {
            IbatisSpringTxMethodRule.setSqlMapConfig(sqlMapConfig);
            stopMysqlAndReset();
        }
    }

    private static void stopMysqlAndReset() {
        stopMysql();
        resetThreadsForCurrentTestClass();
    }

    private static final Object resourceGuard = new Object();
    private static MysqldResource resource;

    private void startAndInitMysql() {
        synchronized (resourceGuard) {
            if (resource == null) {
                resource = startMysql();
                try {
                    executeInitScript();
                } catch (RuntimeException e) {
                    logger.error("failed to execute " + initScript, e);
                    scheduleMysqlStop();
                    throw e;
                }
                // Stop mysql on java exit:
                scheduleMysqlStop();
            }
        }
    }

    public static void scheduleMysqlStop() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopMysql();
            }
        });
    }

    @Override
    protected DataSource dataSource() {
        startAndInitMysql();
        return createDataSource();
    }

    private static DataSource createDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:" + port + "/" + schema + "?user=" + user + "&password=" + pass + "&createDatabaseIfNotExist=true");
        return ds;
    }

    private static MysqldResource startMysql() {
        MysqldResource mysqldResource = new MysqldResource(mysqlDir);

        Map<String, Object> database_options = new HashMap<String, Object>();
        database_options.put(MysqldResourceI.PORT, Integer.toString(port));
        database_options.put(MysqldResourceI.INITIALIZE_USER, "true");
        database_options.put(MysqldResourceI.INITIALIZE_USER_NAME, user);
        database_options.put(MysqldResourceI.INITIALIZE_PASSWORD, pass);
        // On windows the following line causes that no firewall warning is displayed:
        database_options.put("server.bind-address", "127.0.0.1");

        mysqldResource.start("mysql", database_options);

        if (!mysqldResource.isRunning()) {
            throw new RuntimeException("mysql did not start");
        }

        logger.info("mysql is running.");

        return mysqldResource;
    }

    private void executeInitScript() {
        SimpleJdbcTestUtils.executeSqlScript(new SimpleJdbcTemplate(createDataSource()), new ClassPathResource(initScript), false);
    }

    public static void stopMysql() {
        synchronized (resourceGuard) {
            if (resource != null) {
                try {
                    resource.shutdown();
                    resource = null;
                    try {
                        FileUtils.deleteDirectory(mysqlDir);
                    } catch (IOException e) {
                    }
                } catch (RuntimeException e) {
                    logger.error("exception while stopping mysql", e);
                    throw e;
                }
            }
        }
    }
}
