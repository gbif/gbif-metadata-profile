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

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal in-memory representation of DwC-DP/Data Package metadata fields consumed by the registry.
 */
public class DwcDpMetadata {

  private String name;
  private String id;
  private String profile;
  private String title;
  private String description;
  private String homepage;
  private String version;
  private String created;
  private String language;
  private String image;
  private final List<String> keywords = new ArrayList<>();
  private final List<Identifier> identifiers = new ArrayList<>();
  private final List<Contributor> contributors = new ArrayList<>();
  private final List<Source> sources = new ArrayList<>();
  private final List<LicenseInfo> licenses = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

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

  public String getHomepage() {
    return homepage;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public List<Identifier> getIdentifiers() {
    return identifiers;
  }

  public List<Contributor> getContributors() {
    return contributors;
  }

  public List<Source> getSources() {
    return sources;
  }

  public List<LicenseInfo> getLicenses() {
    return licenses;
  }

  public static class Identifier {
    private String type;
    private String value;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
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
      if (type == null || type.isBlank()) {
        return value.trim();
      }
      return type.trim() + ":" + value.trim();
    }
  }

  public static class Contributor {
    private String title;
    private String email;
    private String path;
    private String role;
    private String organization;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public String getOrganization() {
      return organization;
    }

    public void setOrganization(String organization) {
      this.organization = organization;
    }
  }

  public static class LicenseInfo {
    private String name;
    private String path;
    private String title;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
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
