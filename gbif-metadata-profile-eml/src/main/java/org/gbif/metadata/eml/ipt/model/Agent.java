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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

  @Serial private static final long serialVersionUID = 7028536657833651816L;

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

  /**
   * Whether this agent describes the same party as another agent: all contact information is equal, the role is
   * ignored. Salutation, names and organisation are compared trimmed, with blank treated as absent, so an empty
   * string matches null; other fields are compared as they are.
   *
   * @param other agent to compare with
   * @return true if both agents carry the same contact information
   */
  public boolean isSameParty(Agent other) {
    if (other == null) {
      return false;
    }
    if (this == other) {
      return true;
    }
    return Objects.equals(
            StringUtils.trimToNull(salutation), StringUtils.trimToNull(other.salutation))
        && Objects.equals(
            StringUtils.trimToNull(firstName), StringUtils.trimToNull(other.firstName))
        && Objects.equals(StringUtils.trimToNull(lastName), StringUtils.trimToNull(other.lastName))
        && Objects.equals(
            StringUtils.trimToNull(organisation), StringUtils.trimToNull(other.organisation))
        && Objects.equals(position, other.position)
        && Objects.equals(address, other.address)
        && Objects.equals(phone, other.phone)
        && Objects.equals(email, other.email)
        && Objects.equals(homepage, other.homepage)
        && Objects.equals(userIds, other.userIds);
  }

  /**
   * @return a deep copy of this agent, sharing no mutable state with it
   */
  public Agent copy() {
    Agent copy = new Agent();
    copy.salutation = salutation;
    copy.firstName = firstName;
    copy.lastName = lastName;
    copy.organisation = organisation;
    copy.role = role;
    copy.position = new ArrayList<>(position);
    copy.phone = new ArrayList<>(phone);
    copy.email = new ArrayList<>(email);
    copy.homepage = new ArrayList<>(homepage);

    if (address != null) {
      Address addressCopy = new Address();
      addressCopy.setAddress(new ArrayList<>(address.getAddress()));
      addressCopy.setCity(address.getCity());
      addressCopy.setProvince(address.getProvince());
      addressCopy.setPostalCode(address.getPostalCode());
      addressCopy.setCountry(address.getCountry());
      copy.address = addressCopy;
    } else {
      copy.address = null;
    }

    List<UserId> userIdsCopy = new ArrayList<>();
    for (UserId userId : userIds) {
      userIdsCopy.add(new UserId(userId.getDirectory(), userId.getIdentifier()));
    }
    copy.userIds = userIdsCopy;
    return copy;
  }
}
