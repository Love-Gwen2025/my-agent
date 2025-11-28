package com.ynp.agent.manager;

import com.ynp.agent.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Manager 层基类，集中提供公共配置。
 */
public abstract class BaseManager {

    @Autowired
    protected AppProperties appProperties;
}
