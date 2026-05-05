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
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DwcDpMetadataParserTest {

  @Test
  public void parseFullDescriptorMapsCoreAndExtendedFields() throws Exception {
    String json =
        "{"
            + "\"name\":\"dwcdp-demo\","
            + "\"id\":\"10.48580/dwcdp-demo\","
            + "\"profile\":\"https://rs.gbif.org/profile/dwc-dp.json\","
            + "\"title\":\"DwC-DP demo dataset\","
            + "\"description\":\"A demo Data Package\","
            + "\"homepage\":\"https://example.org/dataset\","
            + "\"version\":\"1.4.0\","
            + "\"created\":\"2025-09-17T00:00:00Z\","
            + "\"language\":\"en\","
            + "\"keywords\":[\"dwc\",\"biodiversity\"],"
            + "\"identifier\":[\"doi:10.48580/dwcdp-demo\",{\"type\":\"local\",\"value\":\"abc-123\"}],"
            + "\"licenses\":[{\"name\":\"CC-BY-4.0\",\"path\":\"https://creativecommons.org/licenses/by/4.0/\"}],"
            + "\"contributors\":["
            + "{\"title\":\"Jane Doe\",\"email\":\"jane@example.org\",\"path\":\"https://example.org/jane\",\"role\":\"author\"},"
            + "{\"title\":\"GBIF\",\"role\":\"publisher\"}"
            + "],"
            + "\"sources\":[{\"title\":\"Source Org\",\"path\":\"https://example.org/source\",\"email\":\"source@example.org\"}],"
            + "\"resources\":[{\"name\":\"event\",\"path\":\"event.csv\"}]"
            + "}";

    DwcDpMetadata metadata = DwcDpMetadataParser.buildMetadata(json.getBytes(StandardCharsets.UTF_8));
    Dataset dataset = DwcDpMetadataParser.build(json.getBytes(StandardCharsets.UTF_8));

    assertEquals("dwcdp-demo", metadata.getName());
    assertEquals(1, metadata.getSources().size());
    assertEquals("Source Org", metadata.getSources().get(0).getTitle());
    assertEquals("https://example.org/source", metadata.getSources().get(0).getPath());
    assertEquals("source@example.org", metadata.getSources().get(0).getEmail());
    assertEquals("DwC-DP demo dataset", dataset.getTitle());
    assertEquals("dwcdp-demo", dataset.getShortName());
    assertEquals("A demo Data Package", dataset.getDescription());
    assertEquals("1.4.0", dataset.getVersion());
    assertNotNull(dataset.getHomepage());
    assertEquals(Language.ENGLISH, dataset.getLanguage());
    assertEquals(Language.ENGLISH, dataset.getDataLanguage());
    assertNotNull(dataset.getPubDate());
    assertNotNull(dataset.getDoi());
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
    assertFalse(dataset.getIdentifiers().isEmpty());
    assertTrue(dataset.getIdentifiers().stream().allMatch(id -> id != null && id.getType() != null));
    assertTrue(dataset.getIdentifiers().stream().anyMatch(id -> id.getType() == IdentifierType.DOI));
    assertFalse(
        dataset.getIdentifiers().stream()
            .anyMatch(
                id ->
                    id != null
                        && id.getIdentifier() != null
                        && id.getIdentifier().startsWith("source:")));
    assertFalse(
        dataset.getIdentifiers().stream()
            .anyMatch(id -> id != null && id.getIdentifier() != null && id.getIdentifier().startsWith("license:")));

    assertEquals(1, dataset.getKeywordCollections().size());
    assertEquals(2, dataset.getKeywordCollections().get(0).getKeywords().size());
    assertTrue(dataset.getKeywordCollections().get(0).getKeywords().contains("dwc"));

    Contact author = dataset.getContacts().get(0);
    assertEquals("Jane Doe", author.getLastName());
    assertEquals(ContactType.AUTHOR, author.getType());
    assertTrue(author.getPosition().contains("author"));

    Contact publisher = dataset.getContacts().get(1);
    assertEquals("GBIF", publisher.getLastName());
    assertEquals(ContactType.PUBLISHER, publisher.getType());
    assertTrue(publisher.getPosition().contains("publisher"));
    assertEquals(2, dataset.getContacts().size());

    assertEquals(1, dataset.getBibliographicCitations().size());
    assertEquals(
        "Source Org. https://example.org/source. source@example.org",
        dataset.getBibliographicCitations().get(0).getText());
    assertEquals(
        "https://example.org/source", dataset.getBibliographicCitations().get(0).getIdentifier());
    assertTrue(dataset.getBibliographicCitations().get(0).isCitationProvidedBySource());

    assertNotNull(dataset.getCitation());
    assertTrue(dataset.getCitation().getText().contains("DwC-DP demo dataset"));
  }

  @Test
  public void parseMinimalDescriptorIsAccepted() throws Exception {
    String json = "{\"resources\":[{\"name\":\"occurrence\",\"path\":\"occurrence.csv\"}]}";

    DwcDpMetadata metadata = DwcDpMetadataParser.buildMetadata(json.getBytes(StandardCharsets.UTF_8));
    Dataset dataset = DwcDpMetadataParser.build(json.getBytes(StandardCharsets.UTF_8));

    assertNotNull(metadata);
    assertNotNull(dataset);
    assertTrue(dataset.getIdentifiers().isEmpty());
    assertTrue(dataset.getContacts().isEmpty());
  }

  @Test
  public void parseCustomIdentifierMapsToUnknownType() throws Exception {
    String json =
        "{"
            + "\"identifier\":\"dwc-data-package\","
            + "\"resources\":[{\"name\":\"occurrence\",\"path\":\"occurrence.csv\"}]"
            + "}";

    Dataset dataset = DwcDpMetadataParser.build(json.getBytes(StandardCharsets.UTF_8));

    Identifier customId =
        dataset.getIdentifiers().stream()
            .filter(id -> "dwc-data-package".equals(id.getIdentifier()))
            .findFirst()
            .orElseThrow();
    assertEquals(IdentifierType.UNKNOWN, customId.getType());
  }

  @Test
  public void duplicateContributorsProduceSingleContact() throws Exception {
    String json =
        "{"
            + "\"contributors\":["
            + "{\"title\":\"Jane Doe\",\"email\":\"jane@example.org\",\"path\":\"https://example.org/jane\",\"role\":\"author\"},"
            + "{\"title\":\"Jane Doe\",\"email\":\"jane@example.org\",\"path\":\"https://example.org/jane\",\"role\":\"author\"}"
            + "],"
            + "\"resources\":[{\"name\":\"occurrence\",\"path\":\"occurrence.csv\"}]"
            + "}";

    Dataset dataset = DwcDpMetadataParser.build(json.getBytes(StandardCharsets.UTF_8));

    assertEquals(1, dataset.getContacts().size());
    Contact contact = dataset.getContacts().get(0);
    assertEquals("Jane Doe", contact.getLastName());
    assertEquals(ContactType.AUTHOR, contact.getType());
    assertEquals(1, contact.getPosition().size());
    assertTrue(contact.getPosition().contains("author"));
  }

  @Test
  public void samePersonWithDifferentRolesProducesSingleContact() throws Exception {
    String json =
        "{"
            + "\"contributors\":["
            + "{\"title\":\"Jane Doe\",\"email\":\"jane@example.org\",\"path\":\"https://example.org/jane\",\"role\":\"author\"},"
            + "{\"title\":\"Jane Doe\",\"email\":\"jane@example.org\",\"path\":\"https://example.org/jane\",\"role\":\"contributor\"}"
            + "],"
            + "\"resources\":[{\"name\":\"occurrence\",\"path\":\"occurrence.csv\"}]"
            + "}";

    Dataset dataset = DwcDpMetadataParser.build(json.getBytes(StandardCharsets.UTF_8));

    assertEquals(1, dataset.getContacts().size());
    Contact contact = dataset.getContacts().get(0);
    assertEquals("Jane Doe", contact.getLastName());
    assertEquals(ContactType.AUTHOR, contact.getType());
    assertEquals(2, contact.getPosition().size());
    assertTrue(contact.getPosition().contains("author"));
    assertTrue(contact.getPosition().contains("contributor"));
  }

  @Test
  public void missingResourcesFailsValidation() {
    String json = "{\"title\":\"invalid\"}";
    IOException exception =
        assertThrows(
            IOException.class, () -> DwcDpMetadataParser.buildMetadata(json.getBytes(StandardCharsets.UTF_8)));
    assertTrue(exception.getMessage().contains("resources"));
  }

  @Test
  public void malformedJsonFailsValidation() {
    String json = "{title:}";
    assertThrows(IOException.class, () -> DwcDpMetadataParser.buildMetadata(json.getBytes(StandardCharsets.UTF_8)));
  }
}
