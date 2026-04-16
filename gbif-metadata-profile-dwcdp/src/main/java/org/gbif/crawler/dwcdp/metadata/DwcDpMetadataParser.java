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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.common.parsers.LanguageParser;
import org.gbif.common.parsers.LicenseParser;

import java.io.IOException;
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
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parser for DwC-DP/Data Package descriptor metadata in {@code datapackage.json}.
 */
public class DwcDpMetadataParser {

  private final ObjectMapper jsonMapper = new ObjectMapper();

  /**
   * Builds a Dataset from a raw {@code datapackage.json} document.
   *
   * @param metadataDocument descriptor bytes
   * @return parsed Dataset
   * @throws IOException if parsing fails
   */
  public static Dataset build(byte[] metadataDocument) throws IOException {
    return toDataset(buildMetadata(metadataDocument));
  }

  /**
   * Builds metadata from a raw {@code datapackage.json} document.
   *
   * @param metadataDocument descriptor bytes
   * @return parsed DwC-DP metadata
   * @throws IOException if parsing fails
   */
  public static DwcDpMetadata buildMetadata(byte[] metadataDocument) throws IOException {
    DwcDpMetadataParser parser = new DwcDpMetadataParser();
    JsonNode root = parser.parseDocument(metadataDocument);
    return parser.parseTree(root);
  }

  private JsonNode parseDocument(byte[] metadataDocument) throws IOException {
    if (metadataDocument == null || metadataDocument.length == 0) {
      throw new IOException("Metadata document is empty");
    }
    JsonNode root = jsonMapper.readTree(metadataDocument);
    if (root == null || !root.isObject()) {
      throw new IOException("Descriptor must be a JSON object");
    }
    JsonNode resources = root.get("resources");
    if (resources == null || !resources.isArray() || resources.isEmpty()) {
      throw new IOException("Descriptor must contain a non-empty resources array");
    }
    return root;
  }

  DwcDpMetadata parseTree(JsonNode root) {
    DwcDpMetadata metadata = new DwcDpMetadata();
    metadata.setName(asText(root.get("name")));
    metadata.setId(asText(root.get("id")));
    metadata.setProfile(asText(root.get("profile")));
    metadata.setTitle(asText(root.get("title")));
    metadata.setDescription(asText(root.get("description")));
    metadata.setHomepage(asText(root.get("homepage")));
    metadata.setVersion(asText(root.get("version")));
    metadata.setCreated(asText(root.get("created")));
    metadata.setLanguage(asText(firstPresent(root, "language", "defaultLanguage")));
    metadata.setImage(asText(firstPresent(root, "image", "logo")));

    metadata.getKeywords().addAll(readStringList(root.get("keywords")));
    metadata.getIdentifiers().addAll(readIdentifiers(root.get("identifier")));
    metadata.getLicenses().addAll(readLicenses(root.get("licenses")));
    addContributors(root.get("contributors"), metadata.getContributors()::add);
    addSources(root.get("sources"), metadata.getSources()::add);
    return metadata;
  }

  private List<String> readStringList(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode child : node) {
        String value = asText(child);
        if (value != null) {
          values.add(value);
        }
      }
    } else {
      String value = asText(node);
      if (value != null) {
        values.add(value);
      }
    }
    return values;
  }

  private List<DwcDpMetadata.Identifier> readIdentifiers(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    List<DwcDpMetadata.Identifier> identifiers = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode child : node) {
        DwcDpMetadata.Identifier id = readIdentifier(child);
        if (id != null) {
          identifiers.add(id);
        }
      }
      return identifiers;
    }
    if (node.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        String value = asText(field.getValue());
        if (value != null) {
          DwcDpMetadata.Identifier id = new DwcDpMetadata.Identifier();
          id.setType(field.getKey());
          id.setValue(value);
          identifiers.add(id);
        }
      }
      return identifiers;
    }
    DwcDpMetadata.Identifier id = readIdentifier(node);
    return id == null ? List.of() : List.of(id);
  }

  private DwcDpMetadata.Identifier readIdentifier(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return parseIdentifierString(asText(node));
    }
    if (!node.isObject()) {
      return null;
    }

    String type = asText(firstPresent(node, "type", "scheme", "prefix"));
    String value = asText(firstPresent(node, "id", "identifier", "value"));
    if (value == null) {
      return null;
    }
    DwcDpMetadata.Identifier id = new DwcDpMetadata.Identifier();
    id.setType(type);
    id.setValue(value);
    return id;
  }

  private DwcDpMetadata.Identifier parseIdentifierString(String value) {
    if (value == null) {
      return null;
    }
    DwcDpMetadata.Identifier id = new DwcDpMetadata.Identifier();
    if (value.contains("://")) {
      id.setValue(value);
      return id;
    }
    int separator = value.indexOf(':');
    if (separator > 0 && separator < value.length() - 1) {
      id.setType(value.substring(0, separator).trim());
      id.setValue(value.substring(separator + 1).trim());
    } else {
      id.setValue(value);
    }
    return id;
  }

  private List<DwcDpMetadata.LicenseInfo> readLicenses(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    List<DwcDpMetadata.LicenseInfo> licenses = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode child : node) {
        DwcDpMetadata.LicenseInfo licenseInfo = readLicense(child);
        if (licenseInfo != null) {
          licenses.add(licenseInfo);
        }
      }
      return licenses;
    }
    DwcDpMetadata.LicenseInfo licenseInfo = readLicense(node);
    return licenseInfo == null ? List.of() : List.of(licenseInfo);
  }

  private DwcDpMetadata.LicenseInfo readLicense(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    DwcDpMetadata.LicenseInfo license = new DwcDpMetadata.LicenseInfo();
    if (node.isTextual()) {
      license.setName(asText(node));
      return license;
    }
    if (!node.isObject()) {
      return null;
    }
    license.setName(asText(node.get("name")));
    license.setPath(asText(node.get("path")));
    license.setTitle(asText(node.get("title")));
    if (license.getName() == null && license.getPath() == null && license.getTitle() == null) {
      return null;
    }
    return license;
  }

  private void addContributors(JsonNode node, Consumer<DwcDpMetadata.Contributor> sink) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        DwcDpMetadata.Contributor contributor = readContributor(child);
        if (contributor != null) {
          sink.accept(contributor);
        }
      }
      return;
    }
    DwcDpMetadata.Contributor contributor = readContributor(node);
    if (contributor != null) {
      sink.accept(contributor);
    }
  }

  private DwcDpMetadata.Contributor readContributor(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    DwcDpMetadata.Contributor contributor = new DwcDpMetadata.Contributor();
    if (node.isTextual()) {
      contributor.setTitle(asText(node));
      return contributor;
    }
    if (!node.isObject()) {
      return null;
    }
    contributor.setTitle(asText(firstPresent(node, "title", "name")));
    contributor.setEmail(asText(node.get("email")));
    contributor.setPath(asText(firstPresent(node, "path", "url")));
    contributor.setRole(asText(node.get("role")));
    contributor.setOrganization(asText(node.get("organization")));
    if (contributor.getTitle() == null
        && contributor.getEmail() == null
        && contributor.getPath() == null
        && contributor.getOrganization() == null) {
      return null;
    }
    return contributor;
  }

  private void addSources(JsonNode node, Consumer<DwcDpMetadata.Source> sink) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        DwcDpMetadata.Source source = readSource(child);
        if (source != null) {
          sink.accept(source);
        }
      }
      return;
    }
    DwcDpMetadata.Source source = readSource(node);
    if (source != null) {
      sink.accept(source);
    }
  }

  private DwcDpMetadata.Source readSource(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    DwcDpMetadata.Source source = new DwcDpMetadata.Source();
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
    // Frictionless Data v1 requires title for source objects.
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

  private static Dataset toDataset(DwcDpMetadata metadata) {
    Dataset dataset = new Dataset();
    dataset.setTitle(firstNotNull(metadata.getTitle(), metadata.getName()));
    dataset.setShortName(trimToNull(metadata.getName()));
    dataset.setDescription(metadata.getDescription());
    dataset.setVersion(metadata.getVersion());

    URI homepage = toUri(metadata.getHomepage());
    if (homepage != null) {
      dataset.setHomepage(homepage);
    }

    Language language = parseLanguage(metadata.getLanguage());
    if (language != null) {
      dataset.setLanguage(language);
      dataset.setDataLanguage(language);
    }

    Date created = parseDate(metadata.getCreated());
    if (created != null) {
      dataset.setPubDate(created);
    }

    if (!metadata.getKeywords().isEmpty()) {
      KeywordCollection keywordCollection = new KeywordCollection();
      metadata.getKeywords().forEach(keywordCollection::addKeyword);
      dataset.getKeywordCollections().add(keywordCollection);
    }

    URI logoUri = toUri(metadata.getImage());
    if (logoUri != null) {
      dataset.setLogoUrl(logoUri);
    }

    addIdentifier(dataset, null, metadata.getId());
    metadata.getIdentifiers().forEach(id -> addIdentifier(dataset, id.getType(), id.getValue()));
    setLicense(dataset, metadata.getLicenses());

    for (DwcDpMetadata.Contributor contributor : metadata.getContributors()) {
      Contact contact = toContact(contributor);
      if (contact != null) {
        dataset.getContacts().add(contact);
      }
    }
    metadata.getSources().stream()
        .map(DwcDpMetadataParser::toBibliographicCitation)
        .filter(citation -> citation != null && trimToNull(citation.getText()) != null)
        .forEach(dataset.getBibliographicCitations()::add);

    String firstDoi = firstDoiValue(dataset.getIdentifiers());
    if (DOI.isParsable(firstDoi)) {
      dataset.setDoi(new DOI(firstDoi));
    }

    String citationText = DwcDpCitationFormatter.format(metadata, dataset);
    if (citationText != null) {
      Citation citation = new Citation();
      citation.setText(citationText);
      dataset.setCitation(citation);
    }
    return dataset;
  }

  private static void addIdentifier(Dataset dataset, String type, String value) {
    String text = trimToNull(value);
    if (text == null) {
      return;
    }
    String normalizedType = trimToNull(type);
    Identifier identifier = new Identifier();
    identifier.setIdentifier(normalizedType == null ? text : (normalizedType + ":" + text));
    identifier.setType(classifyIdentifierType(text, identifier.getIdentifier()));
    dataset.getIdentifiers().add(identifier);
  }

  private static IdentifierType classifyIdentifierType(String rawValue, String renderedIdentifier) {
    if (DOI.isParsable(rawValue) || DOI.isParsable(renderedIdentifier)) {
      return IdentifierType.DOI;
    }
    String tail = identifierTail(renderedIdentifier);
    if (isRecognizedUri(rawValue) || isRecognizedUri(tail) || isRecognizedUri(renderedIdentifier)) {
      return IdentifierType.URI;
    }
    return IdentifierType.UNKNOWN;
  }

  private static String identifierTail(String identifier) {
    if (identifier == null) {
      return null;
    }
    int separator = identifier.indexOf(':');
    if (separator <= 0 || separator >= identifier.length() - 1) {
      return null;
    }
    return trimToNull(identifier.substring(separator + 1));
  }

  private static boolean isRecognizedUri(String value) {
    URI uri = toUri(value);
    return uri != null && trimToNull(uri.getScheme()) != null;
  }

  private static Contact toContact(DwcDpMetadata.Contributor contributor) {
    if (contributor == null) {
      return null;
    }
    Contact contact = new Contact();
    String title = trimToNull(contributor.getTitle());
    String organization = trimToNull(contributor.getOrganization());
    if (organization != null) {
      splitPersonName(contact, title);
      contact.setOrganization(organization);
    } else {
      contact.setOrganization(title);
    }
    String email = trimToNull(contributor.getEmail());
    if (email != null) {
      contact.addEmail(email);
    }
    URI homepage = toUri(contributor.getPath());
    if (homepage != null) {
      contact.addHomepage(homepage);
    }
    contact.setType(parseContactType(contributor.getRole()));
    if (trimToNull(contact.getLastName()) == null
        && trimToNull(contact.getOrganization()) == null
        && trimToNull(contact.getFirstName()) == null
        && contact.getEmail().isEmpty()
        && contact.getHomepage().isEmpty()) {
      return null;
    }
    return contact;
  }

  private static Citation toBibliographicCitation(DwcDpMetadata.Source source) {
    if (source == null) {
      return null;
    }
    String title = trimToNull(source.getTitle());
    String path = trimToNull(source.getPath());
    String email = trimToNull(source.getEmail());
    if (title == null) {
      return null;
    }
    StringBuilder value = new StringBuilder(title);
    if (path != null) {
      value.append(". ").append(path);
    }
    if (email != null) {
      value.append(". ").append(email);
    }
    Citation citation = new Citation();
    citation.setText(value.toString());
    citation.setIdentifier(path);
    citation.setCitationProvidedBySource(true);
    return citation;
  }

  private static void splitPersonName(Contact contact, String fullName) {
    if (fullName == null) {
      return;
    }
    int lastSpace = fullName.lastIndexOf(' ');
    if (lastSpace > 0) {
      contact.setFirstName(fullName.substring(0, lastSpace).trim());
      contact.setLastName(fullName.substring(lastSpace + 1).trim());
    } else {
      contact.setLastName(fullName);
    }
  }

  private static ContactType parseContactType(String role) {
    String normalized = trimToNull(role);
    if (normalized == null) {
      return ContactType.CONTENT_PROVIDER;
    }
    switch (normalized.toLowerCase(Locale.ROOT)) {
      case "author":
        return ContactType.AUTHOR;
      case "publisher":
        return ContactType.PUBLISHER;
      case "wrangler":
        return ContactType.METADATA_AUTHOR;
      case "maintainer":
        return ContactType.POINT_OF_CONTACT;
      default:
        return ContactType.CONTENT_PROVIDER;
    }
  }

  private static void setLicense(Dataset dataset, List<DwcDpMetadata.LicenseInfo> licenses) {
    if (licenses == null || licenses.isEmpty()) {
      return;
    }
    LicenseParser licenseParser = LicenseParser.getInstance();
    License result = License.UNSPECIFIED;
    for (DwcDpMetadata.LicenseInfo licenseInfo : licenses) {
      if (licenseInfo == null) {
        continue;
      }
      URI uri = toUri(licenseInfo.getPath());
      String title = firstNotNull(trimToNull(licenseInfo.getTitle()), trimToNull(licenseInfo.getName()));
      License interpreted = licenseParser.parseUriThenTitle(uri, title);
      result = License.getMostRestrictive(result, interpreted, interpreted);
    }
    if (result != License.UNSPECIFIED) {
      dataset.setLicense(result);
    }
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

  private static Language parseLanguage(String language) {
    String value = trimToNull(language);
    if (value == null) {
      return null;
    }
    return LanguageParser.getInstance().parse(value).getPayload();
  }

  private static Date parseDate(String created) {
    String value = trimToNull(created);
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
          return null;
        }
      }
    }
  }

  private static String firstDoiValue(List<Identifier> identifiers) {
    for (Identifier identifier : identifiers) {
      if (identifier == null) {
        continue;
      }
      if (DOI.isParsable(identifier.getIdentifier())) {
        return identifier.getIdentifier();
      }
      int separator = identifier.getIdentifier().indexOf(':');
      if (separator > 0 && separator < identifier.getIdentifier().length() - 1) {
        String tail = identifier.getIdentifier().substring(separator + 1);
        if (DOI.isParsable(tail)) {
          return tail;
        }
      }
    }
    return null;
  }

  private static String firstNotNull(String first, String second) {
    return first != null ? first : second;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
