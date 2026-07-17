# Installation

[简体中文](../zh-CN/installation.md) | [Back to README](../../README.md)

## Docker Compose (Build yourself), Recommended

> Notice: This method doesn't need you to install MySQL, RustFS and Redis manually. However, this may cause resource waste if you already have a MySQL, RustFS or Redis instance. To be precise, we recommend you have at least 1.5GB of free memory to run the whole project and its dependencies.

Requirements:

* Docker
* Docker Compose
* Git

### 1. Clone the repository

```bash
git clone https://github.com/baiyuanneko/jiallo-platform.git
```

### 2. Set up environment variables

Copy the `.env.example` file to `.env` and modify the variables as needed.

Please carefully read the instructions in the `.env.example` file before start! Especially the part about how to generate and set the necessary keys.

### 3. Build the images

```bash
docker compose build
```

### 4. Run

```bash
docker compose up -d
```

## Upgrading

If you want to upgrade later when you pull the repository again, you can use the following command:

```bash
docker compose build
```

Then run again:

```bash
docker compose down && docker compose up -d
```
