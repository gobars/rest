package com.github.gobars.rest.json;

import java.io.IOException;

/**
 * @author 刘金良
 * @version 1.0
 */
public class JsonException extends IOException {

  private static final long serialVersionUID = -6774194923869125874L;

  JsonException(final IOException cause) {
    super(cause);
  }
}
