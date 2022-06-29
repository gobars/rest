# rest

[![Build Status](https://travis-ci.org/gobars/rest.svg?branch=master)](https://travis-ci.org/gobars/rest)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.github.gobars%3Arest&metric=alert_status)](https://sonarcloud.io/dashboard/index/com.github.gobars%3Arest)
[![Coverage Status](https://coveralls.io/repos/github/gobars/rest/badge.svg?branch=master)](https://coveralls.io/github/gobars/rest?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.gobars/rest/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.gobars/rest/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

log request and response for http

## Usage

```bash
[main] INFO com.github.gobars.rest.Rest - GET http://127.0.0.1:9333/dir/assign, code:200
RestTest.DirAssign(count=1, fid=3,13807be42d, publicUrl=localhost:8080)
[main] INFO com.github.gobars.rest.Rest - POST http://localhost:8080/3,13807be42d, code:201
RestTest.UploadResult(name=biniki.png, size=440085, eTag=ebb52112)
[main] INFO com.github.gobars.rest.Rest - GET http://localhost:8080/3,13807be42d, code:200
null
[main] INFO com.github.gobars.rest.Rest - HEAD http://localhost:8080/3,13807be42d, code:200
{Accept-Ranges=bytes, Etag="ebb52112", Content-Disposition=inline; filename="biniki.png", Last-Modified=Thu, 10 Sep 2020 11:26:03 GMT, Content-Length=440085, Date=Thu, 10 Sep 2020 11:26:03 GMT, Content-Type=image/png}
[main] INFO com.github.gobars.rest.Rest - POST http://127.0.0.1:8812, code:200
POST / HTTP/1.1
Host: 127.0.0.1:8812
Accept-Encoding: gzip,deflate
Connection: Keep-Alive
Content-Length: 61
Content-Type: application/json; charset=UTF-8
User-Agent: Apache-HttpClient/4.5.12 (Java/11.0.8)

{"count":1,"fid":"3,13807be42d","publicUrl":"localhost:8080"}
```

## Setting

1. env `REST_MAX_CONN_TOTAL` 设置 连接池总大小，默认值 100。设置方法： 使用 `java -DREST_MAX_CONN_TOTAL=100` 或者 启动前设置 shell 环境变量 `export REST_MAX_CONN_TOTAL=100`
2. env `REST_MAX_CONN_PER_ROUTE` 设置 单个主机路由总大小，默认值 100。设置方法同上。
3. 代理
   1. 全局代理：env `REST_PROXY` 设置代理，示例值 `http://localhost:8080`。使用 `java -DREST_PROXY=http://www.proxy.com:8080` 或者 启动前设置 shell 环境变量 `export REST_PROXY=http://www.proxy.com:8080`
   2. 请求代理：`new Rest().exec(new RestOption().proxy("http://www.proxy.com:8080").url("http://127.0.0.1:8080/status"));`
