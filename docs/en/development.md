# Local Development

[简体中文](../zh-CN/development.md) | [Back to README](../../README.md)

## Dockerized Development (Hot Reload), Recommended

If you don't want to install JDK/Node locally, `docker-compose.dev.yml` runs everything in Docker with hot reload: the backend uses spring-boot-devtools to restart on recompiled classes (a watcher recompiles `src/` on change), and the frontend runs the Vite dev server with HMR on port 5173, proxying `/jiallo-backend` to the backend container.

The dev stack uses a separate compose project name (`jiallo-dev`), so it gets its own containers, network and data volumes (`jiallo-dev_*`) — fully isolated from a deployment started with `docker-compose.yml` alone. Both can run at the same time. On first start, the dev stack initializes fresh data: the database schema from the init SQL, the initial admin account, and the object-storage bucket.

```bash
# docker-compose.dev.yml pulls in docker-compose.yml via the `include` directive,
# so a single -f is all you need (passing both files explicitly is equivalent).
docker compose -f docker-compose.dev.yml up --build
```

Then visit http://localhost:5173. Notes:

* The first start is slow: Maven downloads all dependencies into the `maven_repo` volume.
* Backend hot-restart takes a few seconds per change; frontend HMR is instant.
* The backend container runs as root and writes to `java-backend/target/`. Run `sudo chown -R $USER:$USER java-backend/target` before building on the host again.
* If `package-lock.json` changes, refresh the frontend dependencies with `docker compose -f docker-compose.dev.yml exec frontend npm install`.

## Developing on the Host Machine

Dependencies:

* JDK 21
* Node.js v26 + NPM v11.14.1
* MySQL 9.6
* Redis 8.8.0
* RustFS v1.0.0-alpha.93

The program is developed and tested on the dependencies above. You may try to use different versions or compatible implementations of the dependencies, however idk whether it will work.

The program contains two parts: the java backend and the vue frontend.

### Java Backend

The Java backend is a standard Spring Boot application. The ORM is MyBatis Plus. Spring Security is used for authentication and authorization.

If you are not familiar to Java developement and wants to develop the backend, we recommend you use IntelliJ IDEA (Community or Ultimate) to open the project as Maven project. Then you can modify the application.yml to connect to your local MySQL, Redis or RustFS instance. You can go to 'Maven' panel and click the 'sync maven project' button to install the dependencies. Then you can navigate to src/main/java/moe/byn/Bynspring21Application and click the green Run button to start the application.

### Vue Frontend

Use `npm install` to install the dependencies and `npm run dev` to start the development server.
