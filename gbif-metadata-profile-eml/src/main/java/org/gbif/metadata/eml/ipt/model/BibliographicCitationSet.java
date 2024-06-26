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
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Simple POJO container for an ordered list of bibliographic citations.
 */
public class BibliographicCitationSet implements Serializable {

  /**
   * Generated
   */
  private static final long serialVersionUID = -406468584517868175L;

  /**
   * A keyword or key phrase that concisely describes the resource or is related to the resource. Each keyword field
   * should contain one and only one keyword (i.e., keywords should not be separated by commas or other delimiters).
   *
   * @see <a href="http://knb.ecoinformatics.org/software/eml/eml-2.1.0/eml-resource.html#keyword">EML Resource keyword
   *      element</a>
   */
  protected List<Citation> bibliographicCitations = new ArrayList<>();

  /**
   * Default constructor required by Struts2
   */
  public BibliographicCitationSet() {}

  /**
   * @param citations to initialise with
   */
  public BibliographicCitationSet(List<Citation> citations) {
    this.bibliographicCitations = citations;
  }

  public List<Citation> getBibliographicCitations() {
    return bibliographicCitations;
  }

  public void setBibliographicCitations(List<Citation> citations) {
    this.bibliographicCitations = citations;
  }

  /**
   * Adds a bibliographic citation to the list. This was added to simplify the Digester based rules definitions
   *
   * @param citation to add
   */
  public void add(String citation, String identifier) {
    bibliographicCitations.add(new Citation(citation, identifier));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BibliographicCitationSet that = (BibliographicCitationSet) o;
    return Objects.equals(bibliographicCitations, that.bibliographicCitations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bibliographicCitations);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BibliographicCitationSet.class.getSimpleName() + "[", "]")
        .add("bibliographicCitations=" + bibliographicCitations)
        .toString();
  }
}
