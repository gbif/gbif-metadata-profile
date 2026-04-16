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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ProjectAward is used to enter information about a funding award associated with a project. The containing project
 * contains the list of investigators and for the award, while the `award` field contains specifics such as the agency
 * name, award number, and funding program identifiers.
 */
@Setter
@Getter
@EqualsAndHashCode
@ToString
public class ProjectAward {

  /**
   * The name of the funding institution that made this award.
   *
   * @see <a href="https://eml.ecoinformatics.org/schema/eml-project_xsd.html#AwardType_funderName">AwardType/funderName</a>
   */
  private String funderName;

  /**
   * The identifier of the funding agency.
   *
   * @see <a href="https://eml.ecoinformatics.org/schema/eml-project_xsd.html#AwardType_funderIdentifier">AwardType/funderIdentifier</a>
   */
  private List<String> funderIdentifiers = new ArrayList<>();

  /**
   * The assigned award number.
   *
   * @see <a href="https://eml.ecoinformatics.org/schema/eml-project_xsd.html#AwardType_awardNumber">AwardType/awardNumber</a>
   */
  private String awardNumber;

  /**
   * The title of the award.
   *
   * @see <a href="https://eml.ecoinformatics.org/schema/eml-project_xsd.html#AwardType_title">AwardType/title</a>
   */
  private String title;

  /**
   *  The URL associated with award.
   *
   * @see <a href="https://eml.ecoinformatics.org/schema/eml-project_xsd.html#AwardType_awardUrl">AwardType/awardUrl</a>
   */
  private String awardUrl;

  public void addFunderIdentifier(String funderIdentifier) {
    if (StringUtils.isNotEmpty(funderIdentifier)) {
      funderIdentifiers.add(funderIdentifier);
    }
  }
}
