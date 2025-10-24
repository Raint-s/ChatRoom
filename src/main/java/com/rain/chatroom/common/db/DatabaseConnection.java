package com.rain.chatroom.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
public class DatabaseConnection {
    private static HikariDataSource dataSource;

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        try {
            //定义使用类，定义获取配置文件的入口input
            //给使用类定义需要的参数
            //将参数设置进去使用类当中
            //然后封装一个资源连接
            Properties props = new Properties();
            InputStream input = DatabaseConnection.class.getClassLoader()
                    .getResourceAsStream("database.properties");

            if (input == null) {
                log.warn("数据库配置文件未找到，使用默认配置");
                // 使用默认配置
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:mysql://localhost:3306/chatroom");
                config.setUsername("root");
                config.setPassword("password");
                config.setMaximumPoolSize(20);
                dataSource = new HikariDataSource(config);
                return;
            }

            props.load(input);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.username"));
            config.setPassword(props.getProperty("db.password"));
            config.setDriverClassName(props.getProperty("db.driver"));

            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "20")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minimumIdle", "5")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));

            dataSource = new HikariDataSource(config);
            log.info("数据库连接池初始化成功");

        } catch (IOException e) {
            log.error("加载数据库配置失败", e);
            throw new RuntimeException("数据库配置加载失败", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("数据库连接池已关闭");
        }
    }
}