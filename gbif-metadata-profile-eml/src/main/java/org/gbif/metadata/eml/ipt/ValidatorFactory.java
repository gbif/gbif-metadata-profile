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

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

/**
 * For validation of EML files.
 *
 * @see <a href="http://knb.ecoinformatics.org/emlparser/">EML Parser</a>
 */
public class ValidatorFactory {

  public static final String EML_SCHEMA_URL = "https://rs.gbif.org/schema/eml-2.2.0/eml.xsd";

  public static final String EML_GBIF_PROFILE_SCHEMA_URL =
      "https://rs.gbif.org/schema/eml-gbif-profile/1.3/eml-gbif-profile.xsd";

  /**
   * @return xml validator based on the official EML 2.2.0 xml schema hosted at GBIF for network performance issues only.
   */
  public static Validator getEmlValidator() throws MalformedURLException, SAXException {
    return getValidator(EML_SCHEMA_URL);
  }

  public static Validator getGbifValidator() throws MalformedURLException, SAXException {
    return getValidator(EML_GBIF_PROFILE_SCHEMA_URL);
  }

  private static Validator getValidator(String schemaUrl)
      throws MalformedURLException, SAXException {
    // define the type of schema - we use W3C:
    String schemaLang = "http://www.w3.org/2001/XMLSchema";
    // get validation driver:
    SchemaFactory factory = SchemaFactory.newInstance(schemaLang);
    // create schema by reading it from a URL:
    Schema schema = factory.newSchema(new URL(schemaUrl));
    return schema.newValidator();
  }
}
