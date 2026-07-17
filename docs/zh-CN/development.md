# 本地开发

[English](../en/development.md) | [返回 README](../../README.zh-CN.md)

## 容器化开发(热更新),推荐

如果你不想在本地安装 JDK/Node,可以使用 `docker-compose.dev.yml` 在 Docker 中运行整套环境并支持热更新:后端通过 spring-boot-devtools 在类文件变化时自动重启(有守护脚本监听 `src/` 变化并重新编译),前端运行带 HMR 的 Vite 开发服务器(端口 5173),并将 `/jiallo-backend` 代理到后端容器。

开发环境使用独立的 compose 项目名(`jiallo-dev`),因此拥有独立的容器、网络和数据卷(`jiallo-dev_*`),与使用 `docker-compose.yml` 部署的环境完全隔离,两者可以同时运行。首次启动时开发环境会自动初始化全新数据:执行初始化 SQL 建表、创建初始管理员账户、创建对象存储 bucket。

```bash
# docker-compose.dev.yml pulls in docker-compose.yml via the `include` directive,
# so a single -f is all you need (passing both files explicitly is equivalent).
docker compose -f docker-compose.dev.yml up --build
```

然后访问 http://localhost:5173。注意事项:

* 首次启动较慢:Maven 会把全部依赖下载到 `maven_repo` 数据卷中。
* 后端每次改动的热重启需要几秒钟;前端 HMR 是即时的。
* 后端容器以 root 运行并会写入 `java-backend/target/`。如果之后要回到宿主机上构建,请先执行 `sudo chown -R $USER:$USER java-backend/target`。
* 如果 `package-lock.json` 有更新,请执行 `docker compose -f docker-compose.dev.yml exec frontend npm install` 刷新前端依赖。

## 在宿主机上开发

依赖:

* JDK 21
* Node.js v26 + NPM v11.14.1
* MySQL 9.6
* Redis 8.8.0
* RustFS v1.0.0-alpha.93

本程序是在上述依赖环境下开发与测试的。你可以试试用其他版本或兼容的依赖,但行不行得通我就不知道了。

本程序由两部分组成:Java 后端和 Vue 前端。

### Java 后端

Java 后端是一个标准的 Spring Boot 应用程序。ORM 使用的是 MyBatis Plus,并使用 Spring Security 进行身份认证和授权。

如果你不熟悉 Java 开发,但想开发本项目的后端,我们建议你使用 IntelliJ IDEA(社区版或旗舰版)以 Maven 项目的形式打开本项目。然后,你可以修改 application.yml 文件来连接你本地的 MySQL、Redis 或 RustFS 实例。你可以进入 Maven 面板并点击"sync maven project"(同步 Maven 项目)按钮来安装依赖项。最后,定位到 src/main/java/moe/byn/Bynspring21Application,点击绿色的运行按钮即可启动应用程序。

### Vue 前端

使用 `npm install` 安装依赖,使用 `npm run dev` 启动开发服务器。
