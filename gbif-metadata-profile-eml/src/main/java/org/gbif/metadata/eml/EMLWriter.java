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
package org.gbif.metadata.eml;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriodType;
import org.gbif.api.util.ContactAdapter;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.metadata.common.parse.ParagraphContainer;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * A simple tool to serialize a dataset object into an XML document compliant with the latest
 * version of the GBIF Metadata Profile, currently version 1.3.
 */
@ThreadSafe
public class EMLWriter {

  private static final Logger LOG = LoggerFactory.getLogger(EMLWriter.class);

  // Define pairs of DocBook tags. MUST MATCH HTML tags!
  private static final String[] DOCBOOK_TAGS = {
    "<section>", "</section>",
    "<title>", "</title>",
    "<title>", "</title>",
    "<title>", "</title>",
    "<title>", "</title>",
    "<title>", "</title>",
    "<para><itemizedlist>", "</itemizedlist></para>",
    "<para><orderedlist>", "</orderedlist></para>",
    "<listitem><para>", "</para></listitem>",
    "<para>", "</para>",
    "<emphasis>", "</emphasis>",
    "<subscript>", "</subscript>",
    "<superscript>", "</superscript>",
    "<literalLayout>", "</literalLayout>"
  };

  // Define pairs of HTML tags. MUST MATCH DocBook tags!
  private static final String[] HTML_TAGS = {
    "<div>", "</div>",
    "<h1>", "</h1>",
    "<h2>", "</h2>",
    "<h3>", "</h3>",
    "<h4>", "</h4>",
    "<h5>", "</h5>",
    "<ul>", "</ul>",
    "<ol>", "</ol>",
    "<li>", "</li>",
    "<p>", "</p>",
    "<b>", "</b>",
    "<sub>", "</sub>",
    "<sup>", "</sup>",
    "<pre>", "</pre>"
  };

  // List of allowed DocBook tags
  private static final String[] ALLOWED_DOCBOOK_TAGS = {
    "section",
    "title",
    "para",
    "itemizedlist",
    "orderedlist",
    "listitem",
    "emphasis",
    "subscript",
    "superscript",
    "literalLayout",
    "ulink",
    "citetitle"
  };

  private static final String TEMPLATE_PATH = "/gbif-eml-profile-template";
  private final Configuration freemarkerConfig;
  private final boolean useDoiAsIdentifier;
  private final boolean omitXmlDeclaration;

  /**
   * Private constructor, use {@link #newInstance()}
   *
   * @param cfg
   */
  private EMLWriter(Configuration cfg, boolean useDoiAsIdentifier, boolean omitXmlDeclaration) {
    this.freemarkerConfig = cfg;
    this.useDoiAsIdentifier = useDoiAsIdentifier;
    this.omitXmlDeclaration = omitXmlDeclaration;
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration. Same as calling {@link
   * #newInstance(boolean)} method with useDoiAsIdentifier = false
   *
   * @return new instance
   */
  public static EMLWriter newInstance() {
    return new EMLWriter(
        DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH), false, false);
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration.
   *
   * @param useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi
   *     won't be included in the list of alternate identifiers
   * @return
   */
  public static EMLWriter newInstance(boolean useDoiAsIdentifier) {
    return new EMLWriter(
        DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH),
        useDoiAsIdentifier,
        false);
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration.
   *
   * @param useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi
   *     won't be included in the list of alternate identifiers
   * @param omitXmlDeclaration should the XML declaration be omitted in the generated document
   * @return
   */
  public static EMLWriter newInstance(boolean useDoiAsIdentifier, boolean omitXmlDeclaration) {
    return new EMLWriter(
        DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH),
        useDoiAsIdentifier,
        omitXmlDeclaration);
  }

  /**
   * Write a document from a Dataset object for the latest version.
   *
   * @param dataset non null dataset object
   * @param writer where the output document will go. The writer is not closed by this method.
   * @throws IOException if an error occurs while processing the template
   */
  public void writeTo(Dataset dataset, Writer writer) throws IOException {
    innerWrite(dataset, writer, EMLProfileVersion.GBIF_1_3);
  }

  /**
   * Write a document from a Dataset object for a specific EML version.
   *
   * @param dataset non null dataset object
   * @param writer where the output document will go. The writer is not closed by this method.
   * @param emlProfileVersion EML profile version
   * @throws IOException if an error occurs while processing the template
   */
  public void writeTo(Dataset dataset, Writer writer, EMLProfileVersion emlProfileVersion)
      throws IOException {
    innerWrite(dataset, writer, emlProfileVersion);
  }

  private void innerWrite(Dataset dataset, Writer writer, EMLProfileVersion emlProfileVersion)
      throws IOException {
    Objects.requireNonNull(dataset, "Dataset can't be null");

    Map<String, Object> map = new HashMap<>();
    map.put("dataset", dataset);
    map.put("eml", new EmlDatasetWrapper(dataset));
    map.put("useDoiAsIdentifier", useDoiAsIdentifier);
    map.put("omitXmlDeclaration", omitXmlDeclaration);

    String emlTemplate = String.format("eml-dataset-%s.ftl", emlProfileVersion.getVersion());

    try {
      freemarkerConfig.getTemplate(emlTemplate).process(Collections.unmodifiableMap(map), writer);
    } catch (TemplateException e) {
      throw new IOException(
          "Error while processing the EML Freemarker template for dataset " + dataset.getKey(), e);
    }
  }

  /**
   * Wrapper for a dataset instance that exposes some EML specific methods. Mostly used for
   * generating EML, see EMLWriter. This class requires to be public to be used in the Freemarker
   * template.
   */
  public static class EmlDatasetWrapper {

    private final Dataset dataset;
    private final ContactAdapter contactAdapter;

    public EmlDatasetWrapper(Dataset dataset) {
      this.dataset = dataset;
      this.contactAdapter = new ContactAdapter(dataset.getContacts());
    }

    public List<Contact> getAssociatedParties() {
      return contactAdapter.getAssociatedParties();
    }

    public Contact getResourceCreator() {
      return contactAdapter.getResourceCreator();
    }

    /** @return list of {@link Contact} of type ContactType.ORIGINATOR */
    public List<Contact> getCreators() {
      return contactAdapter.getCreators();
    }

    public Contact getAdministrativeContact() {
      return contactAdapter.getAdministrativeContact();
    }

    /** @return list of {@link Contact} of type ContactType.ADMINISTRATIVE_POINT_OF_CONTACT */
    public List<Contact> getContacts() {
      return contactAdapter.getContacts();
    }

    public List<String> getDescription() {
      return new ParagraphContainer(dataset.getDescription()).getParagraphs();
    }

    // Value with all HTML tags replaced by DocBook analogues
    public String getDocBookField(String fieldName) {
      String result = null;

      try {
        String value = BeanUtils.getProperty(dataset, fieldName);

        if (value != null) {
          result = replaceDocBookElements(value.trim());
        }
      } catch (Exception e) {
        LOG.error("Error getting document field", e);
      }

      return result;
    }

    private String replaceDocBookElements(String value) {
      // Handle <a> to <ulink> conversion
      String htmlStringWithLinksReplaced =
          value.replaceAll(
              "<a\\s+href=\"(.*?)\">\\s*(.*?)\\s*</a>",
              "<ulink url=\"$1\"><citetitle>$2</citetitle></ulink>");

      // Perform replacements
      String docBookString =
          StringUtils.replaceEach(htmlStringWithLinksReplaced, HTML_TAGS, DOCBOOK_TAGS);

      // Escape special characters except for allowed DocBook tags
      return escapeExceptAllowedTags(docBookString);
    }

    private String escapeExceptAllowedTags(String input) {
      StringBuilder output = new StringBuilder();

      // Use regex to split input into tags and text segments
      String[] parts = input.split("(?=<[^>]+>)|(?<=>)");

      for (String part : parts) {
        // No need to trim whitespace here to preserve leading/trailing spaces
        if (part.matches("^<[^>]+>$")) {
          // If it's a tag, check if it's an allowed HTML tag
          String tagName = getTagName(part);
          if (isAllowedDocBookTag(tagName) || isDocBookLink(part)) {
            // Preserve allowed tags as-is
            output.append(part);
          } else {
            // Escape non-allowed tags
            output.append(customEscape(part));
          }
        } else {
          // Escape special characters in text
          output.append(customEscape(part));
        }
      }

      return output.toString();
    }

    // Helper method to extract the tag name (without <>)
    private String getTagName(String tag) {
      return tag.replaceAll("[<>/]", "").split("\\s+")[0];
    }

    // Helper method to check if a tag is an allowed DocBook tag
    private boolean isAllowedDocBookTag(String tagName) {
      for (String allowedTag : ALLOWED_DOCBOOK_TAGS) {
        if (allowedTag.equalsIgnoreCase(tagName)) {
          return true;
        }
      }
      return false;
    }

    // Helper method to check if a tag is an DocBook ulink
    private boolean isDocBookLink(String tag) {
      return tag.matches("^<ulink\\s+url=\".*?\">.*?</ulink>$");
    }

    private String customEscape(String input) {
      StringBuilder escaped = new StringBuilder();
      int length = input.length();

      for (int i = 0; i < length; i++) {
        char c = input.charAt(i);

        // Check for '&' to identify potential escaped entities
        if (c == '&' && i + 3 < length) {
          // Extract the next few characters after '&' to check if it's a known escaped entity
          String potentialEntity =
              input.substring(i, Math.min(i + 6, length)); // "&nbsp;" is 6 characters

          if (potentialEntity.startsWith("&nbsp;")) {
            // Replace &nbsp; with &#160;
            escaped.append("&#160;");
            i += 5; // Skip the characters of "&nbsp;"
            continue;
          } else if (potentialEntity.startsWith("&amp;")
              || potentialEntity.startsWith("&lt;")
              || potentialEntity.startsWith("&gt;")
              || potentialEntity.startsWith("&quot;")
              || potentialEntity.startsWith("&apos;")) {
            // If it's an already escaped entity, append it as-is and skip ahead
            escaped.append(potentialEntity);
            i += potentialEntity.length() - 1; // Skip the already escaped entity
            continue;
          }
        }

        // Now escape only unescaped characters
        switch (c) {
          case '&':
            escaped.append("&amp;");
            break;
          case '<':
            escaped.append("&lt;");
            break;
          case '>':
            escaped.append("&gt;");
            break;
          default:
            // Preserve other characters (including Unicode characters)
            escaped.append(c);
        }
      }

      return escaped.toString();
    }

    public Contact getMetadataProvider() {
      return contactAdapter.getFirstPreferredType(ContactType.METADATA_AUTHOR);
    }

    /** @return list of {@link Contact} of type ContactType.METADATA_AUTHOR */
    public List<Contact> getMetadataProviders() {
      return contactAdapter.getMetadataProviders();
    }

    /**
     * @return list of all formation periods {@link VerbatimTimePeriodType} of type
     *     VerbatimTimePeriodType.FORMATION_PERIOD
     */
    public List<VerbatimTimePeriod> getFormationPeriods() {
      return getTimePeriods(VerbatimTimePeriodType.FORMATION_PERIOD);
    }

    /**
     * @return list of all formation periods {@link VerbatimTimePeriodType} of type
     *     VerbatimTimePeriodType.LIVING_TIME_PERIOD
     */
    public List<VerbatimTimePeriod> getLivingTimePeriods() {
      return getTimePeriods(VerbatimTimePeriodType.LIVING_TIME_PERIOD);
    }

    /** @return list of all {@link VerbatimTimePeriodType} of specified type */
    private List<VerbatimTimePeriod> getTimePeriods(VerbatimTimePeriodType type) {
      List<VerbatimTimePeriod> periods = new ArrayList<>();
      for (TemporalCoverage tc : dataset.getTemporalCoverages()) {
        if (tc instanceof VerbatimTimePeriod) {
          VerbatimTimePeriod tp = (VerbatimTimePeriod) tc;
          if (type.equals(tp.getType())) {
            periods.add(tp);
          }
        }
      }
      return periods;
    }

    /**
     * @return list of all {@link SingleDate} and {@link DateRange} {@link TemporalCoverage} or an
     *     empty list if none found
     */
    public List<TemporalCoverage> getSingleDateAndDateRangeCoverages() {
      List<TemporalCoverage> periods = new ArrayList<>();
      for (TemporalCoverage tc : dataset.getTemporalCoverages()) {
        if (tc instanceof DateRange || tc instanceof SingleDate) {
          periods.add(tc);
        }
      }
      return periods;
    }
  }
}
