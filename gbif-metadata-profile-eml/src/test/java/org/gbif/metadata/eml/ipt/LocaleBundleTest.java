/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.metadata.eml.ipt;

import org.gbif.metadata.eml.ipt.model.LocaleBundle;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class can be used for unit testing {@link LocaleBundle}.
 */
public class LocaleBundleTest {

  @SuppressWarnings("UnusedReturnValue")
  private static LocaleBundle create(
      String failMsg, String language, String country, Charset charset) {
    LocaleBundle lb = null;
    try {
      lb = LocaleBundle.create(language, country, charset);
      if (failMsg != null) {
        fail(failMsg);
      } else {
        System.out.printf("Success: create(%s)\n", lb);
      }
    } catch (Exception e) {
      if (failMsg == null) {
        fail(e.getMessage());
      } else {
        System.out.printf("Fail: %s\n", e.getMessage());
      }
    }
    return lb;
  }

  @Test
  public final void testCreate() {
    Charset charset = StandardCharsets.UTF_8;
    create("Should fail with null params", null, null, null);
    create("Should fail with null language", null, "country", charset);
    create("Should fail with null country", "language", null, charset);
    create("Should fail with null charset", "language", "country", null);
    create("Should fail with empty country", "language", "", charset);
    create("Should fail with empty language", "", "country", charset);
    create(null, "language", "country", charset);
  }

  @Test
  public final void testEqualsObject() {
    assertEquals(
        LocaleBundle.create("l", "c", StandardCharsets.UTF_8),
        LocaleBundle.create("l", "c", StandardCharsets.UTF_8));
  }

  @Test
  public final void testGetCharset() {
    Charset charset = StandardCharsets.UTF_8;
    LocaleBundle lb = LocaleBundle.create("l", "c", charset);
    assertEquals(charset.displayName(), lb.getCharset());
  }

  @Test
  public final void testGetCountry() {
    Charset charset = StandardCharsets.UTF_8;
    String country = "C";
    LocaleBundle lb = LocaleBundle.create("l", country, charset);
    assertEquals(country, lb.getCountry());
  }

  @Test
  public final void testGetLanguage() {
    Charset charset = StandardCharsets.UTF_8;
    String language = "l";
    LocaleBundle lb = LocaleBundle.create(language, "c", charset);
    assertEquals(language, lb.getLanguage());
  }

  @Test
  public final void testHashCode() {
    assertEquals(
        LocaleBundle.create("l", "c", StandardCharsets.UTF_8).hashCode(),
        LocaleBundle.create("l", "c", StandardCharsets.UTF_8).hashCode());
  }

  @Test
  public final void testToString() {
    assertEquals(
        "LocaleBundle[country=C, language=l, charset=UTF-8]",
        LocaleBundle.create("l", "c", StandardCharsets.UTF_8).toString());
  }
}
