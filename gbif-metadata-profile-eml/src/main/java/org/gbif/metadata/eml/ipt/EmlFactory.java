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
import org.gbif.metadata.eml.ipt.model.BBox;
import org.gbif.metadata.eml.ipt.model.BibliographicCitationSet;
import org.gbif.metadata.eml.ipt.model.Collection;
import org.gbif.metadata.eml.ipt.model.Eml;
import org.gbif.metadata.eml.ipt.model.GeospatialCoverage;
import org.gbif.metadata.eml.ipt.model.JGTICuratorialUnit;
import org.gbif.metadata.eml.ipt.model.KeywordSet;
import org.gbif.metadata.eml.ipt.model.PhysicalData;
import org.gbif.metadata.eml.ipt.model.Project;
import org.gbif.metadata.eml.ipt.model.ProjectAward;
import org.gbif.metadata.eml.ipt.model.StudyAreaDescription;
import org.gbif.metadata.eml.ipt.model.TaxonKeyword;
import org.gbif.metadata.eml.ipt.model.TaxonomicCoverage;
import org.gbif.metadata.eml.ipt.model.TemporalCoverage;
import org.gbif.metadata.eml.ipt.model.UserId;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.NodeCreateRule;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class is considered a utility for testing but should be migrated to the source when stable, as this is an EML
 * Model Factory based on the Apache Commons Digester and will be used when importing DwC-A.
 */
public class EmlFactory {

  /**
   * Uses rule based parsing to read the EML XML and build the EML model.
   * Note the following: - Metadata provider rules are omitted on the assumption that the provider is the same as the
   * creator - Contact rules are omitted on the assumption that contacts are covered by the creator and associated
   * parties - Publisher rules are omitted on the assumption the publisher is covered by the creator and associated
   * parties
   *
   * @param xml To read. Note this will be closed before returning
   *
   * @return The EML populated
   *
   * @throws IOException  If the Stream cannot be read from
   * @throws SAXException If the XML is not well-formed
   */
  public static Eml build(InputStream xml)
      throws IOException, SAXException, ParserConfigurationException {
    Digester digester = new Digester();
    digester.setNamespaceAware(true);

    // push the EML object onto the stack
    Eml eml = new Eml();
    digester.push(eml);

    // add the rules

    // language as xml:lang attribute
    digester.addCallMethod("eml", "setMetadataLanguage", 1);
    digester.addCallParam("eml", 0, "xml:lang");
    // guid as packageId attribute
    digester.addCallMethod("eml", "setPackageId", 1);
    digester.addCallParam("eml", 0, "packageId");

    // alternative ids
    digester.addCallMethod("eml/dataset/alternateIdentifier", "addAlternateIdentifier", 1);
    digester.addCallParam("eml/dataset/alternateIdentifier", 0);

    // title together with language
    digester.addCallMethod("eml/dataset/title", "setTitle", 2);
    digester.addCallParam("eml/dataset/title", 0);
    digester.addCallParam("eml/dataset/title", 1, "xml:lang");

    // shortName
    digester.addCallMethod("eml/dataset/shortName", "setShortName", 1);
    digester.addCallParam("eml/dataset/shortName", 0);

    digester.addBeanPropertySetter("eml/dataset/language", "language");

    // DocBook description, gettingStarted, introduction, acknowledgements, purpose
    digester.addRule("eml/dataset/abstract", new DocBookRule("setDescription", "abstract"));
    digester.addRule(
        "eml/dataset/gettingStarted", new DocBookRule("setGettingStarted", "gettingStarted"));
    digester.addRule(
        "eml/dataset/introduction", new DocBookRule("setIntroduction", "introduction"));
    digester.addRule(
        "eml/dataset/acknowledgements", new DocBookRule("setAcknowledgements", "acknowledgements"));
    digester.addRule("eml/dataset/purpose", new DocBookRule("setPurpose", "purpose"));

    digester.addBeanPropertySetter("eml/dataset/additionalInfo/para", "additionalInfo");
    digester.addRule("eml/dataset/intellectualRights/para", new NodeCreateRule(Node.ELEMENT_NODE));
    digester.addSetNext("eml/dataset/intellectualRights/para", "parseIntellectualRights");
    digester.addCallMethod("eml/dataset/methods/methodStep/description/para", "addMethodStep", 1);
    digester.addCallParam("eml/dataset/methods/methodStep/description/para", 0);
    digester.addBeanPropertySetter(
        "eml/dataset/methods/sampling/studyExtent/description/para", "studyExtent");
    digester.addBeanPropertySetter(
        "eml/dataset/methods/sampling/samplingDescription/para", "sampleDescription");
    digester.addBeanPropertySetter(
        "eml/dataset/methods/qualityControl/description/para", "qualityControl");

    digester.addCallMethod("eml/dataset/distribution/online/url", "setDistribution", 2);
    digester.addCallParam("eml/dataset/distribution/online/url", 0);
    digester.addCallParam("eml/dataset/distribution/online/url", 1, "function");

    digester.addBeanPropertySetter(
        "eml/dataset/maintenance/description/para", "updateFrequencyDescription");
    digester.addCallMethod(
        "eml/dataset/maintenance/maintenanceUpdateFrequency", "setUpdateFrequency", 1);
    digester.addCallParam("eml/dataset/maintenance/maintenanceUpdateFrequency", 0);
    digester.addCallMethod("eml/additionalMetadata/metadata/gbif/citation", "setCitation", 2);
    digester.addCallParam("eml/additionalMetadata/metadata/gbif/citation", 0);
    digester.addCallParam("eml/additionalMetadata/metadata/gbif/citation", 1, "identifier");
    digester.addCallMethod(
        "eml/additionalMetadata/metadata/gbif/specimenPreservationMethod",
        "addSpecimenPreservationMethod",
        1);
    digester.addCallParam("eml/additionalMetadata/metadata/gbif/specimenPreservationMethod", 0);
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/resourceLogoUrl", "logoUrl");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/hierarchyLevel", "hierarchyLevel");
    digester.addCallMethod("eml/dataset/pubDate", "setPubDateAsString", 1);
    digester.addCallParam("eml/dataset/pubDate", 0);

    digester.addCallMethod("eml/additionalMetadata/metadata/gbif/dateStamp", "setDateStamp", 1);
    digester.addCallParam("eml/additionalMetadata/metadata/gbif/dateStamp", 0);

    digester.addCallMethod("eml/dataset/publisher", "setPublisher", 2);
    digester.addCallParam("eml/dataset/publisher/", 0, "id");
    digester.addCallParam("eml/dataset/publisher/organizationName", 1);

    addAgentRules(digester, "eml/dataset/creator", "addCreator");
    addAgentRules(digester, "eml/dataset/metadataProvider", "addMetadataProvider");
    addAgentRules(digester, "eml/dataset/contact", "addContact");
    addAgentRules(digester, "eml/dataset/associatedParty", "addAssociatedParty");
    addKeywordRules(digester);
    addBibliographicCitations(digester);
    addGeographicCoverageRules(digester);
    addTemporalCoverageRules(digester);
    addLivingTimePeriodRules(digester);
    addFormationPeriodRules(digester);
    addTaxonomicCoverageRules(digester);
    addProjectRules(digester);
    addCollectionRules(digester);
    addPhysicalDataRules(digester);
    addJGTICuratorialIUnit(digester);

    // now parse and return the EML
    try {
      digester.parse(xml);
    } finally {
      xml.close();
    }

    return eml;
  }

  /**
   * This is a reusable set of rules to build Agents and their Addresses, and add the Agent to the predecessor object
   * on the Stack Note that we are ignoring the userId as there have been no requests for the IPT to support this.
   *
   * @param digester     to add the rules to
   * @param prefix       The XPath prefix to prepend for extracting the Agent information
   * @param parentMethod Of the previous stack object to call and add the Agent to
   */
  private static void addAgentRules(Digester digester, String prefix, String parentMethod) {
    digester.addObjectCreate(prefix, Agent.class);
    digester.addBeanPropertySetter(prefix + "/individualName/givenName", "firstName");
    digester.addBeanPropertySetter(prefix + "/individualName/surName", "lastName");
    digester.addBeanPropertySetter(prefix + "/organizationName", "organisation");
    digester.addCallMethod(prefix + "/positionName", "addPosition", 1);
    digester.addCallParam(prefix + "/positionName", 0);
    digester.addCallMethod(prefix + "/phone", "addPhone", 1);
    digester.addCallParam(prefix + "/phone", 0);
    digester.addCallMethod(prefix + "/electronicMailAddress", "addEmail", 1);
    digester.addCallParam(prefix + "/electronicMailAddress", 0);
    digester.addCallMethod(prefix + "/onlineUrl", "addHomepage", 1);
    digester.addCallParam(prefix + "/onlineUrl", 0);

    digester.addBeanPropertySetter(prefix + "/role", "role");

    digester.addObjectCreate(prefix + "/address", Address.class);
    digester.addBeanPropertySetter(prefix + "/address/city", "city");
    digester.addBeanPropertySetter(prefix + "/address/administrativeArea", "province");
    digester.addBeanPropertySetter(prefix + "/address/postalCode", "postalCode");
    digester.addBeanPropertySetter(prefix + "/address/country", "country");
    digester.addCallMethod(prefix + "/address/deliveryPoint", "addAddress", 1);
    digester.addCallParam(prefix + "/address/deliveryPoint", 0);
    digester.addSetNext(
        prefix + "/address", "setAddress"); // called on </address> to set on parent Agent

    digester.addObjectCreate(prefix + "/userId", UserId.class);
    digester.addCallMethod(prefix + "/userId", "setDirectory", 1);
    digester.addCallParam(prefix + "/userId", 0, "directory");
    digester.addBeanPropertySetter(prefix + "/userId", "identifier");
    digester.addSetNext(
        prefix + "/userId", "addUserId"); // called on </userId> to set on parent Agent

    digester.addSetNext(
        prefix, parentMethod); // method called on parent object which is the previous stack object
  }

  /**
   * Add rules to extract the keywords.
   *
   * @param digester to add the rules to
   */
  private static void addKeywordRules(Digester digester) {
    digester.addObjectCreate("eml/dataset/keywordSet", KeywordSet.class);
    digester.addCallMethod("eml/dataset/keywordSet/keyword", "add", 1);
    digester.addCallParam("eml/dataset/keywordSet/keyword", 0);
    digester.addBeanPropertySetter("eml/dataset/keywordSet/keywordThesaurus", "keywordThesaurus");
    digester.addSetNext("eml/dataset/keywordSet", "addKeywordSet"); // add the
    // KeywordSet
    // to the
    // list in
    // EML
  }

  /**
   * Add rules to extract the bibliographic citations.
   *
   * @param digester to add the rules to
   */
  private static void addBibliographicCitations(Digester digester) {
    digester.addObjectCreate(
        "eml/additionalMetadata/metadata/gbif/bibliography", BibliographicCitationSet.class);
    digester.addCallMethod("eml/additionalMetadata/metadata/gbif/bibliography/citation", "add", 2);
    digester.addCallParam("eml/additionalMetadata/metadata/gbif/bibliography/citation", 0);
    digester.addCallParam(
        "eml/additionalMetadata/metadata/gbif/bibliography/citation", 1, "identifier");
    // add the BibliographicCitations to the list in EML
    digester.addSetNext(
        "eml/additionalMetadata/metadata/gbif/bibliography", "setBibliographicCitationSet");
  }

  /**
   * Adds rules to get the geographic coverage.
   *
   * @param digester to add the rules to
   */
  private static void addGeographicCoverageRules(Digester digester) {
    digester.addObjectCreate("eml/dataset/coverage/geographicCoverage", GeospatialCoverage.class);
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/geographicCoverage/geographicDescription", "description");
    digester.addObjectCreate(
        "eml/dataset/coverage/geographicCoverage/boundingCoordinates", BBox.class);
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/geographicCoverage/boundingCoordinates/westBoundingCoordinate",
        "minX");
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/geographicCoverage/boundingCoordinates/eastBoundingCoordinate",
        "maxX");
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/geographicCoverage/boundingCoordinates/northBoundingCoordinate",
        "maxY");
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/geographicCoverage/boundingCoordinates/southBoundingCoordinate",
        "minY");
    digester.addSetNext(
        "eml/dataset/coverage/geographicCoverage/boundingCoordinates",
        "setBoundingCoordinates"); // add
    // the BBox to the GeospatialCoverage
    digester.addSetNext(
        "eml/dataset/coverage/geographicCoverage", "addGeospatialCoverage"); // add the
    // GeospatialCoverage to the list in
    // EML
  }

  /**
   * Adds rules to extract the temporal coverage.
   *
   * @param digester to add the rules to
   */
  private static void addTemporalCoverageRules(Digester digester) {
    digester.addObjectCreate("eml/dataset/coverage/temporalCoverage", TemporalCoverage.class);
    digester.addCallMethod(
        "eml/dataset/coverage/temporalCoverage/singleDateTime/calendarDate", "setStart", 1);
    digester.addCallParam("eml/dataset/coverage/temporalCoverage/singleDateTime/calendarDate", 0);
    digester.addCallMethod(
        "eml/dataset/coverage/temporalCoverage/singleDateTime/calendarDate", "setEnd", 1);
    digester.addCallParam("eml/dataset/coverage/temporalCoverage/singleDateTime/calendarDate", 0);
    digester.addCallMethod(
        "eml/dataset/coverage/temporalCoverage/rangeOfDates/beginDate/calendarDate", "setStart", 1);
    digester.addCallParam(
        "eml/dataset/coverage/temporalCoverage/rangeOfDates/beginDate/calendarDate", 0);
    digester.addCallMethod(
        "eml/dataset/coverage/temporalCoverage/rangeOfDates/endDate/calendarDate", "setEnd", 1);
    digester.addCallParam(
        "eml/dataset/coverage/temporalCoverage/rangeOfDates/endDate/calendarDate", 0);
    // add the TemporalCoverage to the list in EML
    digester.addSetNext("eml/dataset/coverage/temporalCoverage", "addTemporalCoverage");
  }

  /**
   * Adds rules to extract the livingTimePeriod temporal coverage.
   *
   * @param digester to add the rules to
   */
  private static void addLivingTimePeriodRules(Digester digester) {
    digester.addObjectCreate(
        "eml/additionalMetadata/metadata/gbif/livingTimePeriod", TemporalCoverage.class);
    digester.addCallMethod(
        "eml/additionalMetadata/metadata/gbif/livingTimePeriod", "setLivingTimePeriod", 1);
    digester.addCallParam("eml/additionalMetadata/metadata/gbif/livingTimePeriod", 0);
    digester.addSetNext(
        "eml/additionalMetadata/metadata/gbif/livingTimePeriod", "addTemporalCoverage"); // add the
    // TemporalCoverage to the list in EML
  }

  /**
   * Adds rules to extract the formationPeriod temporal coverage.
   *
   * @param digester to add the rules to
   */
  private static void addFormationPeriodRules(Digester digester) {
    digester.addObjectCreate(
        "eml/additionalMetadata/metadata/gbif/formationPeriod", TemporalCoverage.class);
    digester.addCallMethod(
        "eml/additionalMetadata/metadata/gbif/formationPeriod", "setFormationPeriod", 1);
    digester.addCallParam("eml/additionalMetadata/metadata/gbif/formationPeriod", 0);
    digester.addSetNext(
        "eml/additionalMetadata/metadata/gbif/formationPeriod", "addTemporalCoverage"); // add the
    // TemporalCoverage to the list in EML
  }

  /**
   * Adds rules to extract the taxonomic coverage.
   *
   * @param digester to add the rules to
   */
  private static void addTaxonomicCoverageRules(Digester digester) {
    digester.addObjectCreate("eml/dataset/coverage/taxonomicCoverage", TaxonomicCoverage.class);
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/taxonomicCoverage/generalTaxonomicCoverage", "description");
    digester.addObjectCreate(
        "eml/dataset/coverage/taxonomicCoverage/taxonomicClassification", TaxonKeyword.class);
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/taxonomicCoverage/taxonomicClassification/taxonRankName", "rank");
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/taxonomicCoverage/taxonomicClassification/taxonRankValue",
        "scientificName");
    digester.addBeanPropertySetter(
        "eml/dataset/coverage/taxonomicCoverage/taxonomicClassification/commonName", "commonName");
    digester.addSetNext(
        "eml/dataset/coverage/taxonomicCoverage/taxonomicClassification",
        "addTaxonKeyword"); // adds the TaxonKeyword part of the
    // TaxonomicCoverage
    digester.addSetNext(
        "eml/dataset/coverage/taxonomicCoverage",
        "addTaxonomicCoverage"); // add the TaxonomicCoverage to the list in EML
  }

  /**
   * Add rules for parsing the project details.
   *
   * @param digester to add the rules to
   */
  private static void addProjectRules(Digester digester) {
    digester.addObjectCreate("eml/dataset/project", Project.class);
    digester.addCallMethod("eml/dataset/project", "setIdentifier", 1);
    digester.addCallParam("eml/dataset/project", 0, "id");
    digester.addBeanPropertySetter("eml/dataset/project/title", "title");
    addAgentRules(digester, "eml/dataset/project/personnel", "addProjectPersonnel");
    digester.addBeanPropertySetter("eml/dataset/project/abstract/para", "description");
    digester.addBeanPropertySetter("eml/dataset/project/funding/para", "funding");
    addProjectAwardsRules(digester, "addAward");
    addRelatedProjectsRules(digester, "addRelatedProject");
    addStudyAreaDescriptionRules(digester);
    digester.addBeanPropertySetter(
        "eml/dataset/project/designDescription/description/para", "designDescription");
    digester.addSetNext("eml/dataset/project", "setProject");
  }

  private static void addProjectAwardsRules(Digester digester, String parentMethod) {
    digester.addObjectCreate("eml/dataset/project/award", ProjectAward.class);
    digester.addBeanPropertySetter("eml/dataset/project/award/funderName", "funderName");
    digester.addBeanPropertySetter("eml/dataset/project/award/awardNumber", "awardNumber");
    digester.addBeanPropertySetter("eml/dataset/project/award/title", "title");
    digester.addBeanPropertySetter("eml/dataset/project/award/awardUrl", "awardUrl");
    digester.addCallMethod("eml/dataset/project/award/funderIdentifier", "addFunderIdentifier", 0);

    digester.addSetNext("eml/dataset/project/award", parentMethod);
  }

  private static void addRelatedProjectsRules(Digester digester, String parentMethod) {
    digester.addObjectCreate("eml/dataset/project/relatedProject", Project.class);
    digester.addCallMethod("eml/dataset/project/relatedProject", "setIdentifier", 1);
    digester.addCallParam("eml/dataset/project/relatedProject", 0, "id");
    digester.addBeanPropertySetter("eml/dataset/project/relatedProject/title", "title");
    digester.addBeanPropertySetter("eml/dataset/project/relatedProject/abstract", "abstract");
    addAgentRules(digester, "eml/dataset/project/relatedProject/personnel", "addProjectPersonnel");

    digester.addSetNext("eml/dataset/project/relatedProject", parentMethod);
  }

  /**
   * Adds rules for the study area description: <studyAreaDescription> <descriptor name="generic"
   * citableClassificationSystem="false"> <descriptorValue>Turkish Mountains</descriptorValue> </descriptor>
   * </studyAreaDescription>
   *
   * @param digester To add the rules to
   */
  private static void addStudyAreaDescriptionRules(Digester digester) {
    digester.addObjectCreate(
        "eml/dataset/project/studyAreaDescription", StudyAreaDescription.class);

    // get the descriptor@name attribute and set it
    digester.addCallMethod("eml/dataset/project/studyAreaDescription/descriptor", "setName", 1);
    digester.addCallParam("eml/dataset/project/studyAreaDescription/descriptor", 0, "name");

    // get the descriptor@citableClassificationSystem and set it
    digester.addCallMethod(
        "eml/dataset/project/studyAreaDescription/descriptor", "setCitableClassificationSystem", 1);
    digester.addCallParam(
        "eml/dataset/project/studyAreaDescription/descriptor", 0, "citableClassificationSystem");

    // set the value of the StudyAreaDescription
    digester.addBeanPropertySetter(
        "eml/dataset/project/studyAreaDescription/descriptor/descriptorValue", "descriptorValue");

    // add the StudyAreaDescription to the project
    digester.addSetNext("eml/dataset/project/studyAreaDescription", "setStudyAreaDescription");
  }

  /**
   * Add rules to extract the physicalData.
   *
   * @param digester to add the rules to
   */
  private static void addPhysicalDataRules(Digester digester) {
    digester.addObjectCreate("eml/additionalMetadata/metadata/gbif/physical", PhysicalData.class);
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/physical/objectName", "name");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/physical/characterEncoding", "charset");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/physical/dataFormat/externallyDefinedFormat/formatName",
        "format");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/physical/dataFormat/externallyDefinedFormat/formatVersion",
        "formatVersion");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/physical/distribution/online/url", "distributionUrl");
    digester.addSetNext(
        "eml/additionalMetadata/metadata/gbif/physical",
        "addPhysicalData"); // add the PhysicalData to the
    // list in EML
  }

  /**
   * Add rules to extract the collection.
   *
   * @param digester to add the rules to
   */
  private static void addCollectionRules(Digester digester) {
    digester.addObjectCreate("eml/additionalMetadata/metadata/gbif/collection", Collection.class);
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/collection/parentCollectionIdentifier",
        "parentCollectionId");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/collection/collectionIdentifier", "collectionId");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/collection/collectionName", "collectionName");
    // add the Collection to the list in EML
    digester.addSetNext("eml/additionalMetadata/metadata/gbif/collection", "addCollection");
  }

  /**
   * Add rules to extract the jgtiCuratorialUnit.
   *
   * @param digester to add the rules to
   */
  private static void addJGTICuratorialIUnit(Digester digester) {
    digester.addObjectCreate(
        "eml/additionalMetadata/metadata/gbif/jgtiCuratorialUnit", JGTICuratorialUnit.class);
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/jgtiCuratorialUnit/jgtiUnitType", "unitType");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/jgtiCuratorialUnit/jgtiUnitRange/beginRange",
        "rangeStart");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/jgtiCuratorialUnit/jgtiUnitRange/endRange",
        "rangeEnd");
    digester.addBeanPropertySetter(
        "eml/additionalMetadata/metadata/gbif/jgtiCuratorialUnit/jgtiUnits", "rangeMean");
    // sets attributes of jgtiUnits (uncertaintyMeasure)
    digester.addSetProperties("eml/additionalMetadata/metadata/gbif/jgtiCuratorialUnit/jgtiUnits");
    digester.addSetNext(
        "eml/additionalMetadata/metadata/gbif/jgtiCuratorialUnit",
        "addJgtiCuratorialUnit"); // add the
    // JGTICuratorialIUnit to the list in
    // EML
  }

  // Converter to literal XML (DocBook) ant then to HTML
  public static class DocBookRule extends NodeCreateRule {

    private String method;
    private String wrapperElement;

    public DocBookRule() throws ParserConfigurationException {
      super(Node.ELEMENT_NODE);
    }

    public DocBookRule(String method, String wrapperElement) throws ParserConfigurationException {
      this.method = method;
      this.wrapperElement = wrapperElement;
    }

    @Override
    public void end(String namespace, String name) throws Exception {
      Element nodeToSerialize = super.getDigester().pop();
      String serializedNode = serializeNode(nodeToSerialize);
      invokeMethodOnTopOfStack(method, serializedNode);
    }

    protected String serializeNode(Element nodeToSerialize) throws Exception {
      String output;

      try (StringWriter writer = new StringWriter()) {
        // Create a new Document to serialize the node
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Import the node to serialize into the new Document
        Element importedNode = (Element) doc.importNode(nodeToSerialize, true);
        doc.appendChild(importedNode);

        // Set up the Transformer to handle XML serialization
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Set Transformer output properties
        transformer.setOutputProperty(OutputKeys.INDENT, "no"); // Disable indentation
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        // Serialize the node to a string
        DOMSource source = new DOMSource(importedNode);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        // Get the serialized XML string
        String serializedDocBookXml = writer.toString();

        // Handle specific whitespace formatting for <pre> tags
        serializedDocBookXml = preservePreformattedWhitespace(serializedDocBookXml);

        // Unwrap the parent tag
        String unwrappedDocBookXml = unwrapParentTag(serializedDocBookXml);

        // Convert DocBook XML to HTML
        output = unwrappedDocBookXml;
      }

      return output;
    }

    private String preservePreformattedWhitespace(String xmlString) {
      // This method preserves whitespace in <pre> tags by restoring line breaks and indentations
      xmlString = xmlString.replaceAll("(<pre>)(.*?)(</pre>)", "$1\n$2\n$3");
      return xmlString;
    }

    private String unwrapParentTag(String str) {
      return StringUtils.replaceEach(
          str,
          new String[] {"<" + wrapperElement + ">", "</" + wrapperElement + ">"},
          new String[] {"", ""});
    }

    protected void invokeMethodOnTopOfStack(String methodName, String param) throws Exception {
      Object objOnTopOfStack = getDigester().peek();
      MethodUtils.invokeExactMethod(objOnTopOfStack, methodName, param);
    }
  }
}
