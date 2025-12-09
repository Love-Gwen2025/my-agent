package com.couple.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 事务管理器配置。
 * 显式声明 {@link PlatformTransactionManager}，确保 @Transactional 能够生效，并统一管理数据库事务。
 */
@Configuration
@EnableTransactionManagement
public class TransactionManagerConfig {
    /**
     * 数据源事务管理器。
     *
     * @param dataSource Spring 容器中已有的数据源
     * @return 事务管理器实例
     */
    @Bean("transactionManagerOne")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }
}
