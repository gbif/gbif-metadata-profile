/*
 * Copyright 2009 GBIF.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.metadata.eml;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Locale;

import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class can be used to bundle a {@link Locale} with a {@link Charset} encoding.
 * Note that this class is immutable. New instances can be created using the static create method.
 */
public class LocaleBundle implements Serializable {

  private static final long serialVersionUID = 4602721372582246629L;

  private final Locale locale;
  private final Charset charset;

  /**
   * Creates a new LocaleBundle instance. Throws {@link NullPointerException} or {@link IllegalArgumentException} if
   * any
   * of the parameters are null or if the language or country parameters are the empty string.
   *
   * @param language the language
   * @param country  the country
   * @param charset  the character set
   *
   * @return new instance of LocalBundle
   */
  public static LocaleBundle create(String language, String country, Charset charset) {
    checkNotNull(language, "Language was null");
    checkArgument(!language.trim().isEmpty(), "Language was empty");
    checkNotNull(country, "Country was null");
    checkArgument(!country.trim().isEmpty(), "Country was empty");
    checkNotNull(charset, "Charset was null");
    return new LocaleBundle(language, country, charset);
  }

  private LocaleBundle(String language, String country, Charset charset) {
    locale = new Locale(language, country);
    this.charset = charset;
  }

  public String getCharset() {
    return charset.displayName();
  }

  public String getCountry() {
    return locale.getCountry();
  }

  public String getLanguage() {
    return locale.getLanguage();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LocaleBundle other = (LocaleBundle) obj;
    return Objects.equal(this.locale, other.locale) && Objects.equal(this.charset, other.charset);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(locale, charset);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).
      add("country", getCountry()).
      add("language", getLanguage()).
      add("charset", charset).
      toString();
  }

}
