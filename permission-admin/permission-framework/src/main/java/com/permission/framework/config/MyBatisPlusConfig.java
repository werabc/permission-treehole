package com.permission.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;

@Configuration
@EnableTransactionManagement
@MapperScan("com.permission.system.mapper")
public class MyBatisPlusConfig {

    /**
     * 数据权限处理器由 permission-system 模块实现（需要登录用户上下文与组织树工具），
     * 这里用 ObjectProvider 可选注入，避免 framework → system 反向依赖
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            ObjectProvider<MultiDataPermissionHandler> dataPermissionHandlerProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        MultiDataPermissionHandler handler = dataPermissionHandlerProvider.getIfAvailable();
        if (handler != null) {
            // 必须排在分页拦截器之前：先限定数据范围再分页，否则 total 会统计到越权数据
            interceptor.addInnerInterceptor(new DataPermissionInterceptor(handler));
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}

