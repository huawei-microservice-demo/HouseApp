# Build/Deploy/Run
## 本地开发环境上 Build/Deploy
### Build
#### 1. maven install
1. 克隆代码到本地
2. 到项目根目录，执行 ```maven install -f pom.xml -s settings.xml ``` 编译

#### 2. build docker images
1. 到项目的 deployment 目录
2. 执行 ```bash build-images.sh build <your tag>``` 本地构建镜像
3. 在 ServiceStage 镜像仓库创建 namespace/仓库 
4. 执行 docker login 登录到远程镜像中心
5. 执行 ```bash build-images.sh push <your tag> <your repo namespace> ``` 将本地镜像推送到远程仓库

### Deploy
#### 1. 创建RDS（MySQL），并创建业务数据库与表
1. 创建RDS
2. 登录到MySQL，执行 deployment/sql 目录下的三个sql文件，创建对应的库与表
3. 创建数据库对应的用户（可选）

#### 2. 创建DCS（Redis）
#### 3. 创建连接CSE注册中心所需凭证
微服务启动后CSE的SDK会自动连接CSE的注册中心，微服务接入到注册中心需要用户的AK/SK作为认证凭证。使用 ServiceStage 的 ConfigMap 对象存储此凭证。

[创建 ConfigMap ](https://servicestage.huaweicloud.com/servicestage/#/stage/configs/newcreate/596aeb28-de26-11e7-a506-0255ac101e21/clusterName/default/configsName/create)，格式如下：

```
kind: ConfigMap
apiVersion: v1
metadata:
  name: cse-credential
  namespace: default
  selfLink: /api/v1/namespaces/default/secrets/cse-credential
data:
  certificate.yaml: |
    cse:
      credentials:
        accessKey: ak
        secretKey: sk
        akskCustomCipher: default  
```

**注意：**
1. 此 ConfigMap 中的 **certificate.yaml** key 对应的内容在部署时候，需要被mount为容器内的 /opt/CSE/etc/cipher 目录下。
2. 实际部署时候，需要替换 accessKey/secretKey 为实际的 AK/SK

#### 4. 创建访问RDS（MySQL）所需的凭证
访问MySQL的凭证保存在 ServiceStage 的 ConfigMap 中，通过环境变量的方式导出给应用使用。
本应用中， user-service/account-service/product-service 使用到了数据库 user_db/account_db/product_db ，需要创建三个 ConfigMap ，名称分别为：mysql-userdb/mysql-accountdb/mysql-productdb 。

保存 MySQL 访问凭证的 ConfigMap 模板如下：

```
kind: ConfigMap
apiVersion: v1
metadata:
  name: mysql-credential-template
  namespace: default
  selfLink: /api/v1/namespaces/default/secrets/mysql-credential-template
data:
  db.url: jdbc:mysql://host:port/db_name
  db.driver: com.mysql.jdbc.Driver
  db.username: username
  db.password: password
```

#### 5. 创建访问DCS（Redis）所需的凭证
访问Redis的凭证保存在 ServiceStage 的 ConfigMap 中，通过环境变量方式导出给应用使用。
本应用中， user-service/account-service/product-service/customer-service 使用到了Redis，用Redis做事务，四个微服务使用的是同一个Redis实例。
保存 Redis 访问凭证的 ConfigMap 模板如下：

```
kind: ConfigMap
apiVersion: v1
metadata:
  name: redis-credential
  namespace: default
  selfLink: /api/v1/namespaces/default/secrets/redis-credential
data:
  cse.tcc.transaction.redis.host: redis-host
  cse.tcc.transaction.redis.port: redis-port
  cse.tcc.transaction.redis.password: redis-password

```
#### 5. 卷
cse-credential 这个 ConfigMap 是通过挂接卷的方式到容器中使用，挂接的卷的目录为: /opt/CSE/etc/cipher

#### 6. 环境变量
redis, mysql 访问凭证的 ConfigMap 是通过导出环境变量到容器中的方式使用的，各个环境变量与 ConfigMap 映射关系如下

1. APPLICATION_ID: 手动输入，对应 CSE 中的 application 
2. TCC_REDIS_HOST: configmap: redis-credential, key: cse.tcc.transaction.redis.host
3. TCC_REDIS_PORT: configmap: redis-credential, key: cse.tcc.transaction.redis.port
4. TCC_REDIS_PASSWD: configmap: redis-credential, key: cse.tcc.transaction.redis.password
5. DB_URL: configmap: mysql-xxx_db, key: db.url
6. DB_USERNAME: configmap: mysql-xxx_db, key: db.username
7. DB_PASSWD: configmap: mysql-xxx_db, key: db.password

#### 7. 镜像版本
部署堆栈的时候，需要输入各个微服务正确的镜像版本

## 使用 DevCloud 流水线 Build/Deploy
Doing...