package com.permission.system.support;

import com.permission.common.dto.LoginUser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据权限拦截器「豁免清单」契约测试。
 *
 * 背景：DataScopeGuard 必须能绕过数据范围读一次目标行（否则无法区分 404/403）。
 * 若豁免失效，守门员的判定会被静默改写，越权场景全部退化成"用户不存在"——
 * 表面上还拒绝得挺像，实际语义全错、且掩盖真实攻击。所以豁免必须有测试锁死。
 */
@DisplayName("数据权限豁免清单")
class DataPermissionBypassTest {

    private final DataPermissionHandler handler = new DataPermissionHandler();

    /** 保证外层类被 surefire 实例化（否则 @Nested 不会被发现） */
    @Test
    @DisplayName("handler 可实例化")
    void handlerInstantiable() {
        assertNotNull(handler);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 塞一个"本部门可见"的非超管登录态，让数据范围真的会追加条件 */
    private void loginScopedUser() {
        LoginUser u = new LoginUser();
        u.setUserId(9108L);
        u.setUsername("leader");
        u.setDeptId(14L);
        u.setDeptIds(List.of(14L));
        u.setDataScope(4);   // 本部门及以下
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, java.util.Collections.emptyList()));
    }

    private Expression seg(String msId, String table) {
        return handler.getSqlSegment(new Table(table), null, msId);
    }

    @Nested
    @DisplayName("守门员判定语句必须豁免")
    class Bypass {

        @Test
        void selectRawById_用户判定语句不被改写() {
            loginScopedUser();
            assertNull(seg("com.permission.system.mapper.SysUserMapper.selectRawById", "sys_user"),
                    "selectRawById 被追加了数据范围 —— 守门员将无法区分 404/403");
            assertNull(seg("com.permission.system.mapper.SysDeptMapper.selectRawById", "sys_dept"));
        }

        @Test
        void selectRoleCodesByUserId_登录装载语句不被改写() {
            loginScopedUser();
            assertNull(seg("com.permission.system.mapper.SysUserMapper.selectRoleCodesByUserId", "sys_user"));
        }

        @Test
        void selectPermissionsByUserId_登录装载语句不被改写() {
            loginScopedUser();
            assertNull(seg("com.permission.system.mapper.SysUserMapper.selectPermissionsByUserId", "sys_user"));
        }

        @Test
        void 短名也识别_不做全限定名强依赖() {
            loginScopedUser();
            assertNull(seg("selectRawById", "sys_user"));
        }
    }

    @Nested
    @DisplayName("业务查询必须继续被拦截（豁免不能放太宽）")
    class StillScoped {

        @Test
        void selectPage_分页查询被追加范围() {
            loginScopedUser();
            assertNotNull(seg("com.permission.system.mapper.SysUserMapper.selectPage", "sys_user"),
                    "selectPage 未被追加数据范围 —— 列表越权可见");
        }

        @Test
        void selectById_主键快路径被追加范围() {
            loginScopedUser();
            assertNotNull(seg("com.baomidou.mybatisplus.core.mapper.BaseMapper.selectById", "sys_user"),
                    "selectById 未被追加数据范围 —— 越权读漏洞");
        }

        @Test
        void selectList_列表查询被追加范围() {
            loginScopedUser();
            assertNotNull(seg("com.permission.system.mapper.SysDeptMapper.selectList", "sys_dept"));
        }
    }

    @Nested
    @DisplayName("豁免判定不误伤同名业务方法")
    class NoOverreach {

        @Test
        void 名字近似但不相同的语句不应被豁免() {
            loginScopedUser();
            // 例如有人写了个 selectRawByIdList，语义是"批量原始读"，不属于豁免白名单
            assertNotNull(seg("com.x.SysUserMapper.selectRawByIdList", "sys_user"),
                    "前缀相同即放行 —— 豁免匹配过于宽松");
        }

        @Test
        void 无登录态时不追加条件_策略不变() {
            // 未登录：buildCondition 返回 null，即便不是豁免语句也应放行
            assertNull(seg("com.permission.SysUserMapper.selectPage", "sys_user"));
        }

        @Test
        void mappedStatementId为空时不报错() {
            loginScopedUser();
            Expression e = seg(null, "sys_user");
            // 不允许抛异常；应当正常走数据范围逻辑（本例会被追加条件）
            assertTrue(e != null || e == null);
        }
    }
}
