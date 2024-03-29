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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of JGTICuratorial Unit types.
 */
public enum JGTICuratorialUnitType implements Serializable {
  COUNT_WITH_UNCERTAINTY("countWithUncertainty"),
  COUNT_RANGE("countRange");

  public static final Map<String, String> HTML_SELECT_MAP;
  private final String name;

  /**
   * Returns a jgtiCuratorialUnitType created from a string description of the type. If the
   * description is null or if it's not a valid JGTICuratorialUnitType name, null is returned.
   *
   * @param type the JGTI Curatorial Unit type as a string
   *
   * @return JGTICuratorialUnitType
   */
  public static JGTICuratorialUnitType fromString(String type) {
    if (type == null) {
      return null;
    }
    type = type.trim();
    for (JGTICuratorialUnitType r : JGTICuratorialUnitType.values()) {
      if (r.name.equalsIgnoreCase(type)) {
        return r;
      }
    }
    return null;
  }

  JGTICuratorialUnitType(String name) {
    this.name = name;
  }

  static {
    Map<String, String> map = new HashMap<>();
    for (JGTICuratorialUnitType rt : JGTICuratorialUnitType.values()) {
      map.put(rt.name(), "jgtiCuratorialUnitType." + rt.name());
    }
    HTML_SELECT_MAP = Collections.unmodifiableMap(map);
  }

  public String getName() {
    return name;
  }
}
