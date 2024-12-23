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
package org.gbif.metadata.eml.parse;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.eml.Collection;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.ProjectAward;
import org.gbif.api.model.registry.eml.RelatedProject;
import org.gbif.api.model.registry.eml.curatorial.CuratorialUnitComposite;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriodType;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.api.vocabulary.PreservationMethodType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.metadata.eml.EMLProfileVersion;
import org.gbif.metadata.eml.EMLWriter;
import org.gbif.metadata.eml.EmlValidator;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DatasetEmlParserTest {

  @Test
  public void testWikipediaContacts() throws Exception {
    Dataset d =
        DatasetEmlParser.build(IOUtils.toByteArray(FileUtils.classpathStream("eml/wikipedia.xml")));
    assertNotNull(d);
    assertEquals(2, d.getContacts().size());
    for (Contact c : d.getContacts()) {
      assertEquals("Markus", c.getFirstName());
      assertEquals("Döring", c.getLastName());
    }
  }

  @Test
  public void testBuildProtocol() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasetEmlParser.build(
                IOUtils.toByteArray(FileUtils.classpathStream("eml/eml-protocol.xml"))));
  }

  private Contact contactByType(Dataset d, ContactType type) {
    for (Contact c : d.getContacts()) {
      if (type == c.getType()) {
        return c;
      }
    }
    return null;
  }

  private void assertIdentifierExists(Dataset d, String id, IdentifierType type) {
    for (Identifier i : d.getIdentifiers()) {
      if (i.getType() == type && id.equals(i.getIdentifier())) {
        return;
      }
    }
    fail("Identifier " + id + " of type " + type + " missing");
  }

  private void assertKeywordExists(Dataset d, String tag) {
    for (KeywordCollection kc : d.getKeywordCollections()) {
      for (String k : kc.getKeywords()) {
        if (k.equals(tag)) {
          return;
        }
      }
    }
    fail("Keyword" + tag + " missing");
  }

  @Test
  public void testEmlParsingBadEnum() {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample3-v1.0.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      // check bad/unrecognized PreservationMethodType enum defaults to PreservationMethodType.OTHER
      assertEquals(
          PreservationMethodType.OTHER,
          dataset.getCollections().get(0).getSpecimenPreservationMethod());
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Roundtripping test for parsing GBIF Metadata Profile version 1.0.1.
   */
  @Disabled("Test fails: probably wrong source data or template")
  @Test
  public void testRoundtrippingV_1_0_1() {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample2-v1.0.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      verifyV_1_0_1(dataset);

      // write again to eml file and read again
      StringWriter writer = new StringWriter();
      EMLWriter emlWriter = EMLWriter.newInstance(true, true);
      emlWriter.writeTo(dataset, writer, EMLProfileVersion.GBIF_1_0_1);

      final String eml = writer.toString();
      // validate new file, written in XML GBIF Metadata Profile v1.1
      EmlValidator.newValidator(EMLProfileVersion.GBIF_1_0_1).validate(eml);

      try (InputStream in =
          new ReaderInputStream(new StringReader(eml), StandardCharsets.UTF_8); ) {
        Dataset dataset2 = DatasetEmlParser.parse(in);
        // ensure new properties in v1.0.1 still properly set
        verifyV_1_0_1(dataset2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Verify properties in GBIF Metadata Profile v1.0.1 are parsed and populated when building a
   * Dataset.
   */
  public void verifyV_1_0_1(Dataset dataset) {
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));

    assertNotNull(dataset);

    assertEquals(new DOI("doi:10.1093/ageing/29.1.57"), dataset.getDoi());

    assertIdentifierExists(dataset, "619a4b95-1a82-4006-be6a-7dbe3c9b33c5", IdentifierType.UUID);
    assertIdentifierExists(
        dataset, "http://ageing.oxfordjournals.org/content/29/1/57", IdentifierType.URL);
    assertEquals(2, dataset.getIdentifiers().size());

    // assertEquals(7, dataset.getEmlVersion());
    assertEquals("Tanzanian Entomological Collection", dataset.getTitle());

    // this is complete test for agents so subsequent agent tests will not be
    // so extensive
    List<Contact> contactList = dataset.getContacts();
    assertEquals(5, contactList.size());

    // test Creator
    Contact contact = contactList.get(0);
    assertEquals("DavidTheCreator", contact.getFirstName());
    assertEquals("Remsen", contact.getLastName());
    assertEquals("ECAT Programme Officer", contact.getPosition().get(0));
    assertEquals("GBIF", contact.getOrganization());
    assertEquals("Universitestparken 15", contact.getAddress().get(0));
    assertEquals("Copenhagen", contact.getCity());
    assertEquals("Sjaelland", contact.getProvince());
    assertEquals("2100", contact.getPostalCode());
    assertEquals(Country.DENMARK, contact.getCountry());
    assertEquals("+4528261487", contact.getPhone().get(0));
    assertEquals("dremsen@gbif.org", contact.getEmail().get(0));
    assertTrue(contact.isPrimary());
    assertEquals(ContactType.ORIGINATOR, contact.getType());

    // test Metadata provider
    contact = contactList.get(1);
    assertEquals("Tim", contact.getFirstName());
    assertEquals("Robertson", contact.getLastName());
    assertEquals("Universitestparken 15", contact.getAddress().get(0));
    assertEquals("Copenhagen", contact.getCity());
    assertEquals("Copenhagen", contact.getProvince());
    assertEquals("2100", contact.getPostalCode());
    assertEquals(Country.DENMARK, contact.getCountry());
    assertEquals("+4528261487", contact.getPhone().get(0));
    assertEquals("trobertson@gbif.org", contact.getEmail().get(0));
    assertTrue(contact.isPrimary());
    assertEquals(ContactType.METADATA_AUTHOR, contact.getType());

    // test Contact
    contact = contactList.get(4);
    assertEquals("David", contact.getFirstName());
    assertEquals("Remsen", contact.getLastName());
    assertEquals("ECAT Programme Officer", contact.getPosition().get(0));
    assertEquals("GBIF", contact.getOrganization());
    assertEquals("Universitestparken 15", contact.getAddress().get(0));
    assertEquals("Copenhagen", contact.getCity());
    assertEquals("Sjaelland", contact.getProvince());
    assertEquals("2100", contact.getPostalCode());
    assertEquals(Country.DENMARK, contact.getCountry());
    assertEquals("+4528261487", contact.getPhone().get(0));
    assertEquals("dremsen@gbif.org", contact.getEmail().get(0));
    assertTrue(contact.isPrimary());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact.getType());

    // test limited agent with role tests
    contact = contactList.get(2);
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, contact.getType());
    assertFalse(contact.isPrimary());
    assertEquals("Markus", contact.getFirstName());
    assertEquals("Döring", contact.getLastName());
    assertEquals("2nd floor", contact.getAddress().get(0));
    assertEquals("Copenhagen", contact.getCity());
    assertEquals("Copenhagen", contact.getProvince());
    assertEquals("2100", contact.getPostalCode());
    assertEquals(Country.DENMARK, contact.getCountry());
    assertEquals("+45 35321 487", contact.getPhone().get(0));
    assertEquals("mdoering@gbif.org", contact.getEmail().get(0));
    assertEquals("http://www.gbif.org", contact.getHomepage().get(0).toString());

    contact = contactList.get(3);
    // assertEquals("pointOfContact", contact.getRole());
    assertEquals(ContactType.POINT_OF_CONTACT, contact.getType());
    assertFalse(contact.isPrimary());

    // Publication Date
    cal.clear();
    cal.set(2010, Calendar.FEBRUARY, 2);
    assertEquals(cal.getTime(), dataset.getPubDate());

    // WritableDataset properties
    // xml:lang="en"
    assertEquals(Language.ENGLISH, dataset.getLanguage());
    // <language>en</language>
    assertEquals(Language.ENGLISH, dataset.getDataLanguage());
    assertEquals("Specimens in jars.", dataset.getDescription());
    assertEquals(
        buildURI("http://www.any.org/fauna/coleoptera/beetleList.html"), dataset.getHomepage());
    assertEquals(buildURI("http://www.tim.org/logo.jpg"), dataset.getLogoUrl());

    // KeywordSets
    assertNotNull(dataset.getKeywordCollections());
    assertEquals(2, dataset.getKeywordCollections().size());
    assertNotNull(dataset.getKeywordCollections().get(0).getKeywords());
    assertEquals(3, dataset.getKeywordCollections().get(0).getKeywords().size());
    assertTrue(dataset.getKeywordCollections().get(0).getKeywords().contains("Insect"));
    assertTrue(dataset.getKeywordCollections().get(0).getKeywords().contains("Insect"));
    assertTrue(dataset.getKeywordCollections().get(0).getKeywords().contains("Insect"));
    assertEquals(
        "Zoology Vocabulary Version 1", dataset.getKeywordCollections().get(0).getThesaurus());
    assertEquals(1, dataset.getKeywordCollections().get(1).getKeywords().size());
    assertTrue(dataset.getKeywordCollections().get(1).getKeywords().contains("Spider"));
    assertEquals(
        "Zoology Vocabulary Version 1", dataset.getKeywordCollections().get(1).getThesaurus());

    // geospatial coverages tests
    assertNotNull(dataset.getGeographicCoverages());
    assertEquals(2, dataset.getGeographicCoverages().size());
    assertEquals("Bounding Box 1", dataset.getGeographicCoverages().get(0).getDescription());
    assertEquals(
        "23.975",
        String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMaxLatitude()));
    assertEquals(
        "0.703",
        String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMaxLongitude()));
    assertEquals(
        "-22.745",
        String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMinLatitude()));
    assertEquals(
        "-1.564",
        String.valueOf(dataset.getGeographicCoverages().get(0).getBoundingBox().getMinLongitude()));
    assertEquals("Bounding Box 2", dataset.getGeographicCoverages().get(1).getDescription());
    assertEquals(
        "43.975",
        String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMaxLatitude()));
    assertEquals(
        "11.564",
        String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMaxLongitude()));
    assertEquals(
        "-32.745",
        String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMinLatitude()));
    assertEquals(
        "-10.703",
        String.valueOf(dataset.getGeographicCoverages().get(1).getBoundingBox().getMinLongitude()));

    // temporal coverages tests
    assertEquals(4, dataset.getTemporalCoverages().size());
    cal.clear();
    cal.set(2009, Calendar.DECEMBER, 1);
    assertEquals(DateRange.class, dataset.getTemporalCoverages().get(0).getClass());
    DateRange d = (DateRange) dataset.getTemporalCoverages().get(0);
    assertEquals(cal.getTime(), d.getStart());
    cal.set(2009, Calendar.DECEMBER, 30);
    assertEquals(cal.getTime(), d.getEnd());

    assertEquals(SingleDate.class, dataset.getTemporalCoverages().get(1).getClass());
    cal.set(2008, Calendar.JUNE, 1);
    SingleDate d2 = (SingleDate) dataset.getTemporalCoverages().get(1);
    assertEquals(cal.getTime(), d2.getDate());

    assertEquals(VerbatimTimePeriod.class, dataset.getTemporalCoverages().get(2).getClass());
    VerbatimTimePeriod d3 = (VerbatimTimePeriod) dataset.getTemporalCoverages().get(2);
    assertEquals("During the 70s", d3.getPeriod());
    assertEquals(VerbatimTimePeriodType.FORMATION_PERIOD, d3.getType());

    assertEquals(VerbatimTimePeriod.class, dataset.getTemporalCoverages().get(3).getClass());
    d3 = (VerbatimTimePeriod) dataset.getTemporalCoverages().get(3);
    assertEquals("Jurassic", d3.getPeriod());
    assertEquals(VerbatimTimePeriodType.LIVING_TIME_PERIOD, d3.getType());

    // taxonomic coverages tests
    assertNotNull(dataset.getTaxonomicCoverages());
    assertEquals(2, dataset.getTaxonomicCoverages().size());
    assertEquals(
        "This is a general taxon coverage with only the scientific name",
        dataset.getTaxonomicCoverages().get(0).getDescription());
    assertEquals(
        "Mammalia",
        dataset.getTaxonomicCoverages().get(0).getCoverages().get(0).getScientificName());
    assertEquals(
        "Reptilia",
        dataset.getTaxonomicCoverages().get(0).getCoverages().get(1).getScientificName());
    assertEquals(
        "Coleoptera",
        dataset.getTaxonomicCoverages().get(0).getCoverages().get(2).getScientificName());

    assertEquals(
        "This is a second taxon coverage with all fields",
        dataset.getTaxonomicCoverages().get(1).getDescription());
    // 1st classification
    assertEquals(
        "Class",
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getRank().getVerbatim());
    assertEquals(
        Rank.CLASS,
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getRank().getInterpreted());
    assertEquals(
        "Aves", dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getScientificName());
    assertEquals(
        "Birds", dataset.getTaxonomicCoverages().get(1).getCoverages().get(0).getCommonName());
    // 2nd classification
    assertEquals(
        "Kingdom",
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getRank().getVerbatim());
    assertEquals(
        Rank.KINGDOM,
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getRank().getInterpreted());
    assertEquals(
        "Plantae",
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getScientificName());
    assertEquals(
        "Plants", dataset.getTaxonomicCoverages().get(1).getCoverages().get(1).getCommonName());
    // 3nd classification with uninterpretable Rank. Only the verbatim value is preserved
    assertEquals(
        "kingggggggggggggdom",
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getRank().getVerbatim());
    assertNull(
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getRank().getInterpreted());
    assertEquals(
        "Animalia",
        dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getScientificName());
    assertEquals(
        "Animals", dataset.getTaxonomicCoverages().get(1).getCoverages().get(2).getCommonName());

    // sampling methods
    assertNotNull(dataset.getSamplingDescription());
    assertEquals(3, dataset.getSamplingDescription().getMethodSteps().size());
    assertEquals(
        "Took picture, identified", dataset.getSamplingDescription().getMethodSteps().get(0));
    assertEquals("Themometer based test", dataset.getSamplingDescription().getMethodSteps().get(1));
    assertEquals(
        "<p>Visual based test</p>\n<p>and one more time</p>",
        dataset.getSamplingDescription().getMethodSteps().get(2));
    assertEquals(
        "Daily Obersevation of Pigeons Eating Habits",
        dataset.getSamplingDescription().getStudyExtent());
    assertEquals(
        "44KHz is what a CD has... I was more like one a day if I felt like it",
        dataset.getSamplingDescription().getSampling());
    assertEquals("None", dataset.getSamplingDescription().getQualityControl());

    // Project (not included is attribute citableClassificationSystem)
    assertNotNull(dataset.getProject());
    assertEquals("Documenting Some Asian Birds and Insects", dataset.getProject().getTitle());
    assertNotNull(dataset.getProject().getContacts());
    assertEquals("Remsen", dataset.getProject().getContacts().get(0).getLastName());
    assertEquals(ContactType.PUBLISHER, dataset.getProject().getContacts().get(0).getType());
    assertEquals("My Deep Pockets", dataset.getProject().getFunding());
    assertEquals("Turkish Mountains", dataset.getProject().getStudyAreaDescription());
    assertEquals(
        "This was done in Avian Migration patterns", dataset.getProject().getDesignDescription());

    assertEquals(Language.ENGLISH, dataset.getLanguage());

    // cal.clear();
    // // 2002-10-23T18:13:51
    // SimpleTimeZone tz = new SimpleTimeZone(1000 * 60 * 60, "berlin");
    // cal.setTimeZone(tz);
    // cal.set(2002, Calendar.OCTOBER, 23, 18, 13, 51);
    // cal.set(Calendar.MILLISECOND, 235);
    // assertEquals(cal.getTime(), dataset.getDateStamp());

    // Single Citation
    assertEquals("doi:tims-ident.2135.ex43.33.d", dataset.getCitation().getIdentifier());
    assertEquals("Tims assembled checklist", dataset.getCitation().getText());

    // Bibliographic citations
    assertNotNull(dataset.getBibliographicCitations());
    assertEquals(3, dataset.getBibliographicCitations().size());
    assertNotNull(dataset.getBibliographicCitations().get(0));
    assertEquals("title 1", dataset.getBibliographicCitations().get(0).getText());
    assertEquals("title 2", dataset.getBibliographicCitations().get(1).getText());
    assertEquals("title 3", dataset.getBibliographicCitations().get(2).getText());
    assertEquals(
        "doi:tims-ident.2136.ex43.33.d",
        dataset.getBibliographicCitations().get(0).getIdentifier());
    assertEquals(
        "doi:tims-ident.2137.ex43.33.d",
        dataset.getBibliographicCitations().get(1).getIdentifier());
    assertEquals(
        "doi:tims-ident.2138.ex43.33.d",
        dataset.getBibliographicCitations().get(2).getIdentifier());

    // assertEquals("dataset", dataset.getHierarchyLevel());

    // Data description (Physical data aka external links)
    assertNotNull(dataset.getDataDescriptions());
    assertEquals(2, dataset.getDataDescriptions().size());
    assertEquals("INV-GCEM-0305a1_1_1.shp", dataset.getDataDescriptions().get(0).getName());
    assertEquals("ASCII", dataset.getDataDescriptions().get(0).getCharset());
    assertEquals("shapefile", dataset.getDataDescriptions().get(0).getFormat());
    assertEquals("2.0", dataset.getDataDescriptions().get(0).getFormatVersion());
    assertEquals(
        buildURI(
            "http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&urlTail=accession=INV-GCEM-0305a1&filename=INV-GCEM-0305a1_1_1.TXT"),
        dataset.getDataDescriptions().get(0).getUrl());
    assertEquals("INV-GCEM-0305a1_1_2.shp", dataset.getDataDescriptions().get(1).getName());
    assertEquals("ASCII", dataset.getDataDescriptions().get(1).getCharset());
    assertEquals("shapefile", dataset.getDataDescriptions().get(1).getFormat());
    assertEquals("2.0", dataset.getDataDescriptions().get(1).getFormatVersion());
    assertEquals(
        buildURI(
            "http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&urlTail=accession=INV-GCEM-0305a1&filename=INV-GCEM-0305a1_1_2.TXT"),
        dataset.getDataDescriptions().get(1).getUrl());

    assertEquals("Provide data to the whole world.", dataset.getPurpose());
    assertEquals(
        "Where can the additional information possibly come from?!", dataset.getAdditionalInfo());

    // Both License and rights are set to null because there was no machine readable license in
    // sample EML
    // intellectualRights and because dataset rights statements are no longer supported
    assertNull(dataset.getLicense());
    assertNull(dataset.getRights());

    // Collection
    Collection collection = dataset.getCollections().get(0);
    assertEquals(PreservationMethodType.ALCOHOL, collection.getSpecimenPreservationMethod());
    assertEquals("urn:lsid:tim.org:12:1", collection.getParentIdentifier());
    assertEquals("urn:lsid:tim.org:12:2", collection.getIdentifier());
    assertEquals("Mammals", collection.getName());
    // JGTI curatorial unit tests inside Collection
    assertNotNull(dataset.getCollections().get(0).getCuratorialUnits());
    assertEquals(5, dataset.getCollections().get(0).getCuratorialUnits().get(0).getCount());
    assertEquals(1, dataset.getCollections().get(0).getCuratorialUnits().get(0).getDeviation());
    assertEquals(
        "SPECIMENS", dataset.getCollections().get(0).getCuratorialUnits().get(0).getTypeVerbatim());
    assertEquals(
        "Drawers", dataset.getCollections().get(0).getCuratorialUnits().get(1).getTypeVerbatim());
    assertEquals(7, dataset.getCollections().get(0).getCuratorialUnits().get(1).getLower());
    assertEquals(2, dataset.getCollections().get(0).getCuratorialUnits().get(1).getUpper());
  }

  @Test
  public void testEmlParsingBreaking() throws IOException {
    // throws a ConversionException/Throwable that is caught - but build still returns the dataset
    // populated partially
    try (InputStream is = FileUtils.classpathStream("eml/sample-breaking.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals(
          "Estimates of walleye abundance for Oneida\n" + "      Lake, NY (1957-2008)",
          dataset.getTitle());
    }
  }

  @Test
  public void testEmlParsingBreakingOnURLConversion() throws IOException {
    // Gracefully handles ConversionException/Throwable during conversion of URLs, and fully
    // populates the dataset
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample1-v1.0.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals("WII Herbarium Dataset", dataset.getTitle());
      assertEquals(buildURI("http://www.wii.gov.in"), dataset.getHomepage());
    }
  }

  private URI buildURI(String uri) {
    try {
      return new URL(uri).toURI();
    } catch (URISyntaxException | MalformedURLException ignored) {
    }
    return null;
  }

  /**
   * Tests parser sets Dataset.rights equal to null when unsupported license is specified in EML.
   */
  @Test
  public void testUnsupportedLicenseSet() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample5-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals(License.UNSUPPORTED, dataset.getLicense());
      assertNull(dataset.getRights());
    }
  }

  /**
   * Tests parser can still set License by lookup by license title, despite malformed license URL in
   * EML.
   */
  @Test
  public void testMalformedLicenseURLSet() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample6-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals(License.CC_BY_4_0, dataset.getLicense());
      assertNull(dataset.getRights());
    }
  }

  /**
   * Simply test we can extract the version of dataset has provided by the IPT
   */
  @Test
  public void testDatasetVersion() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml/ipt_eml.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals("2.1", dataset.getVersion());
    }
  }

  /**
   * Tests parser does NOT set license when no machine readable license specified in EML. This
   * ensures parser does not override dataset license set manually.
   */
  @Test
  public void testUnspecifiedLicenseSet() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample7-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertNull(dataset.getLicense());
      assertNull(dataset.getRights());
    }
  }

  /**
   * Tests parser still sets Dataset.maintenanceUpdateFrequency when accepted alternative name
   * "continous" is specified in EML instead of the correct value "continually" as per the
   * vocabulary/ENUM.
   */
  @Test
  public void testAcceptedAlternateMaintenanceUpdateFrequencySet() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample5-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals(MaintenanceUpdateFrequency.CONTINUALLY, dataset.getMaintenanceUpdateFrequency());
    }
  }

  /**
   * Tests parser does NOT set Dataset.maintenanceUpdateFrequency when no accepted value specified
   * in EML.
   */
  @Test
  public void tesInvalidMaintenanceUpdateFrequencySet() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample6-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertNull(dataset.getMaintenanceUpdateFrequency());
    }
  }

  /**
   * Tests parser does NOT set Dataset.Citation.identifier and Dataset.Citation.text equal to empty
   * strings when invalid citation element supplied in EML. Both fields should be set to NULL.
   */
  @Test
  public void tesInvalidCitationSet() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample6-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertNotNull(dataset.getCitation());
      assertNull(dataset.getCitation().getText());
      assertNull(dataset.getCitation().getIdentifier());
    }
  }

  /**
   * Roundtripping test for GBIF Metadata Profile version 1.1
   */
  @Test
  public void testRoundtrippingV_1_1() {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample4-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      verifyV_1_1(dataset);

      // write again to eml file and read again
      StringWriter writer = new StringWriter();
      EMLWriter emlWriter = EMLWriter.newInstance(true, true);
      emlWriter.writeTo(dataset, writer, EMLProfileVersion.GBIF_1_1);

      final String eml = writer.toString();
      // validate new file
      EmlValidator.newValidator(EMLProfileVersion.GBIF_1_1).validate(eml);

      try (InputStream in = new ReaderInputStream(new StringReader(eml), StandardCharsets.UTF_8)) {
        Dataset dataset2 = DatasetEmlParser.parse(in);
        // ensure new properties in v1.1 still properly set
        verifyV_1_1(dataset2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Verify new properties added in GBIF Metadata Profile v1.1 are parsed and populated when
   * building a Dataset. </br> New properties include: 1. license name and license URL stored in
   * ulink element in intellectualRights (previously intellectualRights was free-text) - POR-2792 2.
   * maintenanceDescription & maintenanceUpdateFrequency 3. multiple contacts, creators, and
   * metadataProvider (previously parser may have expected a single one for each) 4. multiple
   * userIds for any agent (requires aggregating the directory attribute and value together) 5.
   * multiple project personnel (previously may have expected only a single person) 6. project ID
   * attribute 7. multiple collections (previously parser may have expected a single collection)
   * TODO handling multiple specimen preservation methods postponed because it would break API -
   * POR-2460) 8. multiple paragraphs in description
   */
  private void verifyV_1_1(Dataset dataset) {
    // Tests parser can set License by lookup by license URI, when machine readable license EML is
    // well formatted.
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
    assertNull(dataset.getRights());

    assertEquals(MaintenanceUpdateFrequency.NOT_PLANNED, dataset.getMaintenanceUpdateFrequency());
    assertEquals("Data are updated in uneven intervals.", dataset.getMaintenanceDescription());

    List<Contact> contactList = dataset.getContacts();
    assertEquals(8, contactList.size());

    // test Creator #1
    Contact creator1 = contactList.get(0);
    assertTrue(creator1.isPrimary());
    assertEquals(ContactType.ORIGINATOR, creator1.getType());
    assertEquals("Creator 1", creator1.getFirstName());
    assertEquals("Edgar", creator1.getLastName());
    assertNotNull(creator1.getUserId());
    assertEquals("https://orcid.org/0000-0003-0833-9001", creator1.getUserId().get(0));

    // test Creator #2
    Contact creator2 = contactList.get(1);
    assertFalse(creator2.isPrimary());
    assertEquals(ContactType.ORIGINATOR, creator2.getType());
    assertEquals("Creator 2", creator2.getFirstName());
    assertEquals("Stuart-Smith", creator2.getLastName());
    assertNotNull(creator2.getUserId());
    assertEquals("https://orcid.org/0000-0002-8874-0083", creator2.getUserId().get(0));

    // test Metadata Provider #1
    Contact provider1 = contactList.get(2);
    assertTrue(provider1.isPrimary());
    assertEquals(ContactType.METADATA_AUTHOR, provider1.getType());
    assertEquals("Provider 1", provider1.getFirstName());
    assertEquals("Edgar", provider1.getLastName());
    assertNotNull(provider1.getUserId());
    assertEquals(
        "https://scholar.google.com/citations?user=jvW0IrIAAAAY", provider1.getUserId().get(0));

    // test Metadata Provider #2
    Contact provider2 = contactList.get(3);
    assertFalse(provider2.isPrimary());
    assertEquals(ContactType.METADATA_AUTHOR, provider2.getType());
    assertEquals("Provider 2", provider2.getFirstName());
    assertEquals("Stuart-Smith", provider2.getLastName());
    assertNotNull(provider2.getUserId());
    assertEquals(
        "https://scholar.google.com/citations?user=jvW0IrIAAAAZ", provider2.getUserId().get(0));

    // test Associated Party #1
    Contact party1 = contactList.get(4);
    assertFalse(party1.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, party1.getType());
    assertEquals("Party 1", party1.getFirstName());
    assertEquals("Edgar", party1.getLastName());
    assertNotNull(party1.getUserId());
    assertEquals(
        "https://www.linkedin.com/profile/view?id=AAkAAABiOnwBeoX3a3wKqe4IEqDkJ_ifoVj1234",
        party1.getUserId().get(0));

    // test Associated Party #2
    Contact party2 = contactList.get(5);
    assertFalse(party2.isPrimary());
    assertEquals(ContactType.PROCESSOR, party2.getType());
    assertEquals("Party 2", party2.getFirstName());
    assertEquals("Braak", party2.getLastName());
    assertNotNull(party2.getUserId());
    assertEquals(
        "https://www.linkedin.com/profile/view?id=AAkAAABiOnwBeoX3a3wKqe4IEqDkJ_ifoVj4321",
        party2.getUserId().get(0));

    // test Contact #1
    Contact contact1 = contactList.get(6);
    assertTrue(contact1.isPrimary());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact1.getType());
    assertEquals("Contact 1", contact1.getFirstName());
    assertEquals("Edgar", contact1.getLastName());
    assertNotNull(contact1.getUserId());
    assertEquals("http://www.researcherid.com/rid/Z-1234-2013", contact1.getUserId().get(0));

    // test Contact #2
    Contact contact2 = contactList.get(7);
    assertFalse(contact2.isPrimary());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact2.getType());
    assertEquals("Contact 2", contact2.getFirstName());
    assertEquals("Stuart-Smith", contact2.getLastName());
    assertNotNull(contact2.getUserId());
    assertEquals("http://www.researcherid.com/rid/Z-1234-2014", contact2.getUserId().get(0));

    // Project personnel
    List<Contact> personnelList = dataset.getProject().getContacts();
    assertEquals(2, personnelList.size());

    // test project personnel #1
    Contact personnel1 = personnelList.get(0);
    assertFalse(personnel1.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, personnel1.getType());
    assertEquals("Personnel 1", personnel1.getFirstName());
    assertEquals("Edgar", personnel1.getLastName());
    assertNotNull(personnel1.getUserId());
    assertEquals("https://www.linkedin.com/in/john-smith-12345", personnel1.getUserId().get(0));

    // test project personnel #2
    Contact personnel2 = personnelList.get(1);
    assertFalse(personnel2.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, personnel2.getType());
    assertEquals("Personnel 2", personnel2.getFirstName());
    assertEquals("Stuart-Smith", personnel2.getLastName());
    assertNotNull(personnel2.getUserId());
    assertEquals("https://www.linkedin.com/in/john-smith-54321", personnel2.getUserId().get(0));

    // Project ID
    assertEquals("AODN:60978150-1641-11dd-a326-00188b4c0af8", dataset.getProject().getIdentifier());

    // Project abstract and descriptions.
    assertTrue(
        dataset.getProject().getAbstract().startsWith("Reef Life Survey (RLS) aims to improve"));
    assertTrue(
        dataset
            .getProject()
            .getStudyAreaDescription()
            .startsWith("RLS surveys have been undertaken"));
    assertTrue(
        dataset
            .getProject()
            .getDesignDescription()
            .startsWith("As of December 2015, the majority of global data"));

    // Multiple collections
    List<Collection> collections = dataset.getCollections();
    assertEquals(2, collections.size());

    // Collection #1
    Collection collection1 = collections.get(0);
    assertEquals("urn:uuid:rls:fish:0", collection1.getParentIdentifier());
    assertEquals("urn:uuid:rls:fish:1", collection1.getIdentifier());
    assertEquals("Imaginary collection one", collection1.getName());

    // POR-2460 - Handling multiple specimen preservation methods postponed
    // assertEquals(PreservationMethodType.DEEP_FROZEN,
    // collection1.getSpecimenPreservationMethod());

    Collection collection2 = collections.get(1);
    assertEquals("urn:uuid:rls:fish:1", collection2.getParentIdentifier());
    assertEquals("urn:uuid:rls:fish:2", collection2.getIdentifier());
    assertEquals("Imaginary collection two", collection2.getName());

    // JGTI curatorial units (linked to first collection)
    List<CuratorialUnitComposite> units = collection1.getCuratorialUnits();

    // Curatorial unit #1
    CuratorialUnitComposite unit1 = units.get(0);
    assertEquals("Cabinets", unit1.getTypeVerbatim());
    assertEquals(1, unit1.getLower());
    assertEquals(10, unit1.getUpper());

    // Curatorial unit #2
    CuratorialUnitComposite unit2 = units.get(1);
    assertEquals("Drawers", unit2.getTypeVerbatim());
    assertEquals(5, unit2.getDeviation());
    assertEquals(50, unit2.getCount());

    // Multiple paragraphs in description
    assertNotNull(dataset.getDescription());
    String expectedDescription =
        "<p>This dataset contains records of bony fishes and elasmobranchs collected by Reef Life Survey (RLS) divers\n"
            + "  along 50 m transects on shallow rocky and coral reefs, worldwide.\n"
            + "</p>\n"
            + "<p>Abundance information is available for all records found within quantitative survey limits (50 x 5 m swathes\n"
            + "  during a single swim either side of the transect line, each distinguished as a Block), and out-of-survey records\n"
            + "  are identified as presence-only (Method 0).\n"
            + "</p>";
    // remove whitespaces - doesn't matter for HTML
    assertEquals(
        StringUtils.deleteWhitespace(expectedDescription),
        StringUtils.deleteWhitespace(dataset.getDescription()));

    // Citation
    assertNotNull(dataset.getCitation());
    assertEquals("http://doi.org/10.15468/qjgwba", dataset.getCitation().getIdentifier());
    assertEquals(
        "Edgar G J, Stuart-Smith R D (2014): Reef Life Survey: Global reef fish dataset. v2.0. Reef Life Survey. Dataset/Samplingevent. http://doi.org/10.15468/qjgwba",
        dataset.getCitation().getText());

    // Bibliographic citations
    assertEquals(7, dataset.getBibliographicCitations().size());
  }

  /**
   * Roundtripping test for GBIF Metadata Profile version 1.1
   */
  @Test
  public void testRoundtrippingV_1_2() {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample9-v1.2.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      verifyV_1_2(dataset);

      // write again to eml file and read again
      StringWriter writer = new StringWriter();
      EMLWriter emlWriter = EMLWriter.newInstance(true, true);
      emlWriter.writeTo(dataset, writer, EMLProfileVersion.GBIF_1_2);

      final String eml = writer.toString();
      // validate new file
      EmlValidator.newValidator(EMLProfileVersion.GBIF_1_2).validate(eml);

      try (InputStream in = new ReaderInputStream(new StringReader(eml), StandardCharsets.UTF_8)) {
        Dataset dataset2 = DatasetEmlParser.parse(in);
        // ensure new properties in v1.1 still properly set
        verifyV_1_2(dataset2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Verify GBIF Metadata Profile v1.2. Metadata provider - no more required.
   */
  private void verifyV_1_2(Dataset dataset) {
    // Tests parser can set License by lookup by license URI, when machine-readable license EML is
    // well formatted.
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
    assertNull(dataset.getRights());

    assertEquals(MaintenanceUpdateFrequency.NOT_PLANNED, dataset.getMaintenanceUpdateFrequency());
    assertEquals("Data are updated in uneven intervals.", dataset.getMaintenanceDescription());

    List<Contact> contactList = dataset.getContacts();
    assertEquals(6, contactList.size());

    // test Creator #1
    Contact creator1 = contactList.get(0);
    assertTrue(creator1.isPrimary());
    assertEquals(ContactType.ORIGINATOR, creator1.getType());
    assertEquals("Creator 1", creator1.getFirstName());
    assertEquals("Edgar", creator1.getLastName());
    assertNotNull(creator1.getUserId());
    assertEquals("https://orcid.org/0000-0003-0833-9001", creator1.getUserId().get(0));

    // test Creator #2
    Contact creator2 = contactList.get(1);
    assertFalse(creator2.isPrimary());
    assertEquals(ContactType.ORIGINATOR, creator2.getType());
    assertEquals("Creator 2", creator2.getFirstName());
    assertEquals("Stuart-Smith", creator2.getLastName());
    assertNotNull(creator2.getUserId());
    assertEquals("https://orcid.org/0000-0002-8874-0083", creator2.getUserId().get(0));

    // test Associated Party #1
    Contact party1 = contactList.get(2);
    assertFalse(party1.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, party1.getType());
    assertEquals("Party 1", party1.getFirstName());
    assertEquals("Edgar", party1.getLastName());
    assertNotNull(party1.getUserId());
    assertEquals(
        "https://www.linkedin.com/profile/view?id=AAkAAABiOnwBeoX3a3wKqe4IEqDkJ_ifoVj1234",
        party1.getUserId().get(0));

    // test Associated Party #2
    Contact party2 = contactList.get(3);
    assertFalse(party2.isPrimary());
    assertEquals(ContactType.PROCESSOR, party2.getType());
    assertEquals("Party 2", party2.getFirstName());
    assertEquals("Braak", party2.getLastName());
    assertNotNull(party2.getUserId());
    assertEquals(
        "https://www.linkedin.com/profile/view?id=AAkAAABiOnwBeoX3a3wKqe4IEqDkJ_ifoVj4321",
        party2.getUserId().get(0));

    // test Contact #1
    Contact contact1 = contactList.get(4);
    assertTrue(contact1.isPrimary());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact1.getType());
    assertEquals("Contact 1", contact1.getFirstName());
    assertEquals("Edgar", contact1.getLastName());
    assertNotNull(contact1.getUserId());
    assertEquals("http://www.researcherid.com/rid/Z-1234-2013", contact1.getUserId().get(0));

    // test Contact #2
    Contact contact2 = contactList.get(5);
    assertFalse(contact2.isPrimary());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact2.getType());
    assertEquals("Contact 2", contact2.getFirstName());
    assertEquals("Stuart-Smith", contact2.getLastName());
    assertNotNull(contact2.getUserId());
    assertEquals("http://www.researcherid.com/rid/Z-1234-2014", contact2.getUserId().get(0));

    // Project personnel
    List<Contact> personnelList = dataset.getProject().getContacts();
    assertEquals(2, personnelList.size());

    // test project personnel #1
    Contact personnel1 = personnelList.get(0);
    assertFalse(personnel1.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, personnel1.getType());
    assertEquals("Personnel 1", personnel1.getFirstName());
    assertEquals("Edgar", personnel1.getLastName());
    assertNotNull(personnel1.getUserId());
    assertEquals("https://www.linkedin.com/in/john-smith-12345", personnel1.getUserId().get(0));

    // test project personnel #2
    Contact personnel2 = personnelList.get(1);
    assertFalse(personnel2.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, personnel2.getType());
    assertEquals("Personnel 2", personnel2.getFirstName());
    assertEquals("Stuart-Smith", personnel2.getLastName());
    assertNotNull(personnel2.getUserId());
    assertEquals("https://www.linkedin.com/in/john-smith-54321", personnel2.getUserId().get(0));

    // Project ID
    assertEquals("AODN:60978150-1641-11dd-a326-00188b4c0af8", dataset.getProject().getIdentifier());

    // Project abstract and descriptions.
    assertTrue(
        dataset.getProject().getAbstract().startsWith("Reef Life Survey (RLS) aims to improve"));
    assertTrue(
        dataset
            .getProject()
            .getStudyAreaDescription()
            .startsWith("RLS surveys have been undertaken"));
    assertTrue(
        dataset
            .getProject()
            .getDesignDescription()
            .startsWith("As of December 2015, the majority of global data"));

    // Multiple collections
    List<Collection> collections = dataset.getCollections();
    assertEquals(2, collections.size());

    // Collection #1
    Collection collection1 = collections.get(0);
    assertEquals("urn:uuid:rls:fish:0", collection1.getParentIdentifier());
    assertEquals("urn:uuid:rls:fish:1", collection1.getIdentifier());
    assertEquals("Imaginary collection one", collection1.getName());

    // POR-2460 - Handling multiple specimen preservation methods postponed
    // assertEquals(PreservationMethodType.DEEP_FROZEN,
    // collection1.getSpecimenPreservationMethod());

    Collection collection2 = collections.get(1);
    assertEquals("urn:uuid:rls:fish:1", collection2.getParentIdentifier());
    assertEquals("urn:uuid:rls:fish:2", collection2.getIdentifier());
    assertEquals("Imaginary collection two", collection2.getName());

    // JGTI curatorial units (linked to first collection)
    List<CuratorialUnitComposite> units = collection1.getCuratorialUnits();

    // Curatorial unit #1
    CuratorialUnitComposite unit1 = units.get(0);
    assertEquals("Cabinets", unit1.getTypeVerbatim());
    assertEquals(1, unit1.getLower());
    assertEquals(10, unit1.getUpper());

    // Curatorial unit #2
    CuratorialUnitComposite unit2 = units.get(1);
    assertEquals("Drawers", unit2.getTypeVerbatim());
    assertEquals(5, unit2.getDeviation());
    assertEquals(50, unit2.getCount());

    // Multiple paragraphs in description
    assertNotNull(dataset.getDescription());
    String expectedDescription =
        "<p>This dataset contains records of bony fishes and elasmobranchs collected by Reef Life Survey (RLS) divers\n"
            + "  along 50 m transects on shallow rocky and coral reefs, worldwide.\n"
            + "</p>\n"
            + "<p>Abundance information is available for all records found within quantitative survey limits (50 x 5 m swathes\n"
            + "  during a single swim either side of the transect line, each distinguished as a Block), and out-of-survey records\n"
            + "  are identified as presence-only (Method 0).\n"
            + "</p>";
    // remove whitespaces - doesn't matter for HTML
    assertEquals(
        StringUtils.deleteWhitespace(expectedDescription),
        StringUtils.deleteWhitespace(dataset.getDescription()));

    // Citation
    assertNotNull(dataset.getCitation());
    assertEquals("http://doi.org/10.15468/qjgwba", dataset.getCitation().getIdentifier());
    assertEquals(
        "Edgar G J, Stuart-Smith R D (2014): Reef Life Survey: Global reef fish dataset. v2.0. Reef Life Survey. Dataset/Samplingevent. http://doi.org/10.15468/qjgwba",
        dataset.getCitation().getText());

    // Bibliographic citations
    assertEquals(7, dataset.getBibliographicCitations().size());
  }

  /**
   * New element /eml/dataset/distribution/online/url@download
   * Ignored when parsing EML XML, populated from dataset endpoint
   */
  @Test
  public void testDownloadDistribution() {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample10-v1.3.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);

      // add endpoint manually, needed for distribution@download
      Endpoint dwcaEndpoint = new Endpoint();
      dwcaEndpoint.setType(EndpointType.DWC_ARCHIVE);
      dwcaEndpoint.setUrl(URI.create("https://ipt.gbif.org/archive.do?r=res"));
      dataset.addEndpoint(dwcaEndpoint);

      // write again to eml file and read again
      StringWriter writer = new StringWriter();
      EMLWriter emlWriter = EMLWriter.newInstance(true, true);
      emlWriter.writeTo(dataset, writer);

      final String eml = writer.toString();
      String expectedDistribution =
          "<distribution scope=\"document\">\n"
              + "      <online>\n"
              + "        <url function=\"download\">https://ipt.gbif.org/archive.do?r=res</url>\n"
              + "      </online>\n"
              + "    </distribution>";

      assertTrue(
          StringUtils.deleteWhitespace(eml)
              .contains(StringUtils.deleteWhitespace(expectedDistribution)));
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Roundtripping test for GBIF Metadata Profile version 1.3
   */
  @Test
  public void testRoundtrippingV_1_3() {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample10-v1.3.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      verifyV_1_3(dataset);

      // write again to eml file and read again
      StringWriter writer = new StringWriter();
      EMLWriter emlWriter = EMLWriter.newInstance(true, true);
      emlWriter.writeTo(dataset, writer);

      final String eml = writer.toString();
      // validate new file
      EmlValidator.newValidator(EMLProfileVersion.GBIF_1_3).validate(eml);

      try (InputStream in = new ReaderInputStream(new StringReader(eml), StandardCharsets.UTF_8)) {
        Dataset dataset2 = DatasetEmlParser.parse(in);
        // ensure new properties in v1.1 still properly set
        verifyV_1_3(dataset2);
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Verify GBIF Metadata Profile v1.3.
   */
  private void verifyV_1_3(Dataset dataset) {
    // Tests parser can set License by lookup by license URI, when machine-readable license EML is
    // well formatted.
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
    assertNull(dataset.getRights());

    assertEquals("Sample Metadata RLS", dataset.getTitle());
    assertEquals("test-1_3", dataset.getShortName());
    assertEquals("Publishing Organization 1", dataset.getPublishingOrganizationName());
    assertNotNull(dataset.getPublishingOrganizationKey());
    assertEquals(
        "619a4b95-1a82-4006-be6a-7dbe3c9b33c5", dataset.getPublishingOrganizationKey().toString());

    assertNotNull(dataset.getHomepage());
    assertEquals("http://reeflifesurvey.com/", dataset.getHomepage().toString());

    assertEquals(MaintenanceUpdateFrequency.UNKNOWN, dataset.getMaintenanceUpdateFrequency());
    assertEquals("Data are updated in uneven intervals.", dataset.getMaintenanceDescription());

    List<Contact> contactList = dataset.getContacts();
    assertEquals(6, contactList.size());

    // test Creator #1
    Contact creator1 = contactList.get(0);
    assertTrue(creator1.isPrimary());
    assertEquals(ContactType.ORIGINATOR, creator1.getType());
    assertEquals("Mr.", creator1.getSalutation());
    assertEquals("Creator 1", creator1.getFirstName());
    assertEquals("Edgar", creator1.getLastName());
    assertNotNull(creator1.getUserId());
    assertEquals("https://orcid.org/0000-0003-0833-9001", creator1.getUserId().get(0));

    // test Creator #2
    Contact creator2 = contactList.get(1);
    assertFalse(creator2.isPrimary());
    assertEquals(ContactType.ORIGINATOR, creator2.getType());
    assertEquals("Creator 2", creator2.getFirstName());
    assertEquals("Stuart-Smith", creator2.getLastName());
    assertNotNull(creator2.getUserId());
    assertEquals("https://orcid.org/0000-0002-8874-0083", creator2.getUserId().get(0));

    // test Associated Party #1
    Contact party1 = contactList.get(2);
    assertFalse(party1.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, party1.getType());
    assertEquals("Dr.", party1.getSalutation());
    assertEquals("Party 1", party1.getFirstName());
    assertEquals("Edgar", party1.getLastName());
    assertNotNull(party1.getUserId());
    assertEquals(
        "https://www.linkedin.com/profile/view?id=AAkAAABiOnwBeoX3a3wKqe4IEqDkJ_ifoVj1234",
        party1.getUserId().get(0));

    // test Associated Party #2
    Contact party2 = contactList.get(3);
    assertFalse(party2.isPrimary());
    assertEquals(ContactType.PROCESSOR, party2.getType());
    assertEquals("Party 2", party2.getFirstName());
    assertEquals("Braak", party2.getLastName());
    assertNotNull(party2.getUserId());
    assertEquals(
        "https://www.linkedin.com/profile/view?id=AAkAAABiOnwBeoX3a3wKqe4IEqDkJ_ifoVj4321",
        party2.getUserId().get(0));

    // test Contact #1
    Contact contact1 = contactList.get(4);
    assertTrue(contact1.isPrimary());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact1.getType());
    assertEquals("Contact 1", contact1.getFirstName());
    assertEquals("Edgar", contact1.getLastName());
    assertEquals("Reef Life Survey Foundation", contact1.getOrganization());
    assertNotNull(contact1.getPosition());
    assertEquals(2, contact1.getPosition().size());
    assertEquals("President", contact1.getPosition().get(0));
    assertEquals("Director", contact1.getPosition().get(1));
    assertNotNull(contact1.getAddress());
    assertEquals(2, contact1.getAddress().size());
    assertEquals("c/o IMAS, Private Bag 49", contact1.getAddress().get(0));
    assertEquals("Universitetsparken 15", contact1.getAddress().get(1));
    assertEquals("Hobart", contact1.getCity());
    assertEquals("Tasmania", contact1.getProvince());
    assertEquals("7001", contact1.getPostalCode());
    assertEquals(Country.AUSTRALIA, contact1.getCountry());
    assertNotNull(contact1.getPhone());
    assertEquals(2, contact1.getPhone().size());
    assertEquals("+123456789", contact1.getPhone().get(0));
    assertEquals("+987654321", contact1.getPhone().get(1));
    assertNotNull(contact1.getEmail());
    assertEquals(2, contact1.getEmail().size());
    assertEquals("graham@rls.org", contact1.getEmail().get(0));
    assertEquals("gbif@gbif.org", contact1.getEmail().get(1));
    assertNotNull(contact1.getHomepage());
    assertEquals(2, contact1.getHomepage().size());
    assertEquals(URI.create("http://reeflifesurvey.com/"), contact1.getHomepage().get(0));
    assertEquals(URI.create("https://gbif.org/"), contact1.getHomepage().get(1));
    assertNotNull(contact1.getUserId());
    assertEquals(2, contact1.getUserId().size());
    assertEquals("http://www.researcherid.com/rid/Z-1234-2013", contact1.getUserId().get(0));
    assertEquals("https://orcid.org/0000-0000-0000-0000", contact1.getUserId().get(1));

    // test Contact #2
    Contact contact2 = contactList.get(5);
    assertFalse(contact2.isPrimary());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact2.getType());
    assertEquals("Contact 2", contact2.getFirstName());
    assertEquals("Stuart-Smith", contact2.getLastName());
    assertEquals("Reef Life Survey Foundation", contact2.getOrganization());
    assertNotNull(contact2.getPosition());
    assertEquals(1, contact2.getPosition().size());
    assertEquals("Executive Officer", contact2.getPosition().get(0));
    assertNotNull(contact2.getAddress());
    assertEquals(1, contact2.getAddress().size());
    assertEquals("c/o IMAS, Private Bag 49", contact2.getAddress().get(0));
    assertEquals("Hobart", contact2.getCity());
    assertEquals("Tasmania", contact2.getProvince());
    assertEquals("7001", contact2.getPostalCode());
    assertEquals(Country.AUSTRALIA, contact2.getCountry());
    assertNotNull(contact2.getPhone());
    assertEquals(1, contact2.getPhone().size());
    assertEquals("+123456789", contact2.getPhone().get(0));
    assertNotNull(contact2.getEmail());
    assertEquals(1, contact2.getEmail().size());
    assertEquals("rick@rls.org", contact2.getEmail().get(0));
    assertNotNull(contact2.getHomepage());
    assertEquals(1, contact2.getHomepage().size());
    assertEquals(URI.create("http://reeflifesurvey.com/"), contact2.getHomepage().get(0));
    assertNotNull(contact2.getUserId());
    assertEquals(1, contact2.getUserId().size());
    assertEquals("http://www.researcherid.com/rid/Z-1234-2014", contact2.getUserId().get(0));

    // Project personnel
    assertNotNull(dataset.getProject());
    List<Contact> personnelList = dataset.getProject().getContacts();
    assertEquals(2, personnelList.size());

    // test project personnel #1
    Contact personnel1 = personnelList.get(0);
    assertFalse(personnel1.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, personnel1.getType());
    assertEquals("Personnel 1", personnel1.getFirstName());
    assertEquals("Edgar", personnel1.getLastName());
    assertNotNull(personnel1.getUserId());
    assertEquals("https://www.linkedin.com/in/john-smith-12345", personnel1.getUserId().get(0));

    // test project personnel #2
    Contact personnel2 = personnelList.get(1);
    assertFalse(personnel2.isPrimary());
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, personnel2.getType());
    assertEquals("Personnel 2", personnel2.getFirstName());
    assertEquals("Stuart-Smith", personnel2.getLastName());
    assertNotNull(personnel2.getUserId());
    assertEquals("https://www.linkedin.com/in/john-smith-54321", personnel2.getUserId().get(0));

    // Project ID
    assertEquals("AODN:60978150-1641-11dd-a326-00188b4c0af8", dataset.getProject().getIdentifier());

    // Project abstract and descriptions.
    assertTrue(
        dataset.getProject().getAbstract().startsWith("Reef Life Survey (RLS) aims to improve"));
    assertTrue(
        dataset
            .getProject()
            .getStudyAreaDescription()
            .startsWith("RLS surveys have been undertaken"));
    assertTrue(
        dataset
            .getProject()
            .getDesignDescription()
            .startsWith("As of December 2015, the majority of global data"));

    // Project awards
    assertEquals(2, dataset.getProject().getAwards().size());
    ProjectAward award1 = dataset.getProject().getAwards().get(0);
    assertEquals("University 1", award1.getFunderName());
    assertEquals("777", award1.getAwardNumber());
    assertEquals("Funding title 1", award1.getTitle());
    assertEquals("www.example.org", award1.getAwardUrl());
    assertEquals(2, award1.getFunderIdentifiers().size());
    assertEquals("UN1", award1.getFunderIdentifiers().get(0));
    assertEquals("ABC123", award1.getFunderIdentifiers().get(1));
    ProjectAward award2 = dataset.getProject().getAwards().get(1);
    assertEquals("University 2", award2.getFunderName());
    assertEquals("Funding title 2", award2.getTitle());
    assertTrue(award2.getFunderIdentifiers().isEmpty());

    // Related projects
    assertEquals(2, dataset.getProject().getRelatedProjects().size());
    RelatedProject relatedProject1 = dataset.getProject().getRelatedProjects().get(0);
    assertNull(relatedProject1.getIdentifier());
    assertEquals("Related project 1", relatedProject1.getTitle());
    assertEquals("Description of the first related project", relatedProject1.getAbstract());
    assertFalse(relatedProject1.getContacts().isEmpty());
    Contact relatedProjectContact1_1 = relatedProject1.getContacts().get(0);
    assertEquals(ContactType.PRINCIPAL_INVESTIGATOR, relatedProjectContact1_1.getType());
    assertEquals("Org 1", relatedProjectContact1_1.getOrganization());

    RelatedProject relatedProject2 = dataset.getProject().getRelatedProjects().get(1);
    assertEquals("RP-2", relatedProject2.getIdentifier());
    assertEquals("Related project 2", relatedProject2.getTitle());
    assertNull(relatedProject2.getAbstract());
    Contact relatedProjectContact2_1 = relatedProject2.getContacts().get(0);
    assertEquals(ContactType.ORIGINATOR, relatedProjectContact2_1.getType());
    assertEquals("Personnel 1", relatedProjectContact2_1.getFirstName());
    assertEquals("Edgar", relatedProjectContact2_1.getLastName());

    // Multiple collections
    List<Collection> collections = dataset.getCollections();
    assertEquals(2, collections.size());

    // Collection #1
    Collection collection1 = collections.get(0);
    assertEquals("urn:uuid:rls:fish:0", collection1.getParentIdentifier());
    assertEquals("urn:uuid:rls:fish:1", collection1.getIdentifier());
    assertEquals("Imaginary collection one", collection1.getName());

    // POR-2460 - Handling multiple specimen preservation methods postponed
    // assertEquals(PreservationMethodType.DEEP_FROZEN,
    // collection1.getSpecimenPreservationMethod());

    Collection collection2 = collections.get(1);
    assertEquals("urn:uuid:rls:fish:1", collection2.getParentIdentifier());
    assertEquals("urn:uuid:rls:fish:2", collection2.getIdentifier());
    assertEquals("Imaginary collection two", collection2.getName());

    // JGTI curatorial units (linked to first collection)
    List<CuratorialUnitComposite> units = collection1.getCuratorialUnits();

    // Curatorial unit #1
    CuratorialUnitComposite unit1 = units.get(0);
    assertEquals("Cabinets", unit1.getTypeVerbatim());
    assertEquals(1, unit1.getLower());
    assertEquals(10, unit1.getUpper());

    // Curatorial unit #2
    CuratorialUnitComposite unit2 = units.get(1);
    assertEquals("Drawers", unit2.getTypeVerbatim());
    assertEquals(5, unit2.getDeviation());
    assertEquals(50, unit2.getCount());

    // HTML description
    assertNotNull(dataset.getDescription());
    String expectedDescription =
        "<div>\n"
            + "  <h1>A separate section &amp; \"</h1>\n"
            + "  <p>More text  &lt; </p>\n"
            + "  <p>And more text, with  &gt; </p>\n"
            + "    <ul>\n"
            + "      <li>First item</li>\n"
            + "    </ul>\n"
            + "    <ol>\n"
            + "      <li>First item</li>\n"
            + "    </ol>\n"
            + "    <p><a href=\"https://example.org\">Example link</a>\n"
            + "  </p>\n"
            + "  <div>\n"
            + "    <h1>A sub-section</h1>\n"
            + "    <p><b>Emphasis</b>\n"
            + "      CO<sub>2</sub> (or just CO₂)\n"
            + "      m<sup>3</sup> (or just m³)\n"
            + "      <pre>\n"
            + "        x = fn(y, z)\n"
            + "      </pre>\n"
            + "    </p>\n"
            + "  </div>\n"
            + "</div>\n"
            + "<p>some stuff</p>";
    // remove whitespaces - doesn't matter for HTML
    assertEquals(
        StringUtils.deleteWhitespace(expectedDescription),
        StringUtils.deleteWhitespace(dataset.getDescription()));

    // Citation
    assertNotNull(dataset.getCitation());
    assertEquals("http://doi.org/10.15468/qjgwba", dataset.getCitation().getIdentifier());
    assertEquals(
        "Edgar G J, Stuart-Smith R D (2014): Reef Life Survey: Global reef fish dataset. v2.0. Reef Life Survey. Dataset/Samplingevent. http://doi.org/10.15468/qjgwba",
        dataset.getCitation().getText());

    // Bibliographic citations
    assertEquals(7, dataset.getBibliographicCitations().size());

    // Purpose
    assertEquals("<p>The purpose of this dataset.</p>", dataset.getPurpose());

    // introduction, gettingStarted, acknowledgements
    String expectedIntroduction =
        "<p>An introduction goes here.</p>\n"
            + "      <p>It can include multiple paragraphs. And these paragraphs should have enough text to wrap in a wide browser.  So, repeat that last thought. And these paragraphs should have enough text to wrap in a wide browser.  So, repeat that last thought.</p>\n"
            + "      <p>Text can also cite other works, such as [@jones_2001], in which case the associated key must be present\n"
            + "      as either the citation identifier in a `bibtex` element in the EML document, or as the `id` attribute on\n"
            + "      one of the `citation` elements in the EML document.  These identifiers must be unique across the document.  Tools\n"
            + "      such as Pandoc will readily convert these citations and citation entries into various formats, including HTML, PDF,\n"
            + "        and others.</p>\n"
            + "      <p>\n"
            + "        And bulleted lists are also supported:\n"
            + "      </p>\n"
            + "        <ul>\n"
            + "          <li>Science</li>\n"
            + "          <li>Engineering</li>\n"
            + "          <li>Math</li>\n"
            + "        </ul>\n"
            + "      <p>\n"
            + "        It can also include equations:\n"
            + "        <pre>\n"
            + "        $$\\left( x + a \\right)^{n} = \\sum_{k = 0}^{n}{\\left( \\frac{n}{k} \\right)x^{k}a^{n - k}}$$\n"
            + "        </pre>\n"
            + "      </p>\n"
            + "      <p>\n"
            + "        Plus, it can include all of the other features of [Github Flavored Markdown (GFM)](https://github.github.com/gfm/).\n"
            + "      </p>";
    assertEquals(
        StringUtils.deleteWhitespace(expectedIntroduction),
        StringUtils.deleteWhitespace(dataset.getIntroduction()));

    String expectedGettingStarted =
        "<div>\n"
            + "        <p>Some intro text in the getting started, then break into subsections.</p>\n"
            + "        <div>\n"
            + "          <h1>Level 2 heading</h1>\n"
            + "          <p>We use level 2 heading because Level 1 would be at the same level as the main sections of the paper.</p>\n"
            + "        </div>\n"
            + "        <div>\n"
            + "          <h1>Another level 2 heading</h1>\n"
            + "          <p>With some information</p>\n"
            + "        </div>\n"
            + "      </div>";
    assertEquals(
        StringUtils.deleteWhitespace(expectedGettingStarted),
        StringUtils.deleteWhitespace(dataset.getGettingStarted()));

    String expectedAcknowledgements =
        "<p>Many thanks to the field staff and volunteers involved in collecting and "
            + "compiling data on swallow breeding phenology and performance (in alphabetical order): E. Beaton, T. Bryant, "
            + "L. Burke, M, Courtenay, B. Crosby, D. Farrar, S. Lau, A. MacDonald, H. Mann, A. McKeen, D. Nickerson, "
            + "N. Sobhani, and staff from Environment and Climate Change Canada. In addition, we would like to thank all "
            + "the volunteers who contributed to the Maritime Nest Records Scheme. We also thank all the private landowners, "
            + "Acadia University, Ducks Unlimited Canada, Environment and Climate Change Canada, and Parks Canada for "
            + "permitting access to their properties for field research. Special thanks to P. Thomas and B. Whittam at "
            + "Environment and Climate Change Canada for supporting this work, A. Smith at Environment and Climate Change "
            + "Canada for helpful discussions on BBS trends, and to A. Horn, A. Smith, J. Sauer, and two anonymous reviewers "
            + "for providing helpful comments on earlier drafts. Funding was provided by (in alphabetical order) the Canadian "
            + "Wildlife Federation, Environment and Climate Change Canada, the Natural Science and Engineering Research "
            + "Council of Canada, New Brunswick Wildlife Trust Fund, Nova Scotia Habitat Conservation Fund, and Wildlife "
            + "Preservation Canada. This represents Bowdoin Scientific Station contribution no. 2589.</p>";
    assertEquals(
        StringUtils.deleteWhitespace(expectedAcknowledgements),
        StringUtils.deleteWhitespace(dataset.getAcknowledgements()));
  }

  /**
   * Tests parser can handle EML documents exported from BioCASe.
   */
  @Test
  public void testHandlingBiocaseEml() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml-metadata-profile/sample8-v1.1.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);

      // Title
      assertEquals(
          "BoBO - Botanic Garden and Botanical Museum Berlin-Dahlem Observations",
          dataset.getTitle());

      // Description
      assertNotNull(dataset.getDescription());
      assertTrue(
          dataset
              .getDescription()
              .startsWith("<p>BoBO aims at providing biodiversity observation data to GBIF"));

      // License
      assertEquals(License.CC0_1_0, dataset.getLicense());
      assertNull(
          dataset.getRights()); // free-text rights statements are no longer supported by GBIF

      // Creator equal to organisation
      Contact creator = dataset.getContacts().get(0);
      assertEquals(ContactType.ORIGINATOR, creator.getType());
      assertTrue(creator.isPrimary());
      assertEquals("Botanic Garden and Botanical Museum Berlin-Dahlem", creator.getOrganization());

      // Metadata provider equal to organisation
      Contact provider = dataset.getContacts().get(1);
      assertEquals(ContactType.METADATA_AUTHOR, provider.getType());
      assertTrue(provider.isPrimary());
      assertEquals(
          "Gabi Droege, Wolf-Henning Kusber",
          provider.getOrganization()); // TODO fix - not an organisation

      // Contact equal to person
      Contact contact = dataset.getContacts().get(2);
      assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact.getType());
      assertTrue(contact.isPrimary());
      assertEquals(
          "Gabi Droege, Wolf-Henning Kusber",
          contact.getLastName()); // TODO fix - should be split into 2 contacts

      assertNotNull(dataset.getCitation());
      assertNotNull(dataset.getCitation().getText());
      assertTrue(
          dataset
              .getCitation()
              .getText()
              .startsWith("Droege, G., Güntsch, A., Holetschek, J., Kusber"));
    }
  }

  /**
   * Check dates are in UTC, and ranges use the endpoints of the range.
   *
   * <p>An improvement would be to use only the year, not 1st January.
   */
  @Test
  public void testEmlParsingYearCoverage() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml/temporalCoverageRange.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals("Testing date range", dataset.getTitle());
      DateRange dateRange = (DateRange) dataset.getTemporalCoverages().get(0);
      assertEquals(
          Date.from(ZonedDateTime.parse("1986-01-01T00:00:00.001Z").toInstant()),
          dateRange.getStart());
      assertEquals(
          Date.from(ZonedDateTime.parse("2018-12-31T00:00:00.001Z").toInstant()),
          dateRange.getEnd());

      assertEquals(
          Date.from(ZonedDateTime.parse("2018-12-12T00:00:00.000Z").toInstant()),
          dataset.getPubDate());
    }
  }

  /**
   * Plazi provide multiple licenses, we need to use the first (CC0) and not overwrite that with the
   * second (custom).
   */
  @Test
  public void testEmlParsingPlaziLicense() throws IOException {
    try (InputStream is =
        FileUtils.classpathStream("eml/3920856d-4923-4276-ae0b-e8b3478df276.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);
      assertEquals(License.CC0_1_0, dataset.getLicense());
    }
  }

  /**
   * EML descriptions can contain <para> elements, which should be converted to <p> HTML elements.
   */
  @Disabled("Need to decide whether we want to convert escaped HTML and how to do that")
  @Test
  public void testEmlParsingMultipleParagraphs() throws IOException {
    try (InputStream is = FileUtils.classpathStream("eml/multiple-paragraphs-html.xml")) {
      Dataset dataset = DatasetEmlParser.parse(is);

      // Multiple paragraphs in description
      assertNotNull(dataset.getDescription());
      assertEquals(
          "<p>Two CRLFs follow this word:\n"
              + "\n"
              + "One CRLF follows this word:\n"
              + "A new paragraph follows this word:</p>\n"
              + "<p>A list made with CRLFs follows:\n"
              + "- Apple;\n"
              + "- Ball;\n"
              + "New paragraph.</p>\n"
              + "<p>An HTML list follows this line break:\n"
              + "<ul>\n"
              + "<li><a href=\"https://en.wikipedia.org/wiki/Atlantic\">Atlantic</a> (ocean),</li>\n"
              + "<li>Indo-Pacific</li>\n"
              + "</ul>\n"
              + "End paragraph.</p>\n"
              + "<p>More HTML: <i>i</i>, <b>b</b>, <em>em</em>, <strong>strong</strong>.</p>",
          dataset.getDescription());

      // Write the EML out, then read again
      StringWriter writer = new StringWriter();
      EMLWriter emlWriter = EMLWriter.newInstance(true, true);
      emlWriter.writeTo(dataset, writer);
      final String eml = writer.toString();

      try (InputStream in = new ReaderInputStream(new StringReader(eml), StandardCharsets.UTF_8)) {
        Dataset dataset2 = DatasetEmlParser.parse(in);

        // Check description is unchanged.
        assertEquals(dataset.getDescription(), dataset2.getDescription());
      }
    }
  }
}
