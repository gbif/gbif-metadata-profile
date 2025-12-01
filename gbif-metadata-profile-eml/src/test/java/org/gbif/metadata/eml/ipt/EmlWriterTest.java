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
package org.gbif.metadata.eml.ipt;

import org.gbif.metadata.eml.ipt.model.Address;
import org.gbif.metadata.eml.ipt.model.Agent;
import org.gbif.metadata.eml.ipt.model.Collection;
import org.gbif.metadata.eml.ipt.model.Eml;
import org.gbif.metadata.eml.ipt.model.KeywordSet;
import org.gbif.metadata.eml.ipt.model.MaintenanceUpdateFrequency;
import org.gbif.metadata.eml.ipt.model.PhysicalData;
import org.gbif.metadata.eml.ipt.model.TaxonomicCoverage;
import org.gbif.metadata.eml.ipt.model.UserId;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class EmlWriterTest {

  @Test
  public void testRoundtrip() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      temp.deleteOnExit();
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // read EML
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);

      // compare
      assertEquals("Tanzanian Entomological Collection", eml.getTitle());
      assertEquals(eml2.getTitle(), eml.getTitle());

      assertNotNull(eml.getGuid());
      assertEquals(eml2.getGuid(), eml.getGuid());

      assertEquals("619a4b95-1a82-4006-be6a-7dbe3c9b33c5/v7.41", eml.getPackageId());

      assertNotNull(eml.getMetadataLanguage());
      assertEquals(eml2.getMetadataLanguage(), eml.getMetadataLanguage());

      assertFalse(eml.getAlternateIdentifiers().isEmpty());
      assertFalse(eml2.getAlternateIdentifiers().isEmpty());
      assertEquals(eml2.getAlternateIdentifiers().get(0), eml.getAlternateIdentifiers().get(0));
      assertEquals(eml2.getAlternateIdentifiers().get(1), eml.getAlternateIdentifiers().get(1));
      assertEquals(eml2.getAlternateIdentifiers().get(2), eml.getAlternateIdentifiers().get(2));

      assertFalse(eml.getDescription().isEmpty());
      assertFalse(eml2.getDescription().isEmpty());
      assertEquals(eml2.getDescription(), eml.getDescription());

      assertFalse(eml.getIntroduction().isEmpty());
      assertFalse(eml2.getIntroduction().isEmpty());
      assertEquals(eml2.getIntroduction(), eml.getIntroduction());

      assertFalse(eml.getGettingStarted().isEmpty());
      assertFalse(eml2.getGettingStarted().isEmpty());
      assertEquals(eml2.getGettingStarted(), eml.getGettingStarted());

      assertFalse(eml.getAcknowledgements().isEmpty());
      assertFalse(eml2.getAcknowledgements().isEmpty());
      assertEquals(eml2.getAcknowledgements(), eml.getAcknowledgements());

      assertNotNull(eml.getPubDate());
      assertEquals(eml2.getPubDate(), eml.getPubDate());

      // TODO: fix timezone parsing/writing
      // Sth unknown does go wrong here...
      // assertEquals(eml2.getDateStamp(), eml.getDateStamp());

      assertNotNull(eml.getCitation());
      assertEquals(eml2.getCitation(), eml.getCitation());

      assertNotNull(eml.getLogoUrl());
      assertEquals(eml2.getLogoUrl(), eml.getLogoUrl());

      assertNotNull(eml.getHomepageUrl());
      assertEquals(eml2.getHomepageUrl(), eml.getHomepageUrl());

      assertNotNull(eml.getContacts());
      assertEquals(eml2.getContacts().get(0), eml.getContacts().get(0));

      assertNotNull(eml.getMetadataProviders().get(0));
      assertEquals(eml2.getMetadataProviders().get(0), eml.getMetadataProviders().get(0));

      assertNotNull(eml.getAssociatedParties().get(0));
      assertEquals(eml2.getAssociatedParties().get(0), eml.getAssociatedParties().get(0));

      assertNotNull(eml.getCreators().get(0));
      assertEquals(eml2.getCreators().get(0), eml.getCreators().get(0));

      assertNotNull(eml.getDistributionUrl());
      assertEquals(eml2.getDistributionUrl(), eml.getDistributionUrl());

      assertNotNull(eml.getMetadataLanguage());
      assertEquals(eml2.getMetadataLanguage(), eml.getMetadataLanguage());

      assertNotNull(eml.getProject());
      assertEquals(eml2.getProject(), eml.getProject());

      assertNotNull(eml.getCollections().get(0));
      assertEquals(eml2.getCollections().get(0), eml.getCollections().get(0));

      assertNotNull(eml.getPurpose());
      assertEquals(eml2.getPurpose(), eml.getPurpose());

      assertNotNull(eml.getUpdateFrequencyDescription());
      assertEquals(eml2.getUpdateFrequencyDescription(), eml.getUpdateFrequencyDescription());

      assertNotNull(eml.getUpdateFrequency());
      assertEquals(eml2.getUpdateFrequency(), eml.getUpdateFrequency());

      assertNotNull(eml.getIntellectualRights());
      assertEquals(eml2.getIntellectualRights(), eml.getIntellectualRights());

      assertNotNull(eml.getIntellectualRightsXml());
      assertEquals(eml2.getIntellectualRightsXml(), eml.getIntellectualRightsXml());

      // write EML again with more data
      KeywordSet ks = new KeywordSet();
      ks.add("Carla");
      ks.add("Maria");
      ks.add("Luise");
      eml.addKeywordSet(ks);

      KeywordSet ks2 = new KeywordSet();
      ks2.setKeywordsString(null);
      eml.addKeywordSet(ks2);

      TaxonomicCoverage tc = new TaxonomicCoverage();
      tc.addTaxonKeywords("Abies alba; Puma concolor; Luzula luzuloides var. luzuloides");
      eml.addTaxonomicCoverage(tc);
      IptEmlWriter.writeEmlFile(temp, eml);

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testSetNonNullPubDate() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample3.xml"));
      assertNotNull(eml);
      assertNull(eml.getPubDate());

      String pubDate = "2011-02-07";
      eml.setPubDateAsString(pubDate);

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure pubDate is not null
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);

      System.out.println("New pub date: " + eml2.getPubDate());
      assertNotNull(eml2.getPubDate());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testSetNullPubDate() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);

      Date pubDate = null;
      eml.setPubDate(pubDate);

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testMultipleProjectPersonnel() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);
      assertEquals(1, eml.getProject().getPersonnel().size());
      assertEquals("David", eml.getProject().getPersonnel().get(0).getFirstName());
      assertEquals("Green", eml.getProject().getPersonnel().get(0).getLastName());
      assertEquals(
          "http://orcid.org/",
          eml.getProject().getPersonnel().get(0).getUserIds().get(0).getDirectory());
      assertEquals(
          "0000-0002-1234-5678",
          eml.getProject().getPersonnel().get(0).getUserIds().get(0).getIdentifier());
      assertEquals("publisher", eml.getProject().getPersonnel().get(0).getRole());

      Agent anotherPersonnel = new Agent();
      anotherPersonnel.setFirstName("John");
      anotherPersonnel.setLastName("Stewart");
      anotherPersonnel.setRole("originator");
      UserId userId = new UserId("http://www.researcherid.com/rid/", "A-1234-4321");
      anotherPersonnel.addUserId(userId);
      eml.getProject().getPersonnel().add(anotherPersonnel);

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure personnel has been persisted correctly
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);

      assertEquals(2, eml2.getProject().getPersonnel().size());
      assertEquals("John", eml2.getProject().getPersonnel().get(1).getFirstName());
      assertEquals("Stewart", eml2.getProject().getPersonnel().get(1).getLastName());
      assertEquals("originator", eml2.getProject().getPersonnel().get(1).getRole());
      assertEquals(
          "http://www.researcherid.com/rid/",
          eml.getProject().getPersonnel().get(1).getUserIds().get(0).getDirectory());
      assertEquals(
          "A-1234-4321",
          eml.getProject().getPersonnel().get(1).getUserIds().get(0).getIdentifier());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testMultipleCollection() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);
      assertEquals(1, eml.getCollections().size());
      assertEquals("urn:lsid:tim.org:12:1", eml.getCollections().get(0).getParentCollectionId());
      assertEquals("urn:lsid:tim.org:12:2", eml.getCollections().get(0).getCollectionId());
      assertEquals("Mammals", eml.getCollections().get(0).getCollectionName());

      Collection anotherCollection = new Collection();
      anotherCollection.setParentCollectionId("urn:lsid:jose.org:98:1");
      anotherCollection.setCollectionId("urn:lsid:jose.org:98:2");
      anotherCollection.setCollectionName("Birds");
      eml.addCollection(anotherCollection);

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure collection has been persisted correctly
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);

      assertEquals(2, eml2.getCollections().size());
      assertEquals("urn:lsid:jose.org:98:1", eml2.getCollections().get(1).getParentCollectionId());
      assertEquals("urn:lsid:jose.org:98:2", eml2.getCollections().get(1).getCollectionId());
      assertEquals("Birds", eml2.getCollections().get(1).getCollectionName());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testMultipleCreatorsMetadataProvidersContacts() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);
      assertEquals(1, eml.getCreators().size());
      assertEquals("Remsen", eml.getCreators().get(0).getLastName());

      assertEquals(1, eml.getMetadataProviders().size());
      assertEquals("Robertson", eml.getMetadataProviders().get(0).getLastName());

      assertEquals(1, eml.getContacts().size());
      assertEquals("Remsen", eml.getContacts().get(0).getLastName());

      Agent agent = createMockAgent();
      eml.addCreator(agent);
      eml.addMetadataProvider(agent);
      eml.addContact(agent);

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure another agent has been added to creators,
      // metadataProviders, and
      // contacts lists
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);

      assertEquals(2, eml2.getCreators().size());
      assertEquals(agent, eml2.getCreators().get(1));

      assertEquals(2, eml2.getMetadataProviders().size());
      assertEquals(agent, eml2.getMetadataProviders().get(1));

      assertEquals(2, eml2.getContacts().size());
      assertEquals(agent, eml2.getContacts().get(1));
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testMultipleUserIds() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);
      assertEquals(1, eml.getCreators().size());
      assertEquals(2, eml.getCreators().get(0).getUserIds().size());
      assertEquals(
          "https://orcid.org/", eml.getCreators().get(0).getUserIds().get(0).getDirectory());
      assertEquals(
          "0000-0002-8442-8025", eml.getCreators().get(0).getUserIds().get(0).getIdentifier());

      UserId anotherUserId = new UserId();
      anotherUserId.setDirectory("http://otherorcid.org/");
      anotherUserId.setIdentifier("ABCD-123");

      eml.getCreators().get(0).addUserId(anotherUserId);

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure another agent has been added to creators,
      // metadataProviders, and
      // contacts lists
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);

      assertEquals(3, eml2.getCreators().get(0).getUserIds().size());
      assertEquals(
          "http://otherorcid.org/", eml2.getCreators().get(0).getUserIds().get(2).getDirectory());
      assertEquals("ABCD-123", eml2.getCreators().get(0).getUserIds().get(2).getIdentifier());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testWriteNewProjectIdAndDescription() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);
      assertEquals("T123", eml.getProject().getIdentifier());
      assertEquals("Part of a series of events.", eml.getProject().getDescription());

      eml.getProject().setIdentifier("T123:1");
      eml.getProject().setDescription("Part of a year-long series of events.");

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure project has been persisted correctly
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);
      assertEquals("T123:1", eml.getProject().getIdentifier());
      assertEquals("Part of a year-long series of events.", eml.getProject().getDescription());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testDefaultMaintenanceUpdateFrequency() {
    try {
      // read EML with no update frequency - it should default to UNKOWN
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample2.xml"));
      assertNotNull(eml);
      assertNull(eml.getUpdateFrequencyDescription());
      assertEquals(MaintenanceUpdateFrequency.UNKNOWN, eml.getUpdateFrequency());

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // Verify the update frequency was persisted correctly
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);
      assertNull(eml2.getUpdateFrequencyDescription());
      assertEquals(MaintenanceUpdateFrequency.UNKNOWN, eml2.getUpdateFrequency());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testMultipleSpecimenPreservationMethod() {
    try {
      // read EML
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);
      assertEquals(1, eml.getSpecimenPreservationMethods().size());
      assertEquals("alcohol", eml.getSpecimenPreservationMethods().get(0));

      eml.addSpecimenPreservationMethod("formaldehyde");

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure specimen preservation methods has been persisted
      // correctly
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);

      assertEquals(2, eml2.getSpecimenPreservationMethods().size());
      assertEquals("formaldehyde", eml2.getSpecimenPreservationMethods().get(1));
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testXmlEscaping() {
    try {
      // read EML to have some defaults
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      assertNotNull(eml);

      // use ampersand values
      Agent gbif = new Agent();
      gbif.setOrganisation("GBIF & EOL");
      eml.getContacts().add(gbif);
      eml.setTitle("The <very> important \"resources\" & other things");

      // write EML
      File temp = File.createTempFile("eml", ".xml");
      System.out.println(
          "Writing temporary xml escaping test eml file to " + temp.getAbsolutePath());
      IptEmlWriter.writeEmlFile(temp, eml);

      // now read the EML in again and ensure pubDate is not null
      Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
      assertNotNull(eml2);
      assertEquals(
          "<para>Specimens in jars.</para><para>Collected over years.</para><para>Still being curated.</para>",
          eml2.getAbstract());
      assertEquals("GBIF & EOL", eml2.getContacts().get(1).getOrganisation());
      assertEquals("The <very> important \"resources\" & other things", eml2.getTitle());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testEmptyFormatVersion()
      throws IOException, TemplateException, SAXException, ParserConfigurationException {
    // read EML
    Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
    assertNotNull(eml);
    assertEquals(2, eml.getPhysicalData().size());
    // ensure the format version is optional - non existing = null
    assertNull(eml.getPhysicalData().get(0).getFormatVersion());
    // ensure the format version can be a String! In 1.0.1 it used to be a decimal
    assertEquals("2.0.97", eml.getPhysicalData().get(1).getFormatVersion());

    // create new PhysicalData, that has empty-string format version
    PhysicalData data = new PhysicalData();

    // empty string - should not be written to EML.xml
    data.setFormatVersion("");
    data.setName("Original dataset");
    data.setCharset("UTF-8");
    data.setFormat("MS Excel Spreadsheet");
    // becomes the 2nd Physical data object
    eml.addPhysicalData(data);

    // write EML
    File temp = File.createTempFile("eml", ".xml");
    System.out.println("Writing temporary test eml file to " + temp.getAbsolutePath());
    IptEmlWriter.writeEmlFile(temp, eml);

    // now read the EML in again and ensure the size is still 2
    // Data
    Eml eml2 = EmlFactory.build(Files.newInputStream(temp.toPath()));
    assertNotNull(eml2);
    assertEquals(2, eml2.getPhysicalData().size());
  }

  /**
   * @return mock agent used in testing
   */
  private Agent createMockAgent() {
    Agent a = new Agent();
    a.setFirstName("John");
    a.setLastName("Stewart");

    Address address = new Address();
    address.addAddress("Central Park");
    address.setCity("New York");
    address.setCountry("United States");
    address.setPostalCode("5600");
    address.setProvince("New York");
    a.setAddress(address);

    a.addEmail("jstewart@ny-nhm.org");
    a.addHomepage("http://www.ny-nhm.org");
    a.setOrganisation("Natural History Museum");
    a.addPhone("+19779779797");
    a.addPosition("Head of Entomology");

    UserId userId = new UserId("http://orcid.org/", "0000-0002-8442-9000");
    a.addUserId(userId);

    return a;
  }
}
