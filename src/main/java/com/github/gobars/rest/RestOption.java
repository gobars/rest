package com.github.gobars.rest;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RestOption implements Cloneable {
  @Getter private String proxy;
  @Getter private String method;
  @Getter private String bizName;
  @Getter private Type type;
  @Getter private Class clazz;
  @Getter private Object requestBody;
  @Getter private String url;
  // 下载文件输出流
  @Getter private OutputStream download;
  // 上传文件名称
  @Getter String fileName;
  // 上传文件输入流
  @Getter InputStream upload;
  @Getter private OkStatus okStatus = new OkStatus() {};
  @Getter private OkBiz okBiz = new OkBiz() {};
  @Getter private Map<String, List<String>> moreHeaders;
  @Getter private DoneBiz doneBiz = (success, rt) -> {};
  @Getter private boolean dump = true;

  @Override
  @SneakyThrows
  protected RestOption clone() {
    return (RestOption) super.clone();
  }

  public RestOption upload(String fileName, InputStream upload) {
    RestOption copy = this.clone();
    copy.fileName = fileName;
    copy.upload = upload;
    return copy;
  }

  public RestOption download(OutputStream download) {
    RestOption copy = this.clone();
    copy.download = download;
    return copy;
  }

  public RestOption GET() {
    return method("GET");
  }

  public RestOption POST() {
    return method("POST");
  }

  public RestOption proxy(String proxy) {
    RestOption copy = this.clone();
    copy.proxy = proxy;
    return copy;
  }

  public RestOption method(String method) {
    RestOption copy = this.clone();
    copy.method = method;
    return copy;
  }

  public <T> RestOption type(TypeRef<T> ref) {
    return type(ref.getType());
  }

  public RestOption type(Type type) {
    RestOption copy = this.clone();
    copy.type = type;
    return copy;
  }

  public RestOption clazz(Class clazz) {
    RestOption copy = this.clone();
    copy.clazz = clazz;
    return copy;
  }

  public RestOption req(Object requestBody) {
    RestOption copy = this.clone();
    copy.requestBody = requestBody;
    return copy;
  }

  @SneakyThrows
  public RestOption url(String... urlParts) {
    URI parent = null;
    for (String p : urlParts) {
      if (parent == null) {
        parent = new URI(p);
      } else {
        parent = parent.resolve(p);
      }
    }

    RestOption copy = this.clone();
    copy.url = parent.toString();
    return copy;
  }

  public RestOption okStatus(OkStatus okStatus) {
    RestOption copy = this.clone();
    copy.okStatus = okStatus;
    return copy;
  }

  public RestOption bizName(String bizName) {
    RestOption copy = this.clone();
    copy.bizName = bizName;
    return copy;
  }

  public RestOption doneBiz(DoneBiz doneBiz) {
    RestOption copy = this.clone();
    copy.doneBiz = doneBiz;
    return copy;
  }

  public RestOption okBiz(OkBiz okBiz) {
    RestOption copy = this.clone();
    copy.okBiz = okBiz;
    return copy;
  }

  public RestOption dump(boolean dump) {
    RestOption copy = this.clone();
    copy.dump = dump;
    return copy;
  }

  public RestOption headers(Map<String, List<String>> moreHeaders) {
    RestOption copy = this.clone();

    if (copy.moreHeaders == null) {
      copy.moreHeaders = new LinkedHashMap<>(moreHeaders);
      return copy;
    }

    for (val entry : moreHeaders.entrySet()) {
      copy.moreHeaders
          .computeIfAbsent(entry.getKey(), k -> new LinkedList<>())
          .addAll(entry.getValue());
    }

    return copy;
  }
}
