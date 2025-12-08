package com.ynp.agent.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "version", () -> 1L, Long.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 强制覆盖更新时间，避免实体上旧值阻断自动填充
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
