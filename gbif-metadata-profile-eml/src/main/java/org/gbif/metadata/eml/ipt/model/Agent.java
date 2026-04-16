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

import org.apache.commons.lang3.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Encapsulates all the information for an Agent.
 */
@Setter
@EqualsAndHashCode
@ToString
public class Agent implements Serializable {

  private static final long serialVersionUID = 7028536657833651816L;

  @Getter private String salutation;
  private String firstName;
  private String lastName;
  private String organisation;
  @Getter private List<String> position = new ArrayList<>();
  @Getter private Address address = new Address();
  @Getter private List<String> phone = new ArrayList<>();
  @Getter private List<String> email = new ArrayList<>();
  @Getter private String role;
  @Getter private List<String> homepage = new ArrayList<>();
  @Getter private List<UserId> userIds = new ArrayList<>();

  public void addPosition(String position) {
    this.position.add(position);
  }

  public void addEmail(String email) {
    this.email.add(email);
  }

  public String getFirstName() {
    if (firstName == null || firstName.isEmpty()) {
      return null;
    }
    return firstName;
  }

  public void addHomepage(String homepage) {
    this.homepage.add(homepage);
  }

  public String getLastName() {
    if (lastName == null || lastName.isEmpty()) {
      return null;
    }
    return lastName;
  }

  public String getOrganisation() {
    if (organisation == null || organisation.isEmpty()) {
      return null;
    }
    return organisation;
  }

  public void addPhone(String phone) {
    this.phone.add(phone);
  }

  public String getFullName() {
    String name = "";
    if (firstName != null && !firstName.isEmpty()) {
      name += firstName;
    }
    if (lastName != null && !lastName.isEmpty()) {
      name += " " + lastName;
    }
    return StringUtils.trimToNull(name);
  }

  /**
   * Utility to add a userId to the list of userIds. This method was introduced to ease the Digester rules for
   * parsing of EML.
   * @param userId to add
   */
  public void addUserId(UserId userId) {
    userIds.add(userId);
  }

  public boolean isEmpty() {
    return StringUtils.isAllBlank(firstName, lastName, organisation, role)
        && position.isEmpty()
        && phone.isEmpty()
        && email.isEmpty()
        && homepage.isEmpty()
        && address.isEmpty()
        && userIds.isEmpty();
  }
}
