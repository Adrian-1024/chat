# Chat

基于 **Spring Boot** + **WebSocket (Socket.IO)** 的聊天后端服务。  
支持容器化部署，方便本地开发和线上运行。

---

## 📂 项目结构
chat-backend  
├── .idea/ # IDE 配置（已在 .gitignore 忽略）  
├── .mvn/ # Maven Wrapper 配置（已忽略）  
├── deploy/ # 部署相关文件  
├── src/ # 源代码  
├── target/ # 编译输出（已忽略）  
├── docker-compose.yml # Docker 启动配置  
├── docker-compose.dev.yml # Docker 开发环境配置  
├── Dockerfile # 应用镜像构建文件  
├── pom.xml # Maven 配置  
├── mvnw / mvnw.cmd # Maven Wrapper 脚本  
├── LICENSE # 开源协议  
└── README.md # 项目说明文档  



---

## 🚀 快速开始

### 1. 本地运行

确保你已安装 **JDK 17+** 和 **Maven**：

```bash
# 编译 & 打包
./mvnw clean package

# 启动
./mvnw spring-boot:run

# 构建镜像
docker build -t chat-backend .

# 启动容器
docker run -d -p 8080:8080 --name chat chat-backend
