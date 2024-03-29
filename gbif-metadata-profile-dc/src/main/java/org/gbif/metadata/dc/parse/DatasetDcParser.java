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
package org.gbif.metadata.dc.parse;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.metadata.common.parse.DatasetWrapper;
import org.gbif.metadata.common.util.MetadataUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.digester3.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static org.gbif.api.vocabulary.MetadataType.DC;

/**
 * Main parser of dataset metadata that uses parser specific digester RuleSets for Dublin Core.
 * It can automatically detect the document type, and if it should match DC.
 * For EML use DatasetEmlParser from eml module.
 * <p>
 * This parser and its digester rules use the DatasetDelegator class to wrap a dataset and set
 * complex bean components.
 */
public class DatasetDcParser {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetDcParser.class);

  private DatasetDcParser() {
    // empty constructor
  }

  /**
   * Build from byte array on-top of a preexisting Dataset populating its fields from a source metadata
   * that's parsed.
   *
   * @param data to read
   * @return The Dataset populated, never null
   * @throws java.io.IOException If the Stream cannot be read from
   * @throws IllegalArgumentException If the XML is not well-formed or is not understood
   */
  public static Dataset build(byte[] data) throws IOException {
    try (InputStream streamToDetectMetadataType = new ByteArrayInputStream(data);
        InputStream mainStream = new ByteArrayInputStream(data)) {
      MetadataType metadataType = MetadataUtils.detectParserType(streamToDetectMetadataType);
      // make sure metadata type is DC!
      if (metadataType != DC) {
        throw new IOException("Wrong metadata type " + metadataType + ", use proper parser!");
      }
      return parse(mainStream);
    }
  }

  public static Dataset parse(InputStream xml) throws IOException {
    LOG.debug("Parsing DC document");
    Digester digester = new Digester();
    digester.setNamespaceAware(true);
    // add digester rules based on parser type
    digester.addRuleSet(new DublinCoreRuleSet());

    // push the Delegating object onto the stack
    DatasetWrapper delegator = new DatasetWrapper();
    digester.push(delegator);

    // now parse and return the dataset
    try {
      digester.parse(xml);
    } catch (ConversionException e) {
      // swallow
    } catch (SAXException e) {
      if (e.getException() == null
          || !e.getException().getClass().equals(ConversionException.class)) {
        // allow type conversions to happen
        throw new IllegalArgumentException("Invalid metadata xml document", e);
      }
    } finally {
      delegator.postProcess();
    }

    return delegator.getTarget();
  }
}
