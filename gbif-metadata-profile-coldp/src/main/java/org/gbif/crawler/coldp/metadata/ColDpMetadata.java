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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal in-memory representation of the ColDP metadata fields the registry can consume.
 */
public class ColDpMetadata {

  private String title;
  private String description;
  private String doi;
  private String issued;
  private String version;
  private String license;
  private String url;
  private String logo;
  private String language;
  private String notes;
  private final List<Identifier> identifiers = new ArrayList<>();
  private final List<Agent> contacts = new ArrayList<>();
  private final List<Agent> creators = new ArrayList<>();
  private final List<Agent> editors = new ArrayList<>();
  private final List<Agent> contributors = new ArrayList<>();
  private final List<Source> sources = new ArrayList<>();
  private Agent publisher;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getIssued() {
    return issued;
  }

  public void setIssued(String issued) {
    this.issued = issued;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public List<Identifier> getIdentifiers() {
    return identifiers;
  }

  public List<Agent> getContacts() {
    return contacts;
  }

  public List<Agent> getCreators() {
    return creators;
  }

  public List<Agent> getEditors() {
    return editors;
  }

  public List<Agent> getContributors() {
    return contributors;
  }

  public List<Source> getSources() {
    return sources;
  }

  public Agent getPublisher() {
    return publisher;
  }

  public void setPublisher(Agent publisher) {
    this.publisher = publisher;
  }

  public boolean hasAnyMetadata() {
    return title != null
        || description != null
        || doi != null
        || url != null
        || logo != null
        || !identifiers.isEmpty()
        || !contacts.isEmpty()
        || !creators.isEmpty()
        || !editors.isEmpty()
        || !contributors.isEmpty()
        || !sources.isEmpty()
        || publisher != null;
  }

  public static class Identifier {

    private String prefix;
    private String value;

    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String asString() {
      if (value == null || value.isBlank()) {
        return null;
      }
      if (prefix == null || prefix.isBlank()) {
        return value.trim();
      }
      return prefix.trim() + ":" + value.trim();
    }
  }

  public static class Agent {

    private String given;
    private String family;
    private String organization;
    private String department;
    private String city;
    private String state;
    private String country;
    private String email;
    private String url;
    private String orcid;
    private String rorid;
    private String note;
    private String literal;

    public String getGiven() {
      return given;
    }

    public void setGiven(String given) {
      this.given = given;
    }

    public String getFamily() {
      return family;
    }

    public void setFamily(String family) {
      this.family = family;
    }

    public String getOrganization() {
      return organization;
    }

    public void setOrganization(String organization) {
      this.organization = organization;
    }

    public String getDepartment() {
      return department;
    }

    public void setDepartment(String department) {
      this.department = department;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }

    public String getState() {
      return state;
    }

    public void setState(String state) {
      this.state = state;
    }

    public String getCountry() {
      return country;
    }

    public void setCountry(String country) {
      this.country = country;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getOrcid() {
      return orcid;
    }

    public void setOrcid(String orcid) {
      this.orcid = orcid;
    }

    public String getRorid() {
      return rorid;
    }

    public void setRorid(String rorid) {
      this.rorid = rorid;
    }

    public String getNote() {
      return note;
    }

    public void setNote(String note) {
      this.note = note;
    }

    public String getLiteral() {
      return literal;
    }

    public void setLiteral(String literal) {
      this.literal = literal;
    }

    public String getDisplayName() {
      if (literal != null && !literal.isBlank()) {
        return literal;
      }
      StringBuilder name = new StringBuilder();
      if (given != null && !given.isBlank()) {
        name.append(given);
      }
      if (family != null && !family.isBlank()) {
        if (!name.isEmpty()) {
          name.append(' ');
        }
        name.append(family);
      }
      if (name.isEmpty() && organization != null && !organization.isBlank()) {
        name.append(organization);
      }
      return name.isEmpty() ? null : name.toString();
    }

    public String getPrimaryOrganization() {
      if (organization != null && !organization.isBlank()) {
        return organization;
      }
      return null;
    }

    public URI getParsedUrl() {
      try {
        return url == null || url.isBlank() ? null : URI.create(url);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  }

  public static class Source {
    private String title;
    private String path;
    private String email;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
}
