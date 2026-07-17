# 安装

[English](../en/installation.md) | [返回 README](../../README.zh-CN.md)

## Docker Compose(自行构建,推荐)

> 注意:此方法不需要您手动安装 MySQL、RustFS 和 Redis。但是,如果您已经拥有 MySQL、RustFS 或 Redis 实例,则此方法可能会导致资源浪费。具体来说,若使用此方法,我们建议您至少有 1.5GB 的空闲内存来运行整个项目及其依赖项。

环境要求:

* Docker
* Docker Compose
* Git

### 1. 克隆本仓库

```bash
git clone https://github.com/baiyuanneko/jiallo-platform.git
```

### 2. 设置环境变量

将 `.env.example` 文件复制为 `.env`,并修改变量。

请在开始之前务必仔细阅读 `.env.example` 文件中的说明!尤其是关于如何生成和设置所需密钥的部分。

### 3. 构建镜像

```bash
docker compose build
```

### 4. 运行

```bash
docker compose up -d
```

## 升级

如果你稍后想要更新,你需要先重新拉取仓库,然后运行以下命令来重新构建:

```bash
docker compose build
```

然后重新运行:

```bash
docker compose down && docker compose up -d
```
