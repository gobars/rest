package com.github.gobars.rest.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author liujinliang5
 * @version 1.0
 */
public final class JsonMapper {

  private static final Logger log = LoggerFactory.getLogger(JsonMapper.class);
  private static final JsonMapper normal;
  private static final JsonMapper nonnull;

  static {
    final ObjectMapper nrml = new ObjectMapper();
    nrml.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    nrml.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    nrml.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    nrml.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    normal = new JsonMapper(nrml);

    final ObjectMapper nn = new ObjectMapper();
    nn.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    nn.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    nn.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    nn.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    nn.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    nonnull = new JsonMapper(nn);
  }

  private final ObjectMapper objMapper;

  private JsonMapper(final ObjectMapper objMapper) {
    this.objMapper = objMapper;
  }


  public static JsonMapper forNormal() {
    return normal;
  }

  public static JsonMapper forNonNull() {
    return nonnull;
  }

  public <T> T fromJson(final String json, final Class<T> clazz) throws JsonException {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return this.objMapper.readValue(json, clazz);
    } catch (final IOException e) {
      throw new JsonException(e);
    }
  }

  public <T> T tryFromJson(final String json, final Class<T> clazz) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return this.objMapper.readValue(json, clazz);
    } catch (final IOException e) {
      log.error(json + " of " + clazz, e);
    }
    return null;
  }

  /**
   * <pre>
   * Two ways to get an instance of {@link TypeRef}:
   *
   * 1. new TypeRef&lt;GenericType&lt;AnotherType&gt;&gt;() {};
   *
   * 2. class ConcreteSubClass extends TypeRef&lt;GenericType&lt;AnotherType&gt;&gt; {
   *    }
   *
   *    new ConcreteSubClass();
   * </pre>
   *
   * Examples:
   *
   * <pre>
   * 1. List&lt;Point&gt; points = JsonMapper.forNormal().tryFromJson(json, new TypeRef&lt;List&lt;Point&gt;&gt;(){});
   *
   * 2. class ListPointTypeRef extends TypeRef&lt;List&lt;Point&gt;&gt; {
   *        public static final ListPointTypeRef THE_TYPE_REF = new ListPointTypeRef();
   *    }
   *
   *    List&lt;Point&gt; points = JsonMapper.forNormal().tryFromJson(json, ListPointTypeRef.THE_TYPE_REF);
   * </pre>
   */
  @SuppressWarnings("unchecked")
  public <T> T tryFromJson(final String json, final TypeRef<T> typeRef) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      // The (T) is for jdk 1.6.0_20.
      return (T) this.objMapper.readValue(json, typeRef);
    } catch (final IOException e) {
      log.error(json + " of " + typeRef, e);

    }
    return null;
  }

  public <T> T tryFromJson(final String json, final Type type) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      // The (T) is for jdk 1.6.0_20.
      return (T) this.objMapper.readValue(json, new TypeReference<Object>() {
        @Override
        public Type getType() {
          return type;
        }
      });
    } catch (final IOException e) {
      log.error(json + " of " + type, e);

    }
    return null;
  }

  public String toJson(final Object obj) throws JsonException {
    if (obj == null) {
      return null;
    }
    try {
      return this.objMapper.writeValueAsString(obj);
    } catch (final JsonProcessingException e) {
      throw new JsonException(e);
    }
  }

  public String tryToJson(final Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return this.objMapper.writeValueAsString(obj);
    } catch (final JsonProcessingException e) {
      log.error(obj.toString(), e);
    }
    return null;
  }
}
