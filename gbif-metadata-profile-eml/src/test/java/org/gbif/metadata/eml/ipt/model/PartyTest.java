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

import org.gbif.metadata.eml.ipt.EmlFactory;
import org.gbif.metadata.eml.ipt.IptEmlWriter;
import org.gbif.utils.file.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PartyTest {

  private static Agent agent(String firstName, String lastName, String email) {
    Agent agent = new Agent();
    agent.setFirstName(firstName);
    agent.setLastName(lastName);
    agent.setOrganisation("GBIF");
    agent.addEmail(email);
    agent.getAddress().setCity("Copenhagen");
    agent.addUserId(new UserId("https://orcid.org/", "0000-0000-0000-000" + lastName.length()));
    return agent;
  }

  private static Agent withRole(Agent agent, String role) {
    agent.setRole(role);
    return agent;
  }

  private static Agent alice() {
    return agent("Alice", "Andersen", "alice@example.org");
  }

  private static Agent bob() {
    return agent("Bob", "Berg", "bob@example.org");
  }

  private static Agent carol() {
    return agent("Carol", "Christensen", "carol@example.org");
  }

  private static Agent dan() {
    return agent("Dan", "Dahl", "dan@example.org");
  }

  private static List<String> lastNames(List<Agent> agents) {
    List<String> names = new ArrayList<>();
    for (Agent agent : agents) {
      names.add(agent.getLastName());
    }
    return names;
  }

  private static List<String> partyNames(List<Party> parties) {
    List<String> names = new ArrayList<>();
    for (Party party : parties) {
      names.add(party.getAgent().getLastName());
    }
    return names;
  }

  /** EML agent element, as written by the IPT. */
  private static String xml(String element, String given, String sur, String email, String role) {
    return "<"
        + element
        + "><individualName><givenName>"
        + given
        + "</givenName><surName>"
        + sur
        + "</surName></individualName><electronicMailAddress>"
        + email
        + "</electronicMailAddress>"
        + (role == null ? "" : "<role>" + role + "</role>")
        + "</"
        + element
        + ">";
  }

  private static Eml parse(String... agentElements) throws Exception {
    String doc =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<eml:eml xmlns:eml=\"https://eml.ecoinformatics.org/eml-2.2.0\" packageId=\"test\" system=\"http://gbif.org\">"
            + "<dataset><title>Test</title>"
            + String.join("", agentElements)
            + "</dataset></eml:eml>";
    try (InputStream in = new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8))) {
      return EmlFactory.build(in);
    }
  }

  // --- parsing: one party per person ---

  @Test
  public void testParseSamePersonInEveryRoleIsOneParty() throws Exception {
    Eml eml =
        parse(
            xml("creator", "Alice", "Andersen", "alice@example.org", null),
            xml("metadataProvider", "Alice", "Andersen", "alice@example.org", null),
            xml("contact", "Alice", "Andersen", "alice@example.org", null),
            xml("associatedParty", "Alice", "Andersen", "alice@example.org", "programmer"));

    assertEquals(1, eml.getParties().size());
    Party party = eml.getParties().get(0);
    assertTrue(party.isCreator());
    assertTrue(party.isContact());
    assertTrue(party.isMetadataProvider());
    assertEquals(List.of("programmer"), party.getAssociatedPartyRoles());
    assertNull(party.getAgent().getRole());
  }

  @Test
  public void testParseDifferentContactInformationStaysSeparate() throws Exception {
    Eml eml =
        parse(
            xml("creator", "Alice", "Andersen", "alice@example.org", null),
            xml("contact", "Alice", "Andersen", "alice@old.example.org", null));

    assertEquals(2, eml.getParties().size());
    assertTrue(eml.getParties().get(0).isCreator());
    assertFalse(eml.getParties().get(0).isContact());
    assertTrue(eml.getParties().get(1).isContact());
  }

  @Test
  public void testParseKeepsPrimaryContactWhoIsAlsoCreator() throws Exception {
    Eml eml =
        parse(
            xml("creator", "Alice", "Andersen", "alice@example.org", null),
            xml("contact", "Bob", "Berg", "bob@example.org", null),
            xml("contact", "Alice", "Andersen", "alice@example.org", null));

    assertEquals(List.of("Berg", "Andersen"), partyNames(eml.getParties()));
    assertEquals(List.of("Andersen"), lastNames(eml.getCreators()));
    assertEquals(List.of("Berg", "Andersen"), lastNames(eml.getContacts()));
  }

  @Test
  public void testParseAssociatedPartyWithoutRoleGetsDefaultRole() throws Exception {
    Eml eml = parse(xml("associatedParty", "Alice", "Andersen", "alice@example.org", null));

    assertEquals(List.of("AssociatedParty"), eml.getParties().get(0).getAssociatedPartyRoles());
  }

  @Test
  public void testParseAssociatedPartyRoleNamedLikeDatasetRoleIsNotADatasetRole() throws Exception {
    Eml eml =
        parse(
            xml("associatedParty", "Alice", "Andersen", "alice@example.org", "metadataProvider"),
            xml("associatedParty", "Alice", "Andersen", "alice@example.org", "pointOfContact"));

    Party party = eml.getParties().get(0);
    assertFalse(party.isMetadataProvider());
    assertFalse(party.isContact());
    assertEquals(List.of("metadataProvider", "pointOfContact"), party.getAssociatedPartyRoles());
    assertTrue(eml.getMetadataProviders().isEmpty());
    assertTrue(eml.getContacts().isEmpty());
  }

  @Test
  public void testParsedSamplesWriteStableXmlWithAllAgents() throws Exception {
    Pattern agentElement = Pattern.compile("<(creator|metadataProvider|contact|associatedParty)>");
    for (String sample :
        List.of("eml/ipt/sample.xml", "eml/ipt/sample2.xml", "eml/ipt/sample3.xml")) {
      String source =
          new String(FileUtils.classpathStream(sample).readAllBytes(), StandardCharsets.UTF_8);
      Eml eml = EmlFactory.build(FileUtils.classpathStream(sample));
      String written = IptEmlWriter.writeEmlAsString(eml);

      // every agent element of the source is written, in the same order
      assertEquals(
          agentElements(agentElement, source), agentElements(agentElement, written), sample);

      // reading back what was written gives the same parties and the same XML
      Eml reread =
          EmlFactory.build(new ByteArrayInputStream(written.getBytes(StandardCharsets.UTF_8)));
      assertEquals(eml.getParties(), reread.getParties(), sample);
      assertEquals(written, IptEmlWriter.writeEmlAsString(reread), sample);
    }
  }

  private static List<String> agentElements(Pattern pattern, String xml) {
    // project personnel are not parties, skip the project section
    int project = xml.indexOf("<project");
    Matcher matcher = pattern.matcher(project < 0 ? xml : xml.substring(0, project));
    List<String> elements = new ArrayList<>();
    while (matcher.find()) {
      elements.add(matcher.group(1));
    }
    return elements;
  }

  // --- Party.merge ordering ---

  @Test
  public void testMergeKeepsCreatorOrderAndPrimaryContact() {
    List<Party> parties =
        Party.merge(
            List.of(alice(), bob(), carol()), List.of(dan(), carol()), List.of(bob()), List.of());

    Eml eml = new Eml();
    eml.setParties(parties);
    assertEquals(4, parties.size());
    assertEquals(List.of("Andersen", "Berg", "Christensen"), lastNames(eml.getCreators()));
    assertEquals(List.of("Dahl", "Christensen"), lastNames(eml.getContacts()));
    assertEquals(List.of("Berg"), lastNames(eml.getMetadataProviders()));
  }

  @Test
  public void testMergeConflictingOrdersFirstAppearanceWins() {
    List<Party> parties =
        Party.merge(List.of(alice(), bob()), List.of(bob(), alice()), List.of(), List.of());

    Eml eml = new Eml();
    eml.setParties(parties);
    assertEquals(List.of("Andersen", "Berg"), partyNames(parties));
    // creator (author) order is kept; contacts follow the single party order
    assertEquals(List.of("Andersen", "Berg"), lastNames(eml.getCreators()));
    assertEquals(List.of("Andersen", "Berg"), lastNames(eml.getContacts()));
  }

  @Test
  public void testMergeGroupsAssociatedPartyRoles() {
    List<Party> parties =
        Party.merge(
            List.of(),
            List.of(),
            List.of(),
            List.of(
                withRole(alice(), "author"),
                withRole(bob(), "editor"),
                withRole(alice(), "programmer"),
                withRole(alice(), "author")));

    assertEquals(2, parties.size());
    assertEquals(List.of("author", "programmer"), parties.get(0).getAssociatedPartyRoles());
    assertEquals(List.of("editor"), parties.get(1).getAssociatedPartyRoles());
  }

  // --- role views ---

  @Test
  public void testRoleViewsFollowPartyOrderAndRoles() {
    Party alice = new Party(alice());
    alice.setCreator(true);
    alice.setContact(true);
    alice.addAssociatedPartyRole("author");
    alice.addAssociatedPartyRole("programmer");
    Party bob = new Party(bob());
    bob.setCreator(true);
    bob.setMetadataProvider(true);
    bob.addAssociatedPartyRole("editor");

    Eml eml = new Eml();
    eml.setParties(new ArrayList<>(List.of(bob, alice)));

    assertEquals(List.of("Berg", "Andersen"), lastNames(eml.getCreators()));
    assertEquals(List.of("Andersen"), lastNames(eml.getContacts()));
    assertEquals(List.of("Berg"), lastNames(eml.getMetadataProviders()));
    List<Agent> associatedParties = eml.getAssociatedParties();
    assertEquals(List.of("Berg", "Andersen", "Andersen"), lastNames(associatedParties));
    assertEquals("editor", associatedParties.get(0).getRole());
    assertEquals("author", associatedParties.get(1).getRole());
    assertEquals("programmer", associatedParties.get(2).getRole());
    assertNull(eml.getCreators().get(0).getRole());

    // reordering the parties reorders the creators (authors)
    eml.getParties().remove(alice);
    eml.getParties().add(0, alice);
    assertEquals(List.of("Andersen", "Berg"), lastNames(eml.getCreators()));
  }

  @Test
  public void testRoleViewsAreReadOnly() {
    Eml eml = new Eml();
    eml.addCreator(alice());

    assertThrows(UnsupportedOperationException.class, () -> eml.getCreators().add(bob()));
    assertThrows(UnsupportedOperationException.class, () -> eml.getContacts().clear());
    assertThrows(UnsupportedOperationException.class, () -> eml.getMetadataProviders().clear());
    assertThrows(UnsupportedOperationException.class, () -> eml.getAssociatedParties().clear());
  }

  @Test
  public void testDatasetRoleViewsReturnThePartyAgent() {
    Eml eml = new Eml();
    eml.addCreator(alice());
    eml.addContact(alice());

    Agent creator = eml.getCreators().get(0);
    assertSame(eml.getParties().get(0).getAgent(), creator);
    creator.getAddress().setCity("Aarhus");

    // one person, so the contact changes too
    assertEquals("Aarhus", eml.getContacts().get(0).getAddress().getCity());
  }

  @Test
  public void testAssociatedPartiesViewReturnsCopies() {
    Eml eml = new Eml();
    eml.addAssociatedParty(withRole(alice(), "author"));
    eml.addAssociatedParty(withRole(alice(), "programmer"));

    List<Agent> associatedParties = eml.getAssociatedParties();
    assertNotSame(associatedParties.get(0), associatedParties.get(1));
    assertNotSame(eml.getParties().get(0).getAgent(), associatedParties.get(0));
    assertEquals("author", associatedParties.get(0).getRole());
    assertEquals("programmer", associatedParties.get(1).getRole());
  }

  @Test
  public void testEditingPartyChangesEveryRole() {
    Eml eml = new Eml();
    eml.addCreator(alice());
    eml.addContact(alice());

    eml.getParties().get(0).getAgent().setLastName("Andersen-Holm");

    assertEquals(List.of("Andersen-Holm"), lastNames(eml.getCreators()));
    assertEquals(List.of("Andersen-Holm"), lastNames(eml.getContacts()));
  }

  @Test
  public void testPartyWithoutRoleOrAgentIsNotWritten() {
    Party noRole = new Party(alice());
    Party noAgent = new Party(null);
    noAgent.setCreator(true);
    Party blankRole = new Party(bob());
    blankRole.addAssociatedPartyRole(" ");

    Eml eml = new Eml();
    eml.setParties(new ArrayList<>(List.of(noRole, noAgent, blankRole)));

    assertTrue(noRole.hasNoRole());
    assertTrue(blankRole.hasNoRole());
    assertEquals(3, eml.getParties().size());
    assertTrue(eml.getCreators().isEmpty());
    assertTrue(eml.getContacts().isEmpty());
    assertTrue(eml.getMetadataProviders().isEmpty());
    assertTrue(eml.getAssociatedParties().isEmpty());
  }

  @Test
  public void testSetPartiesNullMeansNone() {
    Eml eml = new Eml();
    eml.addCreator(alice());

    eml.setParties(null);

    assertTrue(eml.getParties().isEmpty());
    assertTrue(eml.getCreators().isEmpty());
  }

  // --- add methods ---

  @Test
  public void testAddFindsSamePartyOrAppends() {
    Eml eml = new Eml();
    eml.addCreator(alice());
    eml.addContact(bob());
    eml.addContact(alice());
    eml.addAssociatedParty(withRole(alice(), "programmer"));
    eml.addAssociatedParty(bob());

    assertEquals(List.of("Andersen", "Berg"), partyNames(eml.getParties()));
    Party alice = eml.getParties().get(0);
    assertTrue(alice.isCreator());
    assertTrue(alice.isContact());
    assertEquals(List.of("programmer"), alice.getAssociatedPartyRoles());
    assertEquals(List.of("AssociatedParty"), eml.getParties().get(1).getAssociatedPartyRoles());
    // add order of the dataset roles follows the party order
    assertEquals(List.of("Andersen", "Berg"), lastNames(eml.getContacts()));
  }

  @Test
  public void testAddClearsTheAgentRole() {
    Eml eml = new Eml();
    eml.addAssociatedParty(withRole(alice(), "author"));
    eml.addCreator(alice());

    Party party = eml.getParties().get(0);
    assertNull(party.getAgent().getRole());
    assertEquals(List.of("author"), party.getAssociatedPartyRoles());
    assertNull(eml.getCreators().get(0).getRole());
    assertEquals("author", eml.getAssociatedParties().get(0).getRole());
  }

  @Test
  public void testBuiltPartiesEqualPartiesReadBack() throws Exception {
    Eml eml = new Eml();
    eml.setTitle("Test");
    eml.addAssociatedParty(withRole(alice(), "author"));
    eml.addCreator(alice());
    eml.addContact(bob());
    eml.addAssociatedParty(bob());

    String written = IptEmlWriter.writeEmlAsString(eml);
    Eml reread =
        EmlFactory.build(new ByteArrayInputStream(written.getBytes(StandardCharsets.UTF_8)));

    assertEquals(eml.getParties(), reread.getParties());
  }

  @Test
  public void testAddStoresTheAgent() {
    Agent agent = alice();
    Eml eml = new Eml();
    eml.addCreator(agent);

    // callers may keep editing the agent they added
    agent.setLastName("Changed");

    assertSame(agent, eml.getParties().get(0).getAgent());
    assertEquals(List.of("Changed"), lastNames(eml.getCreators()));
  }

  // --- creator / publisher fallbacks ---

  @Test
  public void testCreatorAndPublisherFallBackToContact() {
    Eml eml = new Eml();
    eml.addContact(dan());
    assertEquals("Dan Dahl", eml.getCreatorName());
    assertEquals("dan@example.org", eml.getCreatorEmail());

    eml.addCreator(bob());
    eml.addCreator(alice());
    assertEquals("Bob Berg", eml.getCreatorName());
  }

  // --- Agent ---

  @Test
  public void testIsSamePartyIgnoresRoleAndBlankNames() {
    Agent a = withRole(alice(), "author");
    Agent b = alice();
    b.setSalutation(" ");
    assertTrue(a.isSameParty(b));

    Agent padded = alice();
    padded.setFirstName(" Alice ");
    padded.setLastName("Andersen ");
    assertTrue(alice().isSameParty(padded));

    Agent noOrg = alice();
    noOrg.setOrganisation("");
    Agent nullOrg = alice();
    nullOrg.setOrganisation(null);
    assertTrue(noOrg.isSameParty(nullOrg));

    assertFalse(alice().isSameParty(bob()));
    assertFalse(alice().isSameParty(null));
  }

  @Test
  public void testCopyIsDeep() {
    Agent original = withRole(alice(), "author");
    Agent copy = original.copy();

    assertEquals(original, copy);
    assertNotSame(original.getAddress(), copy.getAddress());
    copy.getEmail().add("other@example.org");
    copy.getUserIds().get(0).setIdentifier("changed");
    assertEquals(List.of("alice@example.org"), original.getEmail());
    assertEquals("0000-0000-0000-0008", original.getUserIds().get(0).getIdentifier());
    assertEquals("author", copy.getRole());
  }
}
