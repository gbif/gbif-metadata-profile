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

import com.google.common.base.Objects;

/**
 * This class can be used to encapsulate information about physical data.
 */
public class PhysicalData implements Serializable {

  /**
   * Generated
   */
  private static final long serialVersionUID = 1209461796079665955L;

  /**
   * This element contains the name of the character encoding. This is typically ASCII or UTF-8, or one of the other
   * common encodings.
   *
   * @see <a href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-physical.html#characterEncoding">EML
   *      Physical
   *      characterEncoding keyword</a>
   */
  private String charset;

  /**
   * The URL of the resource that is available online.
   *
   * @see <a href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-physical.html#url">EML Physical url
   *      keyword</a>
   */
  private String distributionUrl;

  /**
   * Name of the format of the data object.
   *
   * @see <a href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-physical.html#formatName">EML Physical
   *      formatName keyword</a>
   */
  private String format;

  /**
   * Version of the format of the data object.
   *
   * @see <a href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-physical.html#formatVersion">">EML Physical
   *      formatVersion keyword</a>
   */
  private String formatVersion;

  /**
   * The name of the data object, usually a file in a file system or that is accessible on the network.
   *
   * @see <a href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-physical.html#objectName">EML Physical
   *      objectName keyword</a>
   */
  private String name;

  /**
   * Required by Struts2
   */
  public PhysicalData() {
  }

  public String getCharset() {
    return charset;
  }

  public String getDistributionUrl() {
    return distributionUrl;
  }

  public String getFormat() {
    return format;
  }

  public String getFormatVersion() {
    return formatVersion;
  }

  public String getName() {
    return name;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public void setDistributionUrl(String distributionUrl) {
    this.distributionUrl = distributionUrl;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setFormatVersion(String formatVersion) {
    this.formatVersion = formatVersion;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(charset, distributionUrl, format, formatVersion, name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final PhysicalData other = (PhysicalData) obj;
    return Objects.equal(this.charset, other.charset) && Objects.equal(this.distributionUrl, other.distributionUrl)
           && Objects.equal(this.format, other.format) && Objects.equal(this.formatVersion, other.formatVersion)
           && Objects.equal(this.name, other.name);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).
      add("charset", charset).
      add("distributionUrl", distributionUrl).
      add("format", format).
      add("formatVersion", formatVersion).
      add("name", name).
      toString();
  }
}
