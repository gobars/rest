package com.github.gobars.rest;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class PTypeRef<T> {
  private final Type type;

  protected PTypeRef() {
    Class<?> ptypeRefSubclass = findPTypeRefSubclass(getClass());
    Type type = ptypeRefSubclass.getGenericSuperclass();
    Assert.isInstanceOf(ParameterizedType.class, type, "Type must be a parameterized type");
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    Assert.isTrue(actualTypeArguments.length == 1, "Number of type arguments must be 1");
    this.type = actualTypeArguments[0];
  }

  public Type getType() {
    return this.type;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other
        || (other instanceof PTypeRef && this.type.equals(((PTypeRef<?>) other).type)));
  }

  @Override
  public int hashCode() {
    return this.type.hashCode();
  }

  @Override
  public String toString() {
    return "PTypeRef<" + this.type + ">";
  }

  private static Class<?> findPTypeRefSubclass(Class<?> child) {
    Class<?> parent = child.getSuperclass();
    if (Object.class == parent) {
      throw new IllegalStateException("Expected PTypeRef superclass");
    } else if (PTypeRef.class == parent) {
      return child;
    } else {
      return findPTypeRefSubclass(parent);
    }
  }
}
