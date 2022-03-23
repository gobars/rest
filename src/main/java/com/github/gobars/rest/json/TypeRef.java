package com.github.gobars.rest.json;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 类型
 * @author liujinliang5
 * @version 1.0
 */
public abstract class TypeRef<T> extends TypeReference<T> {

  protected TypeRef() {
  }

  public static <T> TypeRef<T> getTypeRef() {
    return new TypeRef<T>() {
    };
  }
}
