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

import org.gbif.metadata.eml.ipt.model.Eml;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class IptEmlWriter {

  private static final String EML_TEMPLATE = "eml-ipt.ftl";

  protected static final Configuration FTL = provideFreemarker();

  private static String processTemplateIntoString(Template template, Object model)
      throws IOException, TemplateException {
    StringWriter result = new StringWriter();
    template.process(model, result);
    return result.toString();
  }

  /**
   * Provides a freemarker template loader. It is configured to access the utf8 templates folder on the classpath, i.e.
   * /src/resources/templates
   */
  private static Configuration provideFreemarker() {
    // load templates from classpath by prefixing /templates
    TemplateLoader tl = new ClassTemplateLoader(IptEmlWriter.class, "/templates");

    Configuration fm = new Configuration(Configuration.VERSION_2_3_31);
    fm.setDefaultEncoding("utf8");
    fm.setTemplateLoader(tl);

    return fm;
  }

  /**
   * Writes a map of data to a utf8 encoded file using a Freemarker {@link Configuration}.
   */
  public static void writeFile(File f, String template, Object data)
      throws IOException, TemplateException {
    String result = processTemplateIntoString(FTL.getTemplate(template), data);
    Writer out = FileUtils.startNewUtf8File(f);
    out.write(result);
    out.close();
  }

  /**
   * Writes an {@link Eml} object to an XML file using a Freemarker {@link Configuration}.
   *
   * @param f   the XML file to write to
   * @param eml the EML object
   */
  public static void writeEmlFile(File f, Eml eml) throws IOException, TemplateException {
    Map<String, Object> map = new HashMap<>();
    map.put("eml", eml);
    writeFile(f, EML_TEMPLATE, map);
  }

  /**
   * Writes an {@link Eml} object to a string using a Freemarker {@link Configuration}.
   *
   * @param eml the EML object
   * @return the XML string
   */
  public static String writeEmlAsString(Eml eml) throws IOException, TemplateException {
    Map<String, Object> map = new HashMap<>();
    map.put("eml", eml);
    return processTemplateIntoString(FTL.getTemplate(EML_TEMPLATE), map);
  }
}
