package com.permission.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.framework.config.RedisConfig
// 【模块】permission-framework（安全与基础设施模块）
//
// 【使用的注解/技术】
//   - @Configuration — Spring，声明配置类
//   - @Bean — Spring，向容器注册 RedisTemplate<String, Object> 单例 Bean
//
// 【关键依赖/注入】
//   - RedisConnectionFactory — Spring Data Redis 自动配置的连接工厂（由 application.yml 中
//     spring.redis.* 注入），通过方法参数自动装配
//   - StringRedisSerializer — 键与 HashKey 使用 UTF-8 字符串序列化（可读性好，便于 redis-cli 调试）
//   - GenericJackson2JsonRedisSerializer — 值与 HashValue 使用 JSON 序列化（跨语言可读，
//     反序列化为 LinkedHashMap/原始对象时需业务层强转）
//
// 【关联文件】
//   - 被 permission-api 模块启动类扫描装配
//   - 被 JwtAuthenticationFilter 注入（调用 redisTemplate.hasKey 做令牌黑名单校验）
//   - 被 permission-service 层注入用于缓存业务数据（如用户信息、菜单树）
//   - 连接参数由 spring-boot-starter-data-redis 自动配置读取 application.yml
//
// 【核心作用】统一 Redis 读写模板的序列化策略，使 Key 可读、Value 以 JSON 存储。
//
// 【设计必要性】
//   - 默认 RedisTemplate 使用 JdkSerializationRedisSerializer，写入 redis-cli 看到的是乱码，
//     且跨语言不可读；替换为 String + JSON 组合后，开发与运维排查问题直观。
//   - 所有模块共用同一个 RedisTemplate Bean，保证序列化/反序列化策略一致，反之会出现
//     ClassCastException 或反序列化失败。
//
// 【注意事项/安全提示】
//   - GenericJackson2JsonRedisSerializer 默认不会写入 @type 类型标记，反序列化为 Object/LinkedMap，
//     业务层使用显式类型（如 String/自定义 DTO）时行为更可控；
//   - 更换序列化方案会与历史已写入数据不兼容，需清空库后切换；
//   - 不要在 Redis 中存放明文密码等敏感信息；若启用了 AOF/RDB，数据落盘同样需考虑机密性。
// ============================================================
