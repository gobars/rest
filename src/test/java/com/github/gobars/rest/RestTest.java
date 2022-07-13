package com.github.gobars.rest;

import lombok.Cleanup;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.HttpResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/** 使用海草命令，weed server，启动服务端，做测试服务器 */
public class RestTest {
  //  static RestServer restServer = new RestServer("go-rest-server");
  //  static RestServer weedServer = new RestServer("weed", "server");

  @BeforeClass
  @SneakyThrows
  public static void beforeClass() {
    //    restServer.start();
    //    weedServer.start();

    //    Thread.sleep(6000L);
  }

  @AfterClass
  public static void afterClass() {
    //    restServer.stop();
    //    weedServer.stop();
  }

  // 启动模拟代理
  // HTTP 代理：        fproxy -addr :7777
  // HTTP 代理（认证）： fproxy -addr :7778 -auth username:password
  // HTTPS 代理：       fproxy -addr :2222 -ca .cert/localhost.pem,.cert/localhost.key
  // HTTPS 代理（认证）：fproxy -addr :2223 -ca .cert/localhost.pem,.cert/localhost.key -auth bingoo:huang

  @Test
  public void get1() {
    // 1.1 全局代理 -> HTTPS
    new Rest().exec(new RestOption().url("https://127.0.0.1:5003/v"));
    // 1.1 (不走全局代理） HTTPS
    new Rest().exec(new RestOption().disableGlobalProxy().url("https://127.0.0.1:5003/v"));
  }

  @Test
  public void for赵国刚0() {
    RestOption o1 = new RestOption().url("http://127.0.0.1:5004/v")
            .proxy("http://127.0.0.1:7777");
    new Rest().exec(o1);

    RestOption o2 = new RestOption().url("http://127.0.0.1:5004/v");
    o2 = o2.proxy("http://127.0.0.1:7777");
    new Rest().exec(o2);
  }


  @Test
  public void for赵国刚1() {
    RestOption o1 = new RestOption().url("https://127.0.0.1:5003/v")
            .proxy("http://127.0.0.1:7777");
    new Rest().exec(o1);

    RestOption o2 = new RestOption().url("https://127.0.0.1:5003/v");
    o2 = o2.proxy("http://127.0.0.1:7777");
    new Rest().exec(o2);
  }

  @Test
  public void for赵国刚2() {
    RestOption o1 = new RestOption().url("https://www.baidu.com")
            .proxy("http://127.0.0.1:7777");
    new Rest().exec(o1);

    RestOption o2 = new RestOption().url("https://www.baidu.com");
    o2 = o2.proxy("http://127.0.0.1:7777");
    new Rest().exec(o2);
  }

  @Test
  public void get() {
    // 1.1 无代理 -> HTTPS
    new Rest().exec(new RestOption().url("https://127.0.0.1:5003/v"));
    // 1.2 无代理 -> HTTP
    new Rest().exec(new RestOption().url("http://127.0.0.1:5004/v"));
    // 2.1 HTTP 代理 -> HTTPS
    new Rest()
        .exec(new RestOption().proxy("http://127.0.0.1:7777").url("https://127.0.0.1:5003/v"));
    // 2.2 HTTP 无代理 -> HTTP
    new Rest().exec(new RestOption().proxy("http://127.0.0.1:7777").url("http://127.0.0.1:5004/v"));

    // 3.1 HTTP 代理 (BASIC 认证) -> HTTPS
    new Rest()
        .exec(
            new RestOption()
                .proxy("http://username:password@127.0.0.1:7778")
                .url("https://127.0.0.1:5003/v"));
    // 3.2 HTTP 代理 (BASIC 认证) -> HTTP
    new Rest()
        .exec(
            new RestOption()
                .proxy("http://username:password@127.0.0.1:7778")
                .url("http://127.0.0.1:5004/v"));

    // 4.1 HTTPS 代理 -> HTTPS
    new Rest()
        .exec(new RestOption().proxy("https://127.0.0.1:2222").url("https://127.0.0.1:5003/v"));
    // 4.2 HTTPS 代理 -> HTTP
    new Rest()
        .exec(new RestOption().proxy("https://127.0.0.1:2222").url("http://127.0.0.1:5004/v"));

    // 5.1 HTTPS 代理 (BASIC 认证) -> HTTPS
    new Rest()
        .exec(
            new RestOption()
                .proxy("https://bingoo:huang@127.0.0.1:2223")
                .url("https://127.0.0.1:5003/v"));
    // 5.2 HTTPS 代理 (BASIC 认证) -> HTTPS
    new Rest()
        .exec(
            new RestOption()
                .proxy("https://bingoo:huang@127.0.0.1:2223")
                .url("http://127.0.0.1:5004/v"));
  }

  @Data
  public static class DirAssign {
    private int count;
    private String fid;
    private String publicUrl;
  }

  @Data
  public static class UploadResult {
    // {"name":"biniki.jpeg","size":1121511,"eTag":"87aeed08"}
    private String name;
    private int size;
    private String eTag;
  }

  @Test
  @SneakyThrows
  public void upload() {
    Rest rest = new Rest();
    String assignUrl = "http://127.0.0.1:9333/dir/assign";
    DirAssign assign =
        rest.exec(new RestOption().url(assignUrl).clazz(DirAssign.class).bizName("分配fid"));
    System.out.println(assign);

    @Cleanup val upload = new FileInputStream("src/test/resources/bikini.png");
    String url = "http://" + assign.getPublicUrl() + "/" + assign.getFid();
    UploadResult uploadResult =
        rest.exec(
            new RestOption()
                .url(url)
                .upload("biniki.png", upload)
                .req(
                    new HashMap<String, String>() {
                      {
                        put("key1", "value1");
                        put("key2", "value2");
                      }
                    })
                .clazz(UploadResult.class));
    System.out.println(uploadResult);

    new File("temp/").mkdirs();
    @Cleanup val fo = new FileOutputStream("temp/" + assign.getFid() + ".png");

    RestOption o =
        new RestOption()
            .url(url)
            .download(fo)
            .headers(
                new HashMap<String, List<String>>() {
                  {
                    put(
                        "APPID",
                        new ArrayList<String>() {
                          {
                            add("BINGOO");
                          }
                        });
                    put(
                        "TRACEID",
                        new ArrayList<String>() {
                          {
                            add(UUID.randomUUID().toString());
                          }
                        });
                  }
                });
    String downloadRest = rest.exec(o);
    System.out.println(downloadRest);

    Map<String, String> headers = rest.exec(o.method("HEAD"));
    System.out.println(headers);

    RestOption req = new RestOption().url("http://127.0.0.1:8812").req(assign);
    String postResult = rest.exec(req);
    System.out.println(postResult);

    DirAssign cloneAssign =
        rest.exec(req.method("POST").url("http://127.0.0.1:8812/echo").clazz(DirAssign.class));
    System.out.println(cloneAssign);

    HttpResponse rsp =
        rest.exec(req.method("POST").url("http://127.0.0.1:8812/echo").clazz(HttpResponse.class));
    System.out.println(rsp);

    HttpResponse getRsp =
        rest.exec(
            req.method("GET")
                .url("http://127.0.0.1:8812/echo")
                .req(assign)
                .clazz(HttpResponse.class));
    System.out.println(getRsp);

    RestRuntime result =
        rest.exec(req.method("POST").url("http://127.0.0.1:8812/echo").clazz(RestRuntime.class));
    System.out.println(result);

    Res<DirAssign> res = new Res<>();
    res.setCode(200);
    res.setMessage("OK");
    res.setData(assign);

    Res<DirAssign> res2 =
        rest.exec(
            req.method("POST")
                .url("http://127.0.0.1:8812/echo")
                .req(res)
                .type(new TypeRef<Res<DirAssign>>() {}));

    System.out.println(res2);
  }

  @Data
  public static class Res<T> {
    private int code;
    private String message;
    private T data;
  }
}
