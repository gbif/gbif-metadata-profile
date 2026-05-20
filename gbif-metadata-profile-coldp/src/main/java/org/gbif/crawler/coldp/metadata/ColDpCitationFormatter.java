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
package org.gbif.crawler.coldp.metadata;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;

public class ColDpCitationFormatter {

  private static final Logger LOG = LoggerFactory.getLogger(ColDpCitationFormatter.class);
  private static final Pattern DATASET_SUFFIX = Pattern.compile(" *\\[Data +set\\]");
  private static final Pattern MISSING_DATE_PREFIX = Pattern.compile("^ *\\(n\\.d\\.\\)\\. *");
  private static final ColDpCitationFormatter APA_TEXT = new ColDpCitationFormatter();

  private final SingleItemProvider provider = new SingleItemProvider();
  private final CSL csl;

  private ColDpCitationFormatter() {
    try {
      csl = new CSL(provider, "apa");
      csl.setOutputFormat("text");
    } catch (IOException e) {
      throw new IllegalStateException("APA CSL processor could not be created", e);
    }
  }

  public static String format(ColDpMetadata metadata) {
    return APA_TEXT.cite(metadata);
  }

  public synchronized String cite(ColDpMetadata metadata) {
    String key = provider.setData(toCslItem(metadata));
    if (isEmpty(provider.data)) {
      return null;
    }

    csl.registerCitationItems(key);
    try {
      String[] entries = csl.makeBibliography().getEntries();
      return entries == null || entries.length == 0 ? null : customCleaning(entries[0].trim());
    } catch (RuntimeException e) {
      LOG.warn("Failed to create COLDP citation", e);
      return null;
    }
  }

  static String customCleaning(String value) {
    return MISSING_DATE_PREFIX
        .matcher(DATASET_SUFFIX.matcher(value).replaceAll(""))
        .replaceFirst("")
        .trim();
  }

  private static CSLItemData toCslItem(ColDpMetadata metadata) {
    CSLItemDataBuilder builder =
        new CSLItemDataBuilder()
            .id(SingleItemProvider.KEY)
            .type(CSLType.DATASET)
            .title(metadata.getTitle())
            .author(toCslNames(metadata.getCreators()))
            .editor(toCslNames(metadata.getEditors()))
            .publisher(getPublisher(metadata))
            .version(metadata.getVersion())
            .URL(metadata.getUrl())
            .DOI(normalizeDoi(metadata.getDoi()));

    Integer issuedYear = parseIssuedYear(metadata.getIssued());
    if (issuedYear != null) {
      builder.issued(issuedYear);
    }

    return builder.build();
  }

  private static CSLName[] toCslNames(List<ColDpMetadata.Agent> agents) {
    return agents.stream()
        .map(ColDpCitationFormatter::toCslName)
        .filter(n -> n != null)
        .toArray(CSLName[]::new);
  }

  private static CSLName toCslName(ColDpMetadata.Agent agent) {
    if (agent == null || trimToNull(agent.getDisplayName()) == null) {
      return null;
    }

    CSLNameBuilder builder = new CSLNameBuilder();
    String literal = trimToNull(agent.getLiteral());
    if (literal != null) {
      builder.literal(literal);
      return builder.build();
    }
    String family = trimToNull(agent.getFamily());
    String given = trimToNull(agent.getGiven());
    if (family != null) {
      builder.family(family);
    }
    if (given != null) {
      builder.given(given);
    }
    if (family == null && given == null) {
      builder.literal(agent.getDisplayName().trim());
    }
    return builder.build();
  }

  private static String getPublisher(ColDpMetadata metadata) {
    if (metadata.getPublisher() == null) {
      return null;
    }
    return trimToNull(metadata.getPublisher().getDisplayName());
  }

  private static Integer parseIssuedYear(String issued) {
    if (trimToNull(issued) == null) {
      return null;
    }
    try {
      return OffsetDateTime.parse(issued).getYear();
    } catch (DateTimeParseException e) {
      try {
        return Integer.valueOf(issued.substring(0, 4));
      } catch (RuntimeException ex) {
        return null;
      }
    }
  }

  private static String normalizeDoi(String doi) {
    String value = trimToNull(doi);
    if (value == null) {
      return null;
    }
    return value.replaceFirst("^(?i)https?://(dx\\.)?doi\\.org/", "");
  }

  private static boolean isEmpty(CSLItemData data) {
    return data.getAuthor() == null
        && data.getEditor() == null
        && data.getIssued() == null
        && data.getDOI() == null
        && data.getPublisher() == null
        && data.getTitle() == null
        && data.getURL() == null
        && data.getVersion() == null;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  static class SingleItemProvider implements ItemDataProvider {

    private static final String KEY = "1";
    private CSLItemData data;

    String setData(CSLItemData data) {
      CSLItemDataBuilder builder = new CSLItemDataBuilder(data);
      builder.id(KEY);
      this.data = builder.build();
      return KEY;
    }

    @Override
    public CSLItemData retrieveItem(String id) {
      return data;
    }

    @Override
    public Collection<String> getIds() {
      return List.of(KEY);
    }
  }
}
