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

import org.gbif.metadata.eml.ipt.model.Agent;
import org.gbif.metadata.eml.ipt.model.Eml;
import org.gbif.metadata.eml.ipt.model.MaintenanceUpdateFrequency;
import org.gbif.metadata.eml.ipt.model.StudyAreaDescriptor;
import org.gbif.utils.file.FileUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.SimpleTimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EmlFactoryTest {

  @Test
  public void testAlternateJGTIBuild() {
    try {
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample2.xml"));

      assertNotNull(eml);

      // JGTI curatorial unit tests
      // A separate test for the alternate JGTI structure, which includes
      // uncertainty is in sample2.xml
      assertNotNull(eml.getJgtiCuratorialUnits());
      assertEquals("jars", eml.getJgtiCuratorialUnits().get(0).getUnitType());
      assertEquals(new Integer("2000"), eml.getJgtiCuratorialUnits().get(0).getRangeMean());
      assertEquals(new Integer("50"), eml.getJgtiCuratorialUnits().get(0).getUncertaintyMeasure());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testBuild() {
    try {
      Eml eml = EmlFactory.build(FileUtils.classpathStream("eml/ipt/sample.xml"));
      Calendar cal = Calendar.getInstance();
      cal.clear();

      assertNotNull(eml);
      assertEquals("619a4b95-1a82-4006-be6a-7dbe3c9b33c5/v7.41", eml.getPackageId());
      assertEquals("619a4b95-1a82-4006-be6a-7dbe3c9b33c5", eml.getGuid());

      assertTrue(eml.getAlternateIdentifiers().contains("619a4b95-1a82-4006-be6a-7dbe3c9b33c5"));
      assertTrue(eml.getAlternateIdentifiers().contains("doi:10.1093/ageing/29.1.57"));
      assertTrue(
          eml.getAlternateIdentifiers()
              .contains("http://ageing.oxfordjournals.org/content/29/1/57"));

      assertEquals("7.41", eml.getEmlVersion().toPlainString());
      assertEquals("Tanzanian Entomological Collection", eml.getTitle());

      // this is complete test for agents so subsequent agent tests will not be so extensive
      Agent firstCreator = eml.getCreators().get(0);
      assertNotNull(firstCreator);
      assertNotNull(firstCreator.getFirstName());
      assertEquals("David", firstCreator.getFirstName());
      assertNotNull(firstCreator.getLastName());
      assertEquals("Remsen", firstCreator.getLastName());
      assertNull(firstCreator.getRole());
      assertNotNull(firstCreator.getPosition());
      assertEquals(1, firstCreator.getPosition().size());
      assertEquals("ECAT Programme Officer", firstCreator.getPosition().get(0));
      assertNotNull(firstCreator.getOrganisation());
      assertEquals("GBIF", firstCreator.getOrganisation());

      assertNotNull(firstCreator.getAddress());
      assertFalse(firstCreator.getAddress().isEmpty());
      assertNotNull(firstCreator.getAddress().getAddress());
      assertEquals(2, firstCreator.getAddress().getAddress().size());
      assertEquals("Universitestparken 15", firstCreator.getAddress().getAddress().get(0));
      assertEquals("10 Downing Street", firstCreator.getAddress().getAddress().get(1));
      assertNotNull(firstCreator.getAddress().getCity());
      assertEquals("Copenhagen", firstCreator.getAddress().getCity());
      assertNotNull(firstCreator.getAddress().getProvince());
      assertEquals("Sjaelland", firstCreator.getAddress().getProvince());
      assertNotNull(firstCreator.getAddress().getPostalCode());
      assertEquals("2100", firstCreator.getAddress().getPostalCode());
      assertNotNull(firstCreator.getAddress().getCountry());
      assertEquals("DK", firstCreator.getAddress().getCountry());

      assertNotNull(firstCreator.getPhone());
      assertEquals(2, firstCreator.getPhone().size());
      assertEquals(Arrays.asList("+4528261487", "+1234567890"), firstCreator.getPhone());

      assertEquals(2, firstCreator.getEmail().size());
      assertEquals(
          Arrays.asList("dremsen@gbif.org", "trobertson@gbif.org"), firstCreator.getEmail());

      assertNotNull(firstCreator.getHomepage());
      assertEquals(3, firstCreator.getHomepage().size());
      assertEquals(
          Arrays.asList(
              "https://www.gbif.org", "https://regsitry.gbif.org", "https://ipt.gbif.org"),
          firstCreator.getHomepage());

      assertFalse(firstCreator.getUserIds().isEmpty());
      assertEquals(2, firstCreator.getUserIds().size());
      assertEquals("https://orcid.org/", firstCreator.getUserIds().get(0).getDirectory());
      assertEquals("0000-0002-8442-8025", firstCreator.getUserIds().get(0).getIdentifier());
      assertEquals("https://orcid.org/", firstCreator.getUserIds().get(1).getDirectory());
      assertEquals("0001-0002-0003-0004", firstCreator.getUserIds().get(1).getIdentifier());

      // agent test with some null values
      Agent firstMetadataProvider = eml.getMetadataProviders().get(0);
      assertNotNull(firstMetadataProvider);
      assertNotNull(firstMetadataProvider.getFirstName());
      assertEquals("Tim", firstMetadataProvider.getFirstName());
      assertNotNull(firstMetadataProvider.getLastName());
      assertEquals("Robertson", firstMetadataProvider.getLastName());
      assertNotNull(firstMetadataProvider.getAddress());
      assertNotNull(firstMetadataProvider.getAddress().getAddress());
      assertEquals("Universitestparken 15", firstMetadataProvider.getAddress().getAddress().get(0));
      assertEquals("Copenhagen", firstMetadataProvider.getAddress().getCity());
      assertEquals("Copenhagen", firstMetadataProvider.getAddress().getProvince());
      assertEquals("2100", firstMetadataProvider.getAddress().getPostalCode());
      assertEquals("DK", firstMetadataProvider.getAddress().getCountry());
      assertNotNull(firstMetadataProvider.getPhone());
      assertEquals(Collections.singletonList("+4528261487"), firstMetadataProvider.getPhone());
      assertEquals(
          Collections.singletonList("trobertson@gbif.org"), firstMetadataProvider.getEmail());
      assertNotNull(firstMetadataProvider.getHomepage());
      assertEquals(
          Collections.singletonList("http://www.gbif.org"), firstMetadataProvider.getHomepage());
      assertFalse(firstMetadataProvider.getUserIds().isEmpty());
      assertEquals(
          "http://www.researcherid.com/rid/",
          firstMetadataProvider.getUserIds().get(0).getDirectory());
      assertEquals("A-8442-8050", firstMetadataProvider.getUserIds().get(0).getIdentifier());

      // agent test for contact
      Agent firstContact = eml.getContacts().get(0);
      assertNotNull(firstContact);
      assertNotNull(firstContact.getFirstName());
      assertEquals("David", firstContact.getFirstName());
      assertNotNull(firstContact.getLastName());
      assertEquals("Remsen", firstContact.getLastName());
      assertNull(firstContact.getRole());
      assertNotNull(firstContact.getPosition());
      assertEquals(1, firstContact.getPosition().size());
      assertEquals("ECAT Programme Officer", firstContact.getPosition().get(0));
      assertNotNull(firstContact.getOrganisation());
      assertEquals("GBIF", firstContact.getOrganisation());
      assertNotNull(firstContact.getAddress());
      assertNotNull(firstContact.getAddress().getAddress());
      assertEquals("Universitestparken 15", firstContact.getAddress().getAddress().get(0));
      assertNotNull(firstContact.getAddress().getCity());
      assertEquals("Copenhagen", firstContact.getAddress().getCity());
      assertNotNull(firstContact.getAddress().getProvince());
      assertEquals("Sjaelland", firstContact.getAddress().getProvince());
      assertNotNull(firstContact.getAddress().getPostalCode());
      assertEquals("2100", firstContact.getAddress().getPostalCode());
      assertNotNull(firstContact.getAddress().getCountry());
      assertEquals("DK", firstContact.getAddress().getCountry());
      assertNotNull(firstContact.getPhone());
      assertEquals(Collections.singletonList("+4528261487"), firstContact.getPhone());
      assertEquals(Collections.singletonList("dremsen@gbif.org"), firstContact.getEmail());
      assertNotNull(firstContact.getHomepage());
      assertEquals(Collections.singletonList("http://www.gbif.org"), firstContact.getHomepage());
      assertFalse(firstContact.getUserIds().isEmpty());
      assertEquals("http://orcid.org/", firstContact.getUserIds().get(0).getDirectory());
      assertEquals("0000-0002-8442-8025", firstContact.getUserIds().get(0).getIdentifier());

      // limited agent with role tests
      assertNotNull(eml.getAssociatedParties());
      assertEquals(2, eml.getAssociatedParties().size());
      assertEquals("principleInvestigator", eml.getAssociatedParties().get(0).getRole());
      assertFalse(eml.getAssociatedParties().get(0).getUserIds().isEmpty());
      assertEquals(
          "http://ldap.org/", eml.getAssociatedParties().get(0).getUserIds().get(0).getDirectory());
      assertEquals(
          "0000-0002-8442-8075",
          eml.getAssociatedParties().get(0).getUserIds().get(0).getIdentifier());
      assertEquals("pointOfContact", eml.getAssociatedParties().get(1).getRole());

      cal.clear();
      cal.set(2010, Calendar.FEBRUARY, 2);
      assertEquals(cal.getTime(), eml.getPubDate());

      assertEquals("en_US", eml.getLanguage());
      assertEquals(
          "<para>Specimens in jars.</para><para>Collected over years.</para><para>Still being curated.</para>",
          eml.getAbstract());
      assertEquals(
          "<section><title>Introduction</title><para>Actual introduction</para></section>",
          eml.getIntroduction());
      assertEquals("<para>getting started stuff</para>", eml.getGettingStarted());
      assertEquals(
          "<para>Test acknowledgements</para><para><itemizedlist><listitem><para>First item</para></listitem></itemizedlist><orderedlist><listitem><para>First item</para></listitem></orderedlist></para><para><emphasis>Emphasis</emphasis>\n"
              + "                CO<subscript>2</subscript> (or just CO₂)\n"
              + "                m<superscript>3</superscript> (or just m³)\n"
              + "                <literalLayout>\n"
              + "                    x = fn(y, z)\n"
              + "                </literalLayout><ulink url=\"https://example.org\"><citetitle>Example link</citetitle></ulink></para>",
          eml.getAcknowledgements());

      // multiple KeywordSets tests
      assertNotNull(eml.getKeywords());
      assertEquals(2, eml.getKeywords().size());
      assertNotNull(eml.getKeywords().get(0).getKeywords());
      assertEquals(3, eml.getKeywords().get(0).getKeywords().size());
      assertEquals("Insect", eml.getKeywords().get(0).getKeywords().get(0));
      assertEquals("Fly", eml.getKeywords().get(0).getKeywords().get(1));
      assertEquals("Bee", eml.getKeywords().get(0).getKeywords().get(2));
      assertEquals("Zoology Vocabulary Version 1", eml.getKeywords().get(0).getKeywordThesaurus());
      assertEquals(1, eml.getKeywords().get(1).getKeywords().size());
      assertEquals("Spider", eml.getKeywords().get(1).getKeywords().get(0));
      assertEquals("Zoology Vocabulary Version 1", eml.getKeywords().get(1).getKeywordThesaurus());

      assertEquals(
          "Where can the additional information possibly come from?!", eml.getAdditionalInfo());

      // intellectual rights tests
      assertNotNull(eml.getIntellectualRights());
      assertEquals(
          "This work is licensed under <ulink url=\"http://creativecommons.org/publicdomain/zero/1.0/legalcode\"><citetitle>Creative Commons CCZero 1.0 License</citetitle></ulink>.",
          eml.getIntellectualRightsXml());
      assertEquals(
          "This work is licensed under <a href=\"http://creativecommons.org/publicdomain/zero/1.0/legalcode\">Creative Commons CCZero 1.0 License</a>.",
          eml.getIntellectualRights());

      // homepage URL, aka distributionUrl
      assertNotNull(eml.getDistributionUrl());
      assertEquals("http://www.any.org/fauna/coleoptera/beetleList.html", eml.getDistributionUrl());

      // download URL
      assertNotNull(eml.getDistributionDownloadUrl());
      assertEquals("https://ipt.gbif.org/archive.do?r=res", eml.getDistributionDownloadUrl());

      // publisher
      assertNotNull(eml.getPublisherName());
      assertEquals("Publishing Organization 1", eml.getPublisherOrganizationName());

      // geospatial coverages tests
      assertNotNull(eml.getGeospatialCoverages());
      assertEquals(2, eml.getGeospatialCoverages().size());
      assertEquals("Bounding Box 1", eml.getGeospatialCoverages().get(0).getDescription());
      assertEquals(
          new Double("23.975"),
          eml.getGeospatialCoverages().get(0).getBoundingCoordinates().getMax().getLatitude());
      assertEquals(
          new Double("0.703"),
          eml.getGeospatialCoverages().get(0).getBoundingCoordinates().getMax().getLongitude());
      assertEquals(
          new Double("-22.745"),
          eml.getGeospatialCoverages().get(0).getBoundingCoordinates().getMin().getLatitude());
      assertEquals(
          new Double("-1.564"),
          eml.getGeospatialCoverages().get(0).getBoundingCoordinates().getMin().getLongitude());
      assertEquals("Bounding Box 2", eml.getGeospatialCoverages().get(1).getDescription());
      assertEquals(
          new Double("43.975"),
          eml.getGeospatialCoverages().get(1).getBoundingCoordinates().getMax().getLatitude());
      assertEquals(
          new Double("11.564"),
          eml.getGeospatialCoverages().get(1).getBoundingCoordinates().getMax().getLongitude());
      assertEquals(
          new Double("-32.745"),
          eml.getGeospatialCoverages().get(1).getBoundingCoordinates().getMin().getLatitude());
      assertEquals(
          new Double("-10.703"),
          eml.getGeospatialCoverages().get(1).getBoundingCoordinates().getMin().getLongitude());

      // temporal coverages tests
      assertEquals(4, eml.getTemporalCoverages().size());
      cal.clear();
      cal.set(2009, Calendar.DECEMBER, 1);
      assertEquals(cal.getTime(), eml.getTemporalCoverages().get(0).getStartDate());
      cal.set(2009, Calendar.DECEMBER, 30);
      assertEquals(cal.getTime(), eml.getTemporalCoverages().get(0).getEndDate());
      cal.set(2008, Calendar.JUNE, 1);
      assertEquals(cal.getTime(), eml.getTemporalCoverages().get(1).getStartDate());
      assertEquals(cal.getTime(), eml.getTemporalCoverages().get(1).getEndDate());
      assertEquals("During the 70s", eml.getTemporalCoverages().get(2).getFormationPeriod());
      assertEquals("Jurassic", eml.getTemporalCoverages().get(3).getLivingTimePeriod());

      // taxonomic coverages tests
      assertEquals(2, eml.getTaxonomicCoverages().size());
      assertEquals(
          "This is a general taxon coverage with only the scientific name",
          eml.getTaxonomicCoverages().get(0).getDescription());
      assertEquals(
          "Mammalia",
          eml.getTaxonomicCoverages().get(0).getTaxonKeywords().get(0).getScientificName());
      assertEquals(
          "Reptilia",
          eml.getTaxonomicCoverages().get(0).getTaxonKeywords().get(1).getScientificName());
      assertEquals(
          "Coleoptera",
          eml.getTaxonomicCoverages().get(0).getTaxonKeywords().get(2).getScientificName());

      assertEquals(
          "This is a second taxon coverage with all fields",
          eml.getTaxonomicCoverages().get(1).getDescription());
      assertEquals("Class", eml.getTaxonomicCoverages().get(1).getTaxonKeywords().get(0).getRank());
      assertEquals(
          "Aves", eml.getTaxonomicCoverages().get(1).getTaxonKeywords().get(0).getScientificName());
      assertEquals(
          "Birds", eml.getTaxonomicCoverages().get(1).getTaxonKeywords().get(0).getCommonName());

      assertEquals("<para>Provide data to the whole world.</para>", eml.getPurpose());

      assertEquals("Changes done as needed.", eml.getUpdateFrequencyDescription());
      assertEquals(MaintenanceUpdateFrequency.AS_NEEDED, eml.getUpdateFrequency());

      // sampling methods tests
      assertNotNull(eml.getMethodSteps());
      assertEquals(3, eml.getMethodSteps().size());
      assertEquals("Took picture, identified", eml.getMethodSteps().get(0));
      assertEquals("Themometer based test", eml.getMethodSteps().get(1));
      assertEquals("Visual based test", eml.getMethodSteps().get(2));
      assertNotNull(eml.getStudyExtent());
      assertEquals("Daily Obersevation of Pigeons Eating Habits", eml.getStudyExtent());
      assertNotNull(eml.getSampleDescription());
      assertEquals(
          "44KHz is what a CD has... I was more like one a day if I felt like it",
          eml.getSampleDescription());
      assertNotNull(eml.getQualityControl());
      assertEquals("None", eml.getQualityControl());

      // project tests
      assertNotNull(eml.getProject());
      assertEquals("Documenting Some Asian Birds and Insects", eml.getProject().getTitle());
      assertNotNull(eml.getProject().getPersonnel());
      assertEquals("David", eml.getProject().getPersonnel().get(0).getFirstName());
      assertEquals("Green", eml.getProject().getPersonnel().get(0).getLastName());
      assertEquals(
          "http://orcid.org/",
          eml.getProject().getPersonnel().get(0).getUserIds().get(0).getDirectory());
      assertEquals(
          "0000-0002-1234-5678",
          eml.getProject().getPersonnel().get(0).getUserIds().get(0).getIdentifier());
      assertEquals("publisher", eml.getProject().getPersonnel().get(0).getRole());
      assertEquals("My Deep Pockets", eml.getProject().getFunding());
      assertNotNull(eml.getProject().getIdentifier());
      assertEquals("T123", eml.getProject().getIdentifier());
      assertNotNull(eml.getProject().getDescription(), "Part of a series of events.");
      assertEquals("Part of a series of events.", eml.getProject().getDescription());
      assertEquals(
          StudyAreaDescriptor.GENERIC, eml.getProject().getStudyAreaDescription().getName());
      assertEquals(
          "false", eml.getProject().getStudyAreaDescription().getCitableClassificationSystem());
      assertEquals(
          "Turkish Mountains", eml.getProject().getStudyAreaDescription().getDescriptorValue());
      assertEquals(
          "This was done in Avian Migration patterns", eml.getProject().getDesignDescription());
      assertEquals("doi:tims-ident.2135.ex43.33.d", eml.getCitation().getIdentifier());
      assertEquals("Tims assembled checklist", eml.getCitation().getCitation());
      assertEquals("en", eml.getMetadataLanguage());
      cal.clear();
      // 2002-10-23T18:13:51
      SimpleTimeZone tz = new SimpleTimeZone(1000 * 60 * 60, "berlin");
      cal.setTimeZone(tz);
      cal.set(2002, Calendar.OCTOBER, 23, 18, 13, 51);
      cal.set(Calendar.MILLISECOND, 235);
      assertEquals(cal.getTime(), eml.getDateStamp());

      // bibliographic citations tests
      assertNotNull(eml.getBibliographicCitations());
      assertEquals(3, eml.getBibliographicCitations().size());
      // assertNotNull(eml.getBibliographicCitations().get(0));
      assertEquals("title 1", eml.getBibliographicCitations().get(0).getCitation());
      assertEquals("title 2", eml.getBibliographicCitations().get(1).getCitation());
      assertEquals("title 3", eml.getBibliographicCitations().get(2).getCitation());

      assertEquals("dataset", eml.getHierarchyLevel());

      // physical data tests
      assertNotNull(eml.getPhysicalData());
      assertEquals(2, eml.getPhysicalData().size());
      assertEquals("INV-GCEM-0305a1_1_1.shp", eml.getPhysicalData().get(0).getName());
      assertEquals("ASCII", eml.getPhysicalData().get(0).getCharset());
      assertEquals("shapefile", eml.getPhysicalData().get(0).getFormat());
      assertNull(eml.getPhysicalData().get(0).getFormatVersion());
      assertEquals(
          "http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&urlTail=accession=INV-GCEM-0305a1&filename=INV-GCEM-0305a1_1_1.TXT",
          eml.getPhysicalData().get(0).getDistributionUrl());
      assertEquals("INV-GCEM-0305a1_1_2.shp", eml.getPhysicalData().get(1).getName());
      assertEquals("ASCII", eml.getPhysicalData().get(1).getCharset());
      assertEquals("shapefile", eml.getPhysicalData().get(1).getFormat());
      assertEquals("2.0.97", eml.getPhysicalData().get(1).getFormatVersion());
      assertEquals(
          "http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&urlTail=accession=INV-GCEM-0305a1&filename=INV-GCEM-0305a1_1_2.TXT",
          eml.getPhysicalData().get(1).getDistributionUrl());

      // JGTI curatorial unit tests
      // A separate for the alternate JGTI structure that includes uncertainty
      // is in sample2.xml
      assertNotNull(eml.getJgtiCuratorialUnits());
      assertEquals("jars", eml.getJgtiCuratorialUnits().get(0).getUnitType());
      assertEquals(new Integer("500"), eml.getJgtiCuratorialUnits().get(0).getRangeStart());
      assertEquals(new Integer("600"), eml.getJgtiCuratorialUnits().get(0).getRangeEnd());

      assertFalse(eml.getSpecimenPreservationMethods().isEmpty());
      assertEquals("alcohol", eml.getSpecimenPreservationMethods().get(0));
      assertEquals("http://www.tim.org/logo.jpg", eml.getLogoUrl());

      assertEquals("urn:lsid:tim.org:12:1", eml.getCollections().get(0).getParentCollectionId());
      assertEquals("urn:lsid:tim.org:12:2", eml.getCollections().get(0).getCollectionId());
      assertEquals("Mammals", eml.getCollections().get(0).getCollectionName());

    } catch (Exception e) {
      fail();
    }
  }
}
