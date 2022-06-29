package com.github.gobars.rest;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.gobars.rest.json.JsonMapper;
import com.github.gobars.rest.json.TypeRef;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

@Slf4j
public class Rest {

  /** MaxConnTotal、MaxConnPerRoute 可以通过设置系统参数或者环境变量的方式修改 */
  public static final HttpClient CLIENT =
      HttpClientBuilder.create()
          .setMaxConnTotal(getEnvUint("REST_MAX_CONN_TOTAL", 100))
          .setMaxConnPerRoute(getEnvUint("REST_MAX_CONN_PER_ROUTE", 100))
          .addInterceptorFirst(new Rsp())
          .addInterceptorFirst(new Req())
          //  http://www.proxy.com:8080
          .setProxy(
              createProxy(getEnv("REST_PROXY"))) // new HttpHost("www.proxy.com", 8080, "http"))
          .build();

  private static HttpHost createProxy(String proxy) {
    return proxy == null ? null : HttpHost.create(proxy);
  }

  private RequestConfig.Builder requestConfigBuilder() {
    return RequestConfig.custom()
        // 从连接池获取到连接的超时时间，如果是非连接池的话，该参数暂时没有发现有什么用处
        .setConnectionRequestTimeout(30 * 1000)
        // 指客户端和服务进行数据交互的时间，是指两者之间如果两个数据包之间的时间大于该时间则认为超时，而不是整个交互的整体时间，
        // 比如如果设置1秒超时，如果每隔0.8秒传输一次数据，传输10次，总共8秒，这样是不超时的。
        // 而如果任意两个数据包之间的时间超过了1秒，则超时。
        .setSocketTimeout(30 * 1000)
        // 建立连接的超时时间
        .setConnectTimeout(30 * 1000);
  }

  public <T> T exec(RestOption restOption) {
    return exec(restOption, new RestRuntime());
  }

  public <T> T exec(RestOption restOption, RestRuntime rt) {
    try {
      return execInternal(restOption, rt);
    } catch (Exception ex) {
      rt.setException(ex);
      log.error("请求地址:{} 费时:{}毫秒 异常:{}", rt.getUrl(), rt.getHttpCostMillis(), ex.getMessage());
      throw ex;
    } finally {
      restOption.getDoneBiz().done(rt.getException() == null, rt);
    }
  }

  @SneakyThrows
  private HttpRequestBase buildRequest(RestOption ro, RestRuntime rt) {
    switch (rt.getMethod().toUpperCase()) {
      case "POST":
        return new HttpPost(rt.getUrl());
      case "DELETE":
        return new HttpDelete(rt.getUrl());
      case "OPTIONS":
        return new HttpOptions(rt.getUrl());
      case "PUT":
        return new HttpPut(rt.getUrl());
      case "HEAD":
        return new HttpHead(rt.getUrl());
      case "GET":
      default:
        if (ro.getRequestBody() != null) {
          URIBuilder ub = new URIBuilder(rt.getUrl());
          Map<String, Object> uriVariables = convertMap(ro.getRequestBody());
          for (Map.Entry<String, Object> entry : uriVariables.entrySet()) {
            ub.addParameter(entry.getKey(), entry.getValue().toString());
          }

          rt.setUrl(ub.toString());
        }

        return new HttpGet(rt.getUrl());
    }
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private <T> T execInternal(RestOption ro, RestRuntime rt) {
    rt.setMethod(fixMethod(ro));
    rt.setUrl(ro.getUrl());

    HttpRequestBase req = buildRequest(ro, rt);
    RequestConfig.Builder rc = requestConfigBuilder();
    if (ro.getProxy() != null) {
      rc.setProxy(createProxy(ro.getProxy()));
    }
    req.setConfig(rc.build());
    jsonBody(ro, req, rt);
    copyHeaders(ro, req);

    HttpResponse rsp;
    val ctx = new BasicHttpContext();
    ctx.setAttribute(REST_OPTION_KEY, ro);
    long start = System.currentTimeMillis();

    try {
      rsp = CLIENT.execute(req, ctx);
      rt.setResponse(rsp);
    } finally {
      rt.setHttpCostMillis(System.currentTimeMillis() - start);
    }

    if (ro.isDump()) {
      log.info(
          "业务名称:{} 请求方法:{} 请求地址:{} 响应码:{} 费时:{}毫秒",
          ro.getBizName(),
          rt.getMethod(),
          rt.getUrl(),
          rsp.getStatusLine().getStatusCode(),
          rt.getHttpCostMillis());
    }

    codeCheck(ro, rsp, rt);

    HttpEntity rspEntity = rsp.getEntity();
    if (rspEntity != null) {
      if (ro.getDownload() != null) {
        rspEntity.writeTo(ro.getDownload());
        return null;
      }

      rt.setResultBody(EntityUtils.toString(rspEntity));
    }

    if (ro.isDump()) {
      log.info("响应体:{}", rt.getResultBody());
    }

    T t = (T) parseT(ro, rsp, rt);
    OkBiz<T> okBiz = ro.getOkBiz();
    if (!okBiz.isOk(rt.getStatusCode(), rt, t)) {
      throw new HttpRestException(rt.getUrl(), rt.getStatusCode(), rsp, "业务判断不成功");
    }

    return t;
  }

  private void copyHeaders(RestOption ro, HttpRequestBase req) {
    Map<String, List<String>> moreHeaders = ro.getMoreHeaders();

    if (moreHeaders == null) {
      return;
    }

    for (val headers : moreHeaders.entrySet()) {
      for (val value : headers.getValue()) {
        req.addHeader(headers.getKey(), value);
      }
    }
  }

  private String fixMethod(RestOption ro) {
    if (ro.getMethod() != null) {
      return ro.getMethod();
    }

    if (ro.getRequestBody() != null || ro.getUpload() != null) {
      return "POST";
    }

    return "GET";
  }

  private Map<String, String> copyHeaders(HttpResponse rsp) {
    Header[] allHeaders = rsp.getAllHeaders();
    Map<String, String> headers = new HashMap<>(allHeaders.length);

    for (val header : allHeaders) {
      headers.put(header.getName(), header.getValue());
    }
    return headers;
  }

  private void codeCheck(RestOption ro, HttpResponse rsp, RestRuntime rt) {
    int code = rsp.getStatusLine().getStatusCode();
    rt.setStatusCode(code);
    OkStatus okStatus = ro.getOkStatus();
    if (!okStatus.isOk(code)) {
      throw new HttpRestException(ro.getUrl(), code, rsp);
    }
  }

  @SneakyThrows
  private void jsonBody(RestOption ro, HttpRequestBase req, RestRuntime rt) {
    if (rt.getMethod().equals("GET") || !(req instanceof HttpEntityEnclosingRequest)) {
      return;
    }

    val er = (HttpEntityEnclosingRequest) req;

    MultipartEntityBuilder builder = null;
    if (ro.getUpload() != null) {
      builder = MultipartEntityBuilder.create();
      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      String fn = ro.getFileName();
      builder.addBinaryBody(fn, ro.getUpload(), ContentType.DEFAULT_BINARY, fn);
    }

    if (ro.getRequestBody() != null) {
      String payload = JsonMapper.forNonNull().tryToJson(ro.getRequestBody());
      if (ro.isDump()) {
        log.info("请求体:{}", payload);
      }

      rt.setPayload(payload);
      if (builder == null) {
        er.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
      } else {
        for (val v : convertMap(ro.getRequestBody()).entrySet()) {
          if (v.getValue() != null) {
            val b = new StringBody(v.getValue().toString(), ContentType.APPLICATION_JSON);
            builder.addPart(v.getKey(), b);
          }
        }
      }
    }

    if (builder != null) {
      er.setEntity(builder.build());
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> convertMap(Object body) {
    if (body instanceof Map) {
      return (Map<String, Object>) body;
    }

    return JsonMapper.forNonNull()
        .tryFromJson(
            JsonMapper.forNonNull().tryToJson(body), new TypeRef<Map<String, Object>>() {});
  }

  private Object parseT(RestOption ro, HttpResponse rsp, RestRuntime rt) {
    if ("HEAD".equals(rt.getMethod())) {
      return copyHeaders(rsp);
    }

    Type type = ro.getType();
    if (type != null) {
      return JsonMapper.forNonNull().tryFromJson(rt.getResultBody(), type);
    }

    Class<?> clazz = ro.getClazz();
    if (clazz == HttpResponse.class) {
      return rsp;
    }

    if (clazz == RestRuntime.class) {
      return rt;
    }

    if (clazz != null) {
      return JsonMapper.forNonNull().tryFromJson(rt.getResultBody(), clazz);
    }

    return rt.getResultBody();
  }

  @EqualsAndHashCode(callSuper = true)
  @Value
  public static class HttpRestException extends RuntimeException {

    String uri;
    int code;
    transient HttpResponse response;

    public HttpRestException(String uri, int code, HttpResponse response) {
      this(uri, code, response, "");
    }

    public HttpRestException(String uri, int code, HttpResponse response, String message) {
      super(message);
      this.uri = uri;
      this.code = code;
      this.response = response;
    }

    @Override
    public String toString() {
      return "url [" + uri + "] failed code:[" + code + "]";
    }
  }

  private static final String REST_OPTION_KEY = "REST_OPTION_KEY";

  /**
   * httpclient请求响应拦截器.
   *
   * <p>参考https://www.tutorialspoint.com/apache_httpclient/apache_httpclient_interceptors.htm
   */
  static class Req implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest r, HttpContext ctx) {
      RestOption ro = (RestOption) ctx.getAttribute(REST_OPTION_KEY);
      if (!ro.isDump()) {
        return;
      }

      log.info("请求啦 {}", r.getRequestLine());
      for (Header h : r.getAllHeaders()) {
        log.info("请求头 {}:{}", h.getName(), h.getValue());
      }
    }
  }

  static class Rsp implements HttpResponseInterceptor {

    @Override
    public void process(HttpResponse r, HttpContext ctx) {
      RestOption ro = (RestOption) ctx.getAttribute(REST_OPTION_KEY);
      if (!ro.isDump()) {
        return;
      }

      log.info("响应啦 {}", r.getStatusLine());
      for (Header h : r.getAllHeaders()) {
        log.info("响应头 {}:{}", h.getName(), h.getValue());
      }
    }
  }

  /**
   * 取 JVM 设置系统属性（-D）和环境变量（export）的值.
   *
   * <p>系统属性优先于环境变量
   *
   * @param key 变量值
   * @param defaultValue 默认值
   * @return 属性无符号整形值
   */
  public static int getEnvUint(String key, int defaultValue) {
    try {
      String env = getEnv(key);
      if (Objects.isNull(env)) {
        return defaultValue;
      }
      int value = Integer.parseInt(env);
      return value <= 0 ? defaultValue : value;
    } catch (Exception ignore) {
    }
    return defaultValue;
  }

  public static String getEnv(String key) {
    String v = System.getProperty(key);
    if (Objects.nonNull(v)) {
      return v;
    }
    v = System.getenv(key);
    if (Objects.nonNull(v)) {
      return v;
    }

    return null;
  }
}
