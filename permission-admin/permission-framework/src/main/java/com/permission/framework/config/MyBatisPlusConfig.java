package com.permission.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;

@Configuration
@EnableTransactionManagement
@MapperScan("com.permission.system.mapper")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
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
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.config.MyBatisPlusConfig
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Configuration — Spring，声明配置类，注册当前类定义的 Bean 到 IoC 容器
//   - @EnableTransactionManagement — Spring，开启基于注解的声明式事务管理（@Transactional 生效）
//   - @Bean — Spring，向容器注册 mybatisPlusInterceptor / metaObjectHandler 两个 Bean
//   - @MapperScan("com.permission.system.mapper") — MyBatis Spring，自动扫描装配 Mapper 接口为代理 Bean
//
// 【关键依赖/注入】
//   - 注入 DbType.MYSQL — 指定分页插件的数据库方言
//   - 注入 PaginationInnerInterceptor — MyBatis-Plus 分页插件，自动拼接 LIMIT 分页
//   - 注入 BlockAttackInnerInterceptor — MyBatis-Plus 防全表更新/删除拦截器（拦截无 WHERE 条件的 update/delete）
//   - MetaObjectHandler — 字段自动填充（insert 时填充 createTime/updateTime/deleted，update 时刷新 updateTime）
//
// 【关联文件】
//   - 被 permission-api 模块启动类扫描装配（包扫描含 com.permission.framework）
//   - 为 permission-system 模块所有 Mapper 提供分页与拦截器能力
//   - 被 entity 基类字段（createTime / updateTime / deleted）配合做自动填充
//
// 【核心作用】MyBatis-Plus 的全局配置入口，统一注册分页插件 + 防全表操作拦截器 + 审计字段自动填充策略。
//
// 【设计必要性】
//   - 没有分页拦截器，PageHelper 式手动分页会散落在各 Service 中；
//   - 没有 BlockAttackInnerInterceptor，误写的 delete(null) 可能清空整张表；
//   - 没有 MetaObjectHandler，每插入/更新都要手写 setUpdateTime(now)，易遗漏。
//
// 【注意事项/安全提示】
//   - BlockAttackInnerInterceptor 只对 MyBatis-Plus 生成的语句生效，原生 SQL XML 不做防护；
//   - 若未来切换数据库（如 PostgreSQL），需同步修改 DBType；
//   - 审计字段名（createTime 等）由实体类决定，添加新审计字段需同时更新此处与服务端填充逻辑。
// ============================================================
