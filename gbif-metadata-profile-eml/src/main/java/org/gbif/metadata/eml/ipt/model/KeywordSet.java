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
package org.gbif.metadata.eml.ipt.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple POJO container for an ordered list of keywords and optionally, a thesaurus to which those keywords are
 * associated
 */
public class KeywordSet implements Serializable {

  private static final long serialVersionUID = -421915165032215809L;

  /**
   * The name of the official keyword thesaurus from which keyword was derived.
   *
   * @see <a href="https://eml.ecoinformatics.org/schema/eml-resource_xsd.html#ResourceGroup_ResourceGroup_keywordSet_keywordThesaurus">EML
   *      Resource
   *      keywordThesaurus element</a>
   */
  private String keywordThesaurus;

  /**
   * A keyword or key phrase that concisely describes the resource or is related to the resource. Each keyword field
   * should contain one and only one keyword (i.e., keywords should not be separated by commas or other delimiters).
   *
   * @see <a href="https://eml.ecoinformatics.org/schema/eml-resource_xsd.html#ResourceGroup_ResourceGroup_keywordSet_keyword">EML Resource keyword
   *      element</a>
   */
  private List<String> keywords = new ArrayList<>();

  /**
   * Default constructor required by Struts2
   */
  public KeywordSet() {}

  /**
   * @param keywords To initialise with
   */
  public KeywordSet(List<String> keywords) {
    this.keywords = keywords;
  }

  /**
   * @param keywords  To initialise with
   * @param thesaurus To initialise with
   */
  public KeywordSet(List<String> keywords, String thesaurus) {
    this.keywords = keywords;
    keywordThesaurus = thesaurus;
  }

  /**
   * Adds a keyword to the list. This was added to simplify the Digester based rules definitions
   *
   * @param keyword To add
   */
  public void add(String keyword) {
    keywords.add(keyword);
  }

  /**
   * @return the keywordThesaurus
   */
  public String getKeywordThesaurus() {
    return keywordThesaurus;
  }

  /**
   * @param keywordThesaurus the keywordThesaurus to set
   */
  public void setKeywordThesaurus(String keywordThesaurus) {
    this.keywordThesaurus = keywordThesaurus;
  }

  /**
   * @return the keywords
   */
  public List<String> getKeywords() {
    return keywords;
  }

  /**
   * @param keywords the keywords to set
   */
  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

  public void setKeywordsString(String keywords) {
    setKeywordsString(keywords, ",");
  }

  /**
   * @param separator the separator to use between keywords
   */
  public void setKeywordsString(String keywords, String separator) {
    this.keywords.clear();
    if (keywords != null) {
      for (String k : StringUtils.split(keywords, separator)) {
        Optional.ofNullable(k).map(StringUtils::trimToNull).ifPresent(this.keywords::add);
      }
    }
  }

  public String getKeywordsString() {
    return getKeywordsString(", ");
  }

  public String getKeywordsString(String separator) {
    StringBuilder sb = new StringBuilder();
    boolean b = false;
    for (String keyword : keywords) {
      if (b && separator != null) {
        sb.append(separator).append(keyword);
      } else {
        sb.append(keyword);
        b = true;
      }
    }
    return sb.toString();
  }
}
