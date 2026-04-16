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
package org.gbif.crawler.dwcdp.metadata;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Builds a compact fallback citation for DwC-DP metadata. */
public class DwcDpCitationFormatter {

  private DwcDpCitationFormatter() {}

  static String format(DwcDpMetadata metadata, Dataset dataset) {
    String title = trimToNull(dataset.getTitle());
    if (title == null) {
      return null;
    }

    String authors = joinAuthors(dataset.getContacts());
    String year = null;
    if (dataset.getPubDate() != null) {
      year =
          DateTimeFormatter.ofPattern("yyyy")
              .withZone(ZoneOffset.UTC)
              .format(dataset.getPubDate().toInstant());
    }

    String doi = dataset.getDoi() == null ? null : trimToNull(dataset.getDoi().getDoiName());
    String homepage = dataset.getHomepage() == null ? null : dataset.getHomepage().toString();
    String version = trimToNull(metadata.getVersion());

    StringBuilder citation = new StringBuilder();
    if (authors != null) {
      citation.append(authors);
      citation.append(". ");
    }
    citation.append(title);
    if (year != null) {
      citation.append(" (").append(year).append(")");
    }
    if (version != null) {
      citation.append(". Version ").append(version);
    }
    if (doi != null) {
      citation.append(". https://doi.org/").append(doi);
    } else if (homepage != null) {
      citation.append(". ").append(homepage);
    }
    return citation.toString().trim();
  }

  private static String joinAuthors(List<Contact> contacts) {
    List<String> names =
        contacts.stream()
            .map(DwcDpCitationFormatter::contactName)
            .filter(n -> n != null)
            .distinct()
            .collect(Collectors.toList());
    if (names.isEmpty()) {
      return null;
    }
    if (names.size() > 3) {
      return names.get(0) + " et al";
    }
    return String.join(", ", names);
  }

  private static String contactName(Contact c) {
    String first = trimToNull(c.getFirstName());
    String last = trimToNull(c.getLastName());
    if (first != null && last != null) {
      return first + " " + last;
    }
    if (last != null) {
      return last;
    }
    if (first != null) {
      return first;
    }
    return trimToNull(c.getOrganization());
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
