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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.common.parsers.LicenseParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Parser for the current ColDP metadata shape with limited backward-compatible handling for agent
 * fields that may appear as single objects, arrays, or simple strings.
 */
public class ColDpMetadataParser {

  private static final Logger LOG = LoggerFactory.getLogger(ColDpMetadataParser.class);
  private static final List<String> YAML_NAMES = List.of("metadata.yaml", "metadata.yml");
  private static final String JSON_NAME = "metadata.json";

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  private final ObjectMapper jsonMapper = new ObjectMapper();

  /**
   * Builds Dataset from raw metadata document bytes (JSON or YAML).
   *
   * @param metadataDocument metadata document bytes
   * @return parsed Dataset
   * @throws IOException if parsing fails
   */
  public static Dataset build(byte[] metadataDocument) throws IOException {
    return toDataset(buildMetadata(metadataDocument));
  }

  /**
   * Builds metadata from raw metadata document bytes (JSON or YAML).
   *
   * @param metadataDocument metadata document bytes
   * @return parsed ColDP metadata
   * @throws IOException if parsing fails
   */
  public static ColDpMetadata buildMetadata(byte[] metadataDocument) throws IOException {
    ColDpMetadataParser parser = new ColDpMetadataParser();
    JsonNode root = parser.parseDocument(metadataDocument);
    return parser.parseTree(root);
  }

  /**
   * Builds Dataset from a CoLDP archive by reading metadata.yaml/metadata.yml or metadata.json.
   *
   * @param archive CoLDP archive
   * @return parsed Dataset
   * @throws IOException if no metadata document is found or parsing fails
   */
  public static Dataset build(File archive) throws IOException {
    return toDataset(new ColDpMetadataParser().parseArchive(archive));
  }

  public ColDpMetadata parseArchive(File archive) throws IOException {
    try (ZipFile zipFile = new ZipFile(archive)) {
      ZipEntry metadataEntry = findEntry(zipFile, YAML_NAMES);
      if (metadataEntry != null) {
        try (InputStream inputStream = zipFile.getInputStream(metadataEntry)) {
          return parseYaml(inputStream);
        }
      }

      metadataEntry = findEntry(zipFile, List.of(JSON_NAME));
      if (metadataEntry != null) {
        LOG.info("Falling back to metadata.json for archive {}", archive.getName());
        try (InputStream inputStream = zipFile.getInputStream(metadataEntry)) {
          return parseJson(inputStream);
        }
      }
    }

    throw new IOException(
        "No metadata.yaml or metadata.json found in " + archive.getAbsolutePath());
  }

  public ColDpMetadata parseYaml(InputStream inputStream) throws IOException {
    return parseTree(yamlMapper.readTree(inputStream));
  }

  public ColDpMetadata parseJson(InputStream inputStream) throws IOException {
    return parseTree(jsonMapper.readTree(inputStream));
  }

  private JsonNode parseDocument(byte[] metadataDocument) throws IOException {
    if (metadataDocument == null || metadataDocument.length == 0) {
      throw new IOException("Metadata document is empty");
    }
    int first = firstNonWhitespaceByte(metadataDocument);
    if (first == -1) {
      throw new IOException("Metadata document is empty");
    }
    if (first == '{' || first == '[') {
      return jsonMapper.readTree(metadataDocument);
    }
    return yamlMapper.readTree(metadataDocument);
  }

  private int firstNonWhitespaceByte(byte[] data) {
    for (byte datum : data) {
      int value = datum & 0xFF;
      if (!Character.isWhitespace(value)) {
        return value;
      }
    }
    return -1;
  }

  ColDpMetadata parseTree(JsonNode root) {
    ColDpMetadata metadata = new ColDpMetadata();
    metadata.setTitle(asText(root.get("title")));
    metadata.setDescription(asText(root.get("description")));
    metadata.setDoi(asText(root.get("doi")));
    metadata.setIssued(asText(root.get("issued")));
    metadata.setVersion(asText(root.get("version")));
    metadata.setLicense(asText(root.get("license")));
    metadata.setUrl(asText(root.get("url")));
    metadata.setLogo(asText(root.get("logo")));
    metadata.setLanguage(asText(firstPresent(root, "language", "defaultLanguage")));
    metadata.setNotes(asText(root.get("notes")));
    metadata.setAlias(asText(root.get("alias")));
    metadata.setTaxonomicScope(asText(root.get("taxonomicScope")));
    metadata.setGeographicScope(asText(root.get("geographicScope")));
    metadata.setTemporalScope(asText(root.get("temporalScope")));
    metadata.getIdentifiers().addAll(readIdentifiers(root.get("identifier")));

    addAgents(root.get("contact"), metadata.getContacts()::add);
    addAgents(root.get("creator"), metadata.getCreators()::add);
    addAgents(root.get("editor"), metadata.getEditors()::add);
    addAgents(root.get("contributor"), metadata.getContributors()::add);
    addSources(firstPresent(root, "source", "sources"), metadata.getSources()::add);
    metadata.setPublisher(readAgent(root.get("publisher")));
    return metadata;
  }

  private List<ColDpMetadata.Identifier> readIdentifiers(JsonNode identifiersNode) {
    if (identifiersNode == null || identifiersNode.isNull()) {
      return List.of();
    }

    List<ColDpMetadata.Identifier> identifiers = new ArrayList<>();
    if (identifiersNode.isArray()) {
      for (JsonNode child : identifiersNode) {
        ColDpMetadata.Identifier identifier = readIdentifier(child);
        if (identifier != null) {
          identifiers.add(identifier);
        }
      }
      return identifiers;
    }

    if (identifiersNode.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = identifiersNode.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        String value = asText(field.getValue());
        if (value != null) {
          ColDpMetadata.Identifier identifier = new ColDpMetadata.Identifier();
          identifier.setPrefix(field.getKey());
          identifier.setValue(value);
          identifiers.add(identifier);
        }
      }
      return identifiers;
    }

    ColDpMetadata.Identifier identifier = readIdentifier(identifiersNode);
    return identifier == null ? List.of() : List.of(identifier);
  }

  private ColDpMetadata.Identifier readIdentifier(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return parseIdentifierString(asText(node));
    }
    if (!node.isObject()) {
      return null;
    }

    String prefix = asText(firstPresent(node, "prefix", "scheme", "type"));
    String value = asText(firstPresent(node, "id", "identifier", "value"));
    if (value != null) {
      ColDpMetadata.Identifier identifier = new ColDpMetadata.Identifier();
      identifier.setPrefix(prefix);
      identifier.setValue(value);
      return identifier;
    }

    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    if (!fields.hasNext()) {
      return null;
    }
    Map.Entry<String, JsonNode> firstField = fields.next();
    if (fields.hasNext()) {
      return null;
    }
    String singleValue = asText(firstField.getValue());
    if (singleValue == null) {
      return null;
    }
    ColDpMetadata.Identifier identifier = new ColDpMetadata.Identifier();
    identifier.setPrefix(firstField.getKey());
    identifier.setValue(singleValue);
    return identifier;
  }

  private ColDpMetadata.Identifier parseIdentifierString(String value) {
    if (value == null) {
      return null;
    }
    ColDpMetadata.Identifier identifier = new ColDpMetadata.Identifier();
    if (value.contains("://")) {
      identifier.setValue(value);
      return identifier;
    }
    int separator = value.indexOf(':');
    if (separator > 0 && separator < value.length() - 1) {
      identifier.setPrefix(value.substring(0, separator).trim());
      identifier.setValue(value.substring(separator + 1).trim());
    } else {
      identifier.setValue(value);
    }
    return identifier;
  }

  private void addAgents(JsonNode node, Consumer<ColDpMetadata.Agent> sink) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        ColDpMetadata.Agent agent = readAgent(child);
        if (agent != null) {
          sink.accept(agent);
        }
      }
      return;
    }

    ColDpMetadata.Agent agent = readAgent(node);
    if (agent != null) {
      sink.accept(agent);
    }
  }

  private ColDpMetadata.Agent readAgent(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }

    ColDpMetadata.Agent agent = new ColDpMetadata.Agent();
    if (node.isTextual()) {
      agent.setLiteral(asText(node));
      return agent;
    }
    if (!node.isObject()) {
      return null;
    }

    agent.setGiven(asText(node.get("given")));
    agent.setFamily(asText(node.get("family")));
    agent.setOrganization(asText(firstPresent(node, "organisation", "organization")));
    agent.setDepartment(asText(node.get("department")));
    agent.setCity(asText(node.get("city")));
    agent.setState(asText(node.get("state")));
    agent.setCountry(asText(node.get("country")));
    agent.setEmail(asText(node.get("email")));
    agent.setUrl(asText(firstPresent(node, "url", "path")));
    agent.setOrcid(asText(node.get("orcid")));
    agent.setRorid(asText(node.get("rorid")));
    agent.setNote(asText(node.get("note")));

    if (agent.getDisplayName() == null
        && agent.getEmail() == null
        && agent.getPrimaryOrganization() == null
        && agent.getOrcid() == null
        && agent.getRorid() == null) {
      return null;
    }
    return agent;
  }

  private void addSources(JsonNode node, Consumer<ColDpMetadata.Source> sink) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        ColDpMetadata.Source source = readSource(child);
        if (source != null) {
          sink.accept(source);
        }
      }
      return;
    }
    ColDpMetadata.Source source = readSource(node);
    if (source != null) {
      sink.accept(source);
    }
  }

  private ColDpMetadata.Source readSource(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    ColDpMetadata.Source source = new ColDpMetadata.Source();
    if (node.isTextual()) {
      source.setTitle(asText(node));
      return trimToNull(source.getTitle()) == null ? null : source;
    }
    if (!node.isObject()) {
      return null;
    }
    source.setTitle(asText(firstPresent(node, "title", "name")));
    source.setPath(asText(firstPresent(node, "path", "url")));
    source.setEmail(asText(node.get("email")));
    if (source.getTitle() == null) {
      return null;
    }
    return source;
  }

  private JsonNode firstPresent(JsonNode node, String... fieldNames) {
    for (String fieldName : fieldNames) {
      JsonNode child = node.get(fieldName);
      if (child != null && !child.isNull()) {
        return child;
      }
    }
    return null;
  }

  private String asText(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    String value = node.isValueNode() ? node.asText() : node.toString();
    if (value == null) {
      return null;
    }
    value = value.trim();
    return value.isEmpty() ? null : value;
  }

  private ZipEntry findEntry(ZipFile zipFile, List<String> acceptedNames) {
    return zipFile.stream()
        .filter(entry -> !entry.isDirectory())
        .filter(entry -> matchesEntry(entry.getName(), acceptedNames))
        .findFirst()
        .orElse(null);
  }

  private boolean matchesEntry(String entryName, List<String> acceptedNames) {
    String normalized = entryName.toLowerCase(Locale.ROOT);
    for (String acceptedName : acceptedNames) {
      String lowered = acceptedName.toLowerCase(Locale.ROOT);
      if (normalized.equals(lowered) || normalized.endsWith("/" + lowered)) {
        return true;
      }
    }
    return false;
  }

  private static Dataset toDataset(ColDpMetadata metadata) {
    Dataset dataset = new Dataset();
    dataset.setTitle(metadata.getTitle());
    dataset.setDescription(metadata.getDescription());
    dataset.setVersion(metadata.getVersion());

    Date issued = parseDate(metadata.getIssued());
    if (issued != null) {
      dataset.setPubDate(issued);
    }

    String language = trimToNull(metadata.getLanguage());
    if (language != null) {
      dataset.setLanguage(Language.fromIsoCode(language));
    }

    String licenseText = trimToNull(metadata.getLicense());
    if (licenseText != null) {
      LicenseParser licenseParser = LicenseParser.getInstance();
      URI uri = toUri(licenseText);
      License interpreted = licenseParser.parseUriThenTitle(uri, licenseText);
      if (interpreted != License.UNSPECIFIED && interpreted != License.UNSUPPORTED) {
        dataset.setLicense(interpreted);
      }
    }

    dataset.setAlias(metadata.getAlias());

    if (metadata.getGeographicScope() != null) {
      dataset.setGeographicCoverageDescription(metadata.getGeographicScope());
    }

    if (metadata.getTaxonomicScope() != null) {
      TaxonomicCoverages taxonomicCoverages = new TaxonomicCoverages();
      taxonomicCoverages.setDescription(metadata.getTaxonomicScope());
      dataset.getTaxonomicCoverages().add(taxonomicCoverages);
    }

    if (metadata.getTemporalScope() != null) {
      VerbatimTimePeriod verbatimTimePeriod = new VerbatimTimePeriod();
      verbatimTimePeriod.setPeriod(metadata.getTemporalScope());
      dataset.getTemporalCoverages().add(verbatimTimePeriod);
    }

    String notes = trimToNull(metadata.getNotes());
    if (notes != null) {
      dataset.setAdditionalInfo(notes);
    }

    URI logoUrl = toUri(metadata.getLogo());
    if (logoUrl != null) {
      dataset.setLogoUrl(logoUrl);
    }

    URI homepage = toUri(metadata.getUrl());
    if (homepage != null) {
      dataset.setHomepage(homepage);
    }

    if (DOI.isParsable(metadata.getDoi())) {
      dataset.setDoi(new DOI(metadata.getDoi()));
    }

    for (ColDpMetadata.Identifier sourceId : metadata.getIdentifiers()) {
      String identifierText = sourceId.asString();
      if (identifierText == null) {
        continue;
      }
      Identifier identifier = new Identifier();
      identifier.setIdentifier(identifierText);
      if (DOI.isParsable(sourceId.getValue()) || DOI.isParsable(identifierText)) {
        identifier.setType(IdentifierType.DOI);
      } else if (identifierText.startsWith("http://") || identifierText.startsWith("https://")) {
        identifier.setType(IdentifierType.URL);
      } else {
        identifier.setType(IdentifierType.UNKNOWN);
      }
      dataset.getIdentifiers().add(identifier);
    }

    addOrMergeContacts(
        dataset, metadata.getContacts(), ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    addOrMergeContacts(dataset, metadata.getCreators(), ContactType.ORIGINATOR);
    addOrMergeContacts(dataset, metadata.getEditors(), ContactType.METADATA_AUTHOR);
    addOrMergeContacts(dataset, metadata.getContributors(), ContactType.CONTENT_PROVIDER);
    if (metadata.getPublisher() != null) {
      addOrMergeContact(dataset, metadata.getPublisher(), ContactType.PUBLISHER);
    }
    metadata.getSources().stream()
        .map(ColDpMetadataParser::toBibliographicCitation)
        .filter(citation -> citation != null && trimToNull(citation.getText()) != null)
        .forEach(dataset.getBibliographicCitations()::add);

    String citationText = ColDpCitationFormatter.format(metadata);
    if (citationText != null) {
      Citation citation = new Citation();
      citation.setText(citationText);
      dataset.setCitation(citation);
    }

    return dataset;
  }

  private static void addOrMergeContacts(
      Dataset dataset, List<ColDpMetadata.Agent> agents, ContactType type) {
    for (ColDpMetadata.Agent agent : agents) {
      addOrMergeContact(dataset, agent, type);
    }
  }

  private static void addOrMergeContact(
      Dataset dataset, ColDpMetadata.Agent agent, ContactType type) {
    Contact contact = toContact(agent, type);
    if (contact == null) {
      return;
    }
    Contact existingContact = findMatchingContact(dataset.getContacts(), contact);
    if (existingContact == null) {
      dataset.getContacts().add(contact);
    } else {
      mergePositions(existingContact, contact);
    }
  }

  private static Contact toContact(ColDpMetadata.Agent agent, ContactType type) {
    if (agent == null) {
      return null;
    }
    Contact contact = new Contact();
    contact.setFirstName(trimToNull(agent.getGiven()));
    contact.setLastName(trimToNull(agent.getFamily()));
    contact.setOrganization(trimToNull(agent.getPrimaryOrganization()));
    String email = trimToNull(agent.getEmail());
    if (email != null) {
      contact.addEmail(email);
    }
    URI uri = agent.getParsedUrl();
    if (uri != null) {
      contact.addHomepage(uri);
    }
    String orcid = trimToNull(agent.getOrcid());
    if (orcid != null) {
      contact.addUserId(orcid);
    }
    contact.setType(type);
    if (trimToNull(contact.getFirstName()) == null
        && trimToNull(contact.getLastName()) == null
        && trimToNull(contact.getOrganization()) == null
        && contact.getEmail().isEmpty()
        && contact.getHomepage().isEmpty()
        && (contact.getUserId() == null || contact.getUserId().isEmpty())) {
      return null;
    }
    return contact;
  }

  private static Contact findMatchingContact(List<Contact> contacts, Contact candidate) {
    for (Contact existing : contacts) {
      if (isSamePerson(existing, candidate)
          && Objects.equals(existing.getType(), candidate.getType())) {
        return existing;
      }
    }
    return null;
  }

  private static boolean isSamePerson(Contact left, Contact right) {
    return Objects.equals(trimToNull(left.getFirstName()), trimToNull(right.getFirstName()))
        && Objects.equals(trimToNull(left.getLastName()), trimToNull(right.getLastName()))
        && Objects.equals(trimToNull(left.getOrganization()), trimToNull(right.getOrganization()))
        && Objects.equals(left.getEmail(), right.getEmail())
        && Objects.equals(left.getHomepage(), right.getHomepage())
        && Objects.equals(left.getUserId(), right.getUserId());
  }

  private static void mergePositions(Contact existing, Contact incoming) {
    for (String position : incoming.getPosition()) {
      String normalized = trimToNull(position);
      if (normalized != null && !existing.getPosition().contains(normalized)) {
        existing.addPosition(normalized);
      }
    }
  }

  private static Citation toBibliographicCitation(ColDpMetadata.Source source) {
    if (source == null) {
      return null;
    }
    String title = trimToNull(source.getTitle());
    if (title == null) {
      return null;
    }
    String path = trimToNull(source.getPath());
    String email = trimToNull(source.getEmail());
    StringBuilder text = new StringBuilder(title);
    if (path != null) {
      text.append(". ").append(path);
    }
    if (email != null) {
      text.append(". ").append(email);
    }
    Citation citation = new Citation();
    citation.setText(text.toString());
    citation.setIdentifier(path);
    citation.setCitationProvidedBySource(true);
    return citation;
  }

  private static URI toUri(String value) {
    String text = trimToNull(value);
    if (text == null) {
      return null;
    }
    try {
      return URI.create(text);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static Date parseDate(String issued) {
    String value = trimToNull(issued);
    if (value == null) {
      return null;
    }
    try {
      return Date.from(Instant.parse(value));
    } catch (DateTimeParseException e) {
      try {
        return Date.from(OffsetDateTime.parse(value).toInstant());
      } catch (DateTimeParseException e2) {
        try {
          return Date.from(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (DateTimeParseException e3) {
          LOG.warn("Could not parse issued date: {}", value);
          return null;
        }
      }
    }
  }
}
