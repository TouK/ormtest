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
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import com.mysql.management.MysqldResource;
import com.mysql.management.MysqldResourceI;

public class MysqlIbatisSpringTxTestRule extends IbatisSpringTxTestRule {

    private static final Log logger = LogFactory.getLog(MysqlIbatisSpringTxTestRule.class);

    private static final String[] schemaDefault = new String[]{"s"};
    private static volatile String[] schema = schemaDefault;
    private static final String[] initScriptDefault = new String[]{"mysql-init.sql"};
    private static volatile String[] initScript = initScriptDefault;

    private static final String user = "u";
    private static final String pass = "p";
    private static final int port = 3336;
    private static final File mysqlDir =
            new File(new File(System.getProperty("java.io.tmpdir")), "db" + System.currentTimeMillis());

    public MysqlIbatisSpringTxTestRule(String schema, String initScript, String sqlMapConfig) {
        setSchema(schema);
        setInitScript(initScript);
        setSqlMapConfig(sqlMapConfig);
    }

    public MysqlIbatisSpringTxTestRule() {
    }

    public static void setSchema(String schema) {
        if (schema == null || schema.length() == 0) {
            throw new IllegalArgumentException("schema must not be null or empty");
        }
        String[] schemaArray = new String[]{schema};
        if (!Arrays.equals(schemaArray, MysqlIbatisSpringTxTestRule.schema)) {
            stopMysqlAndReset();
            MysqlIbatisSpringTxTestRule.schema = schemaArray;
        }
    }

    public static void setInitScript(String initScript) {
        if (initScript == null || initScript.length() == 0) {
            throw new IllegalArgumentException("initScript must not be null or empty");
        }
        String[] initScriptArray = new String[]{initScript};
        if (!Arrays.equals(initScriptArray, MysqlIbatisSpringTxTestRule.initScript)) {
            stopMysqlAndReset();
            MysqlIbatisSpringTxTestRule.initScript = initScriptArray;
        }
    }

    public static void setSqlMapConfig(String... sqlMapConfig) {
        if (!Arrays.equals(sqlMapConfig, getSqlMapConfig())) {
            stopMysqlAndReset();
            IbatisSpringTxTestRule.setSqlMapConfig(sqlMapConfig);
        }
    }

    private static void stopMysqlAndReset() {
        stopMysql();
        resetThreadsForCurrentTestClass(false);
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
        return createDataSource(0);
    }

    private static DataSource createDataSource(int i) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:" + port + "/" + schema[i] + "?user="
                + user + "&password=" + pass + "&createDatabaseIfNotExist=true");
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
        if (schema.length != initScript.length) {
            throw new IllegalStateException("schema count (" + schema.length
                    + ") different than init script count (" + initScript.length + ")");
        }
        for (int i = 0; i < initScript.length; i++) {
            try {
                SimpleJdbcTestUtils.executeSqlScript(
                        new SimpleJdbcTemplate(createDataSource(i)),
                        new ClassPathResource(initScript[i]), false);
            } catch (RuntimeException e) {
                logger.error("failed to execute init script " + initScript[i], e);
                throw e;
            }
        }
    }

    public static void resetThreadsForCurrentTestClass() {
        MysqlIbatisSpringTxTestRule.resetThreadsForCurrentTestClass(true);
    }

    protected static void resetThreadsForCurrentTestClass(boolean hardReset) {
        IbatisSpringTxTestRule.resetThreadsForCurrentTestClass(hardReset);
        if (hardReset) {
            schema = schemaDefault;
            initScript = initScriptDefault;
        }
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
                        logger.warn("exception while deleting directory: " + mysqlDir, e);
                    }
                } catch (RuntimeException e) {
                    logger.error("exception while stopping mysql", e);
                    throw e;
                }
            }
        }
    }

    public static void setSchemasAndInitScripts(String... schemaAndInitScriptArray) {
        if (schemaAndInitScriptArray == null
                || schemaAndInitScriptArray.length == 0
                || schemaAndInitScriptArray.length % 2 == 1) {
            throw new IllegalArgumentException(
                    "schemaAndInitScriptArray must not be null nor empty and should have the following form: " +
                    "[<schema1>, <initScript1>, <schema2>, <initScript2>,..., <schemaN>, <initScriptN>]");
        }
        schema = new String[schemaAndInitScriptArray.length / 2];
        initScript = new String[schemaAndInitScriptArray.length / 2];
        for (int i = 0; i < schemaAndInitScriptArray.length; i += 2) {
            if (schemaAndInitScriptArray[i] == null || schemaAndInitScriptArray[i].length() == 0) {
                throw new IllegalArgumentException(
                        "schema at index " + i + " of schemaAndInitScriptArray is null or empty");
            }
            if (schemaAndInitScriptArray[i + 1] == null || schemaAndInitScriptArray[i + 1].length() == 0) {
                throw new IllegalArgumentException(
                        "init script at index " + (i + 1) + " of schemaAndInitScriptArray is null or empty");
            }
            schema[i / 2] = schemaAndInitScriptArray[i];
            initScript[i / 2] = schemaAndInitScriptArray[i + 1];
        }
    }
}
