package com.ynp.agent.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 自动填充时间字段。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 1. 插入时写入创建与更新时间。
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    /**
     * 1. 更新时写入更新时间。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        if (Objects.nonNull(metaObject)) {
            strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }
    }
}
