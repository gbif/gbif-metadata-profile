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

import org.gbif.api.model.registry.Dataset;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColDpMetadataParserTest {

  @Test
  public void parseJsonSupportsMixedAgentAndIdentifierShapes() throws Exception {
    String json =
        "{"
            + "\"title\":\"Checklist A\","
            + "\"description\":\"Test metadata\","
            + "\"doi\":\"10.1234/example\","
            + "\"identifier\":[\"doi:10.1234/example\", {\"prefix\":\"local\",\"value\":\"abc\"}],"
            + "\"contact\":{\"given\":\"Jane\",\"family\":\"Doe\",\"email\":\"jane@example.org\"},"
            + "\"creator\":[\"Literal Creator\", {\"organization\":\"GBIF\"}],"
            + "\"sources\":[{\"title\":\"Checklist source\",\"path\":\"https://example.org/source\",\"email\":\"source@example.org\"}],"
            + "\"publisher\":{\"organisation\":\"Catalogue of Life\",\"url\":\"https://www.catalogueoflife.org\"}"
            + "}";

    ColDpMetadata metadata = ColDpMetadataParser.buildMetadata(json.getBytes(StandardCharsets.UTF_8));
    Dataset dataset = ColDpMetadataParser.build(json.getBytes(StandardCharsets.UTF_8));

    assertEquals("Checklist A", metadata.getTitle());
    assertEquals("Test metadata", metadata.getDescription());
    assertEquals("10.1234/example", metadata.getDoi());
    assertTrue(metadata.hasAnyMetadata());
    assertEquals("Checklist A", dataset.getTitle());
    assertEquals("Test metadata", dataset.getDescription());
    assertNotNull(dataset.getCitation());
    assertNotNull(dataset.getCitation().getText());

    assertEquals(2, metadata.getIdentifiers().size());
    assertEquals("doi:10.1234/example", metadata.getIdentifiers().get(0).asString());
    assertEquals("local:abc", metadata.getIdentifiers().get(1).asString());

    assertEquals(1, metadata.getContacts().size());
    assertEquals("Jane Doe", metadata.getContacts().get(0).getDisplayName());

    assertEquals(2, metadata.getCreators().size());
    assertEquals("Literal Creator", metadata.getCreators().get(0).getDisplayName());
    assertEquals("GBIF", metadata.getCreators().get(1).getPrimaryOrganization());
    assertEquals(1, metadata.getSources().size());
    assertEquals("Checklist source", metadata.getSources().get(0).getTitle());
    assertEquals("https://example.org/source", metadata.getSources().get(0).getPath());
    assertEquals("source@example.org", metadata.getSources().get(0).getEmail());

    assertNotNull(metadata.getPublisher());
    assertEquals("Catalogue of Life", metadata.getPublisher().getPrimaryOrganization());
    assertNotNull(metadata.getPublisher().getParsedUrl());
    assertEquals(1, dataset.getBibliographicCitations().size());
    assertEquals(
        "Checklist source. https://example.org/source. source@example.org",
        dataset.getBibliographicCitations().get(0).getText());
    assertEquals(
        "https://example.org/source", dataset.getBibliographicCitations().get(0).getIdentifier());
    assertTrue(dataset.getBibliographicCitations().get(0).isCitationProvidedBySource());
  }

  @Test
  public void parseJsonObjectIdentifiersAndInvalidAgentDropsEmptyObjects() throws Exception {
    String json =
        "{"
            + "\"identifier\":{\"doi\":\"10.48580/dfg7\",\"url\":\"https://example.org/id\"},"
            + "\"editor\":{\"given\":\" \",\"family\":\" \"},"
            + "\"contributor\":{\"literal\":\"\"}"
            + "}";

    ColDpMetadata metadata = ColDpMetadataParser.buildMetadata(json.getBytes(StandardCharsets.UTF_8));

    assertEquals(2, metadata.getIdentifiers().size());
    assertEquals("doi:10.48580/dfg7", metadata.getIdentifiers().get(0).asString());
    assertEquals("url:https://example.org/id", metadata.getIdentifiers().get(1).asString());
    assertTrue(metadata.getEditors().isEmpty());
    assertTrue(metadata.getContributors().isEmpty());
    assertNull(metadata.getPublisher());
  }

  @Test
  public void citationCleaningRemovesDatasetSuffix() {
    String raw = "(n.d.). Example title [Data set]";
    assertEquals("Example title", ColDpCitationFormatter.customCleaning(raw));
  }
}
