/*
 * Copyright 2009 GBIF.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gbif.metadata.eml;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * Joint GeoTaxonomic Index (JGTI) Curatorial Unit A quantitative descriptor (number of specimens, samples or batches).
 * The actual quantification could be covered by 1) an exact number of JGI-units in the collection plus a measure of
 * uncertainty (+/- x); 2) a range of numbers (x to x), with the lower value representing an exact number, when the
 * higher value is omitted.
 */
public class JGTICuratorialUnit implements Serializable {

  /**
   * Generated
   */
  private static final long serialVersionUID = 4302214747473277031L;

  private String unitType;
  private Integer rangeStart;
  private Integer rangeEnd;
  private Integer rangeMean;
  private Integer uncertaintyMeasure;

  /**
   * Required by Struts2
   */
  public JGTICuratorialUnit() {
  }

  public String getUnitType() {
    return unitType;
  }

  public Integer getRangeStart() {
    return rangeStart;
  }

  public Integer getRangeEnd() {
    return rangeEnd;
  }

  public Integer getRangeMean() {
    return rangeMean;
  }

  public Integer getUncertaintyMeasure() {
    return uncertaintyMeasure;
  }

  public void setUnitType(String unitType) {
    this.unitType = unitType;
  }

  public void setRangeStart(Integer rangeStart) {
    this.rangeStart = rangeStart;
  }

  public void setRangeEnd(Integer rangeEnd) {
    this.rangeEnd = rangeEnd;
  }

  public void setRangeMean(Integer rangeMean) {
    this.rangeMean = rangeMean;
  }

  public void setUncertaintyMeasure(Integer uncertaintyMeasure) {
    this.uncertaintyMeasure = uncertaintyMeasure;
  }

  public JGTICuratorialUnitType getType() {
    if (uncertaintyMeasure != null) {
      return JGTICuratorialUnitType.COUNT_WITH_UNCERTAINTY;
    }
    return JGTICuratorialUnitType.COUNT_RANGE;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(unitType, rangeStart, rangeEnd, rangeMean, uncertaintyMeasure);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final JGTICuratorialUnit other = (JGTICuratorialUnit) obj;
    return Objects.equal(this.unitType, other.unitType) && Objects.equal(this.rangeStart, other.rangeStart) && Objects
      .equal(this.rangeEnd, other.rangeEnd) && Objects.equal(this.rangeMean, other.rangeMean) && Objects
      .equal(this.uncertaintyMeasure, other.uncertaintyMeasure);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).
      add("unitType", unitType).
      add("rangeStart", rangeStart).
      add("rangeEnd", rangeEnd).
      add("rangeMean", rangeMean).
      add("uncertaintyMeasure", uncertaintyMeasure).
      toString();
  }
}
