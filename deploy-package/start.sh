#!/bin/bash
# 全栈启动脚本 (Linux)

echo "=== 启动企业级权限管理系统 ==="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误: 未安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "错误: 未安装 Docker Compose"
    exit 1
fi

# 创建上传目录
mkdir -p uploads

# 设置 JWT Secret（生产环境必须修改）
if [ -z "$JWT_SECRET" ]; then
    echo "警告: 使用默认 JWT_SECRET，生产环境请设置环境变量"
    export JWT_SECRET="cGVybWlzc2lvbi1hZG1pbi1zZWNyZXQta2V5LTIwMjQtbXVzdC1iZS1sb25nLWVub3VnaC1mb3ItaHMyNTY="
fi

# 启动服务
docker-compose up -d --build

echo ""
echo "=== 服务启动中，请等待..."
sleep 10

echo ""
echo "=== 服务状态 ==="
docker-compose ps

echo ""
echo "=== 访问地址 ==="
echo "管理后台: http://your-server-ip/admin/"
echo "树洞前端: http://your-server-ip/"
echo "后端API:  http://your-server-ip/api/"
echo ""
echo "默认账号: admin / Admin@1234"
echo ""
echo "查看日志: docker-compose logs -f"
