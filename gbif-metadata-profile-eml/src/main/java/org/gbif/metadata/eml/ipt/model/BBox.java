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
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Bounding box representation with 2 points. P1 has maximum latitude and longitude, while p2 represents the minimum
 * ones, i.e. p1 is NorthEast, p2 is SouthWest
 */
public class BBox implements Serializable {

  // x = east/west=longitude, -180/180
  // y = north/south = latitude, -90/90
  private Point max;

  private Point min;

  public static BBox newWorldInstance() {
    return new BBox(new Point(-90.0, -180.0), new Point(90.0, 180.0));
  }

  public BBox() {
    this(-90d, -180d, 90d, 180d);
  }

  public BBox(Point min, Point max) {
    this.min = new Point(min);
    this.max = new Point(max);
    setOrderedBounds(min.getLatitude(), min.getLongitude(), max.getLatitude(), max.getLongitude());
  }

  public BBox(Double minY, Double minX, Double maxY, Double maxX) {
    minY = minY == null ? -90 : minY;
    minX = minX == null ? -180 : minX;
    maxY = maxY == null ? 90 : maxY;
    maxX = maxX == null ? 180 : maxX;
    min = new Point(minY, minX);
    max = new Point(maxY, maxX);
    setOrderedBounds(minY, minX, maxY, maxX);
  }

  public Point getMax() {
    return max;
  }

  public Point getMin() {
    return min;
  }

  /**
   * Returns the Point of the bbox centre
   */
  public Point centre() {
    if (max == null || min == null) {
      return null;
    }
    return new Point(min.getLatitude() + height() / 2.0, min.getLongitude() + width() / 2.0);
  }

  /**
   * Check if point lies within this bbox
   */
  public boolean contains(Point p) {
    if (p != null && p.isValid() && isValid()) {
      if (p.getLatitude() <= max.getLatitude()
          && p.getLatitude() >= min.getLatitude()
          && p.getLongitude() <= max.getLongitude()
          && p.getLongitude() >= min.getLongitude()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Expands bounding box boundaries to fit this coordinate into the box
   */
  public void expandBox(Point p) {
    if (p != null && p.isValid()) {
      if (!contains(p)) {
        if (isValid()) {
          if (p.getLatitude() > max.getLatitude()) {
            max.setLatitude(p.getLatitude());
          }
          if (p.getLatitude() < min.getLatitude()) {
            min.setLatitude(p.getLatitude());
          }
          if (p.getLongitude() > max.getLongitude()) {
            max.setLongitude(p.getLongitude());
          }
          if (p.getLongitude() < min.getLongitude()) {
            min.setLongitude(p.getLongitude());
          }
        } else {
          // this BBox doesnt yet contain any points. Use this point for min+max
          setMin(p);
          setMax(p);
        }
      }
    }
  }

  /**
   * Expands BBox so that its longitude/latitude ratio becomes 2:1 which is often used for maps (360° : 180°).
   */
  public void expandToMapRatio() {
    expandToMapRatio(2f);
  }

  /**
   * Expands BBox so that its longitude/latitude ratio becomes the specified width/height ratio. Takes care to not go
   * beyond the -180/180 + -90/90 bboc limits and might shift the center of the bbox if this is not otherwise possible.
   */
  public void expandToMapRatio(double mapRatio) {
    // longitude=x, latitude=y
    if (isValid()) {
      double width = max.getLongitude() - min.getLongitude();
      double height = max.getLatitude() - min.getLatitude();
      double ratio = width / height;

      if (mapRatio > ratio) {
        // was rather a square before. need to extend the latitude on both
        // min+max
        double equalWidthIncrease = ((height * mapRatio) - width) / 2;
        double minX = min.getLongitude();
        double maxX = max.getLongitude();
        if (minX - equalWidthIncrease < Point.MIN_LONGITUDE) {
          minX = Point.MIN_LONGITUDE;
          maxX = width * mapRatio;
        } else if (maxX + equalWidthIncrease > Point.MAX_LONGITUDE) {
          minX = Point.MAX_LONGITUDE - width * mapRatio;
          maxX = Point.MAX_LONGITUDE;
        } else {
          minX -= equalWidthIncrease;
          maxX += equalWidthIncrease;
        }
        min.setLongitude(minX);
        max.setLongitude(maxX);
      } else if (mapRatio < ratio) {
        // was more of a flat rectangle before. need to extend the longitude on
        // both min+max
        double equalHeightIncrease = ((width / mapRatio) - height) / 2;
        double minY = min.getLatitude();
        double maxY = max.getLatitude();
        if (minY - equalHeightIncrease < Point.MIN_LATITUDE) {
          minY = Point.MIN_LATITUDE;
          maxY = width * mapRatio;
        } else if (maxY + equalHeightIncrease > Point.MAX_LATITUDE) {
          minY = Point.MAX_LATITUDE - width * mapRatio;
          maxY = Point.MAX_LATITUDE;
        } else {
          minY -= equalHeightIncrease;
          maxY += equalHeightIncrease;
        }
        min.setLatitude(minY);
        max.setLatitude(maxY);
      }
    }
  }

  public double height() {
    if (max == null || min == null) {
      return 0.0;
    }
    return max.getY() - min.getY();
  }

  public boolean isValid() {
    return min != null && max != null && min.isValid() && max.isValid();
  }

  public boolean overlaps(BBox bbox) {
    if (bbox == null || !bbox.isValid()) {
      throw new IllegalArgumentException();
    }
    Point c1 = this.centre();
    Point c2 = bbox.centre();
    // If distance between centre X's is smaller than the sum of half the widths
    // of the two boxes and
    // the distance between centre Y's is smaller than the sum of half the
    // heights of the two boxes,
    // then the two boxes overlap! A shared boundary is not considered an
    // overlap here.
    return c1.distanceX(c2) < (this.width() / 2.0 + bbox.width() / 2.0)
        && (c1.distanceY(c2) < (this.height() / 2.0 + bbox.height() / 2.0));
  }

  /**
   * Try to expand BBox by factor given but keep box centered and expand to maximum possible in case we reach world
   * limits.
   *
   * @param factor 0-1 for shrinking, >1 for expanding boxes
   */
  public void resize(double factor) {
    if (factor < 0f) {
      throw new IllegalArgumentException("Factor must be larger than 0");
    }
    if (!isValid()) {
      throw new IllegalStateException("BBox is not valid");
    }
    double minX = min.getLongitude();
    double minY = min.getLatitude();
    double maxX = max.getLongitude();
    double maxY = max.getLatitude();
    double width = maxX - minX;
    double height = maxY - minY;
    // detect maximum possible expand factor
    double[] maxFactors = {
      (factor - 1) / 2f,
      (Point.MAX_LATITUDE - maxY) / height,
      (Point.MAX_LATITUDE + minY) / height,
      (Point.MAX_LONGITUDE - maxX) / width,
      (Point.MAX_LONGITUDE + minX) / width
    };
    Arrays.sort(maxFactors);
    double expandFactor = maxFactors[0];
    // change bbox
    minX -= (expandFactor * width);
    maxX += (expandFactor * width);
    minY -= (expandFactor * height);
    maxY += (expandFactor * height);
    min.setLongitude(minX);
    min.setLatitude(minY);
    max.setLongitude(maxX);
    max.setLatitude(maxY);
  }

  public void setMax(Point max) {
    if (max == null) {
      this.max = null;
    } else {
      if (min == null) {
        min = new Point(max.getX(), max.getY());
      }
      setOrderedX(min.getX(), max.getX());
      setOrderedY(min.getY(), max.getY());
    }
  }

  /**
   * Sets the east bounding coordinate as given: a box crossing the 180th meridian has its west edge east of its east
   * edge, so the longitudes are not reordered.
   */
  public void setMaxX(String s) {
    try {
      max.setLongitude(Double.parseDouble(s));
    } catch (NumberFormatException ignored) {
      // not a Double, so not much can be done really
    }
  }

  /**
   * Sets the northing coordinate
   */
  public void setMaxY(String s) {
    try {
      setOrderedY(max.getLatitude(), Double.parseDouble(s));
    } catch (NumberFormatException ignored) {
      // not a Double, so not much can be done really
    }
  }

  public void setMin(Point min) {
    if (min == null) {
      this.min = null;
    } else {
      if (max == null) {
        max = new Point(min.getX(), min.getY());
      }
      setOrderedX(this.min.getX(), max.getX());
      setOrderedY(this.min.getY(), max.getY());
    }
  }

  /**
   * Sets the west bounding coordinate as given: a box crossing the 180th meridian has its west edge east of its east
   * edge, so the longitudes are not reordered.
   */
  public void setMinX(String s) {
    try {
      min.setLongitude(Double.parseDouble(s));
    } catch (NumberFormatException ignored) {
      // not a Double, so not much can be done really
    }
  }

  /**
   * Sets the southing coordinate
   */
  public void setMinY(String s) {
    try {
      setOrderedY(min.getLatitude(), Double.parseDouble(s));
    } catch (NumberFormatException ignored) {
      // not a Double, so not much can be done really
    }
  }

  // Insure p1 is NE (max lat and long) and p2 is SW (min lat and long)
  public void setOrderedBounds(Double minY, Double minX, Double maxY, Double maxX) {
    setOrderedX(minX, maxX);
    setOrderedY(minY, maxY);
  }

  // Insure that the greater of the x values goes in max and the lesser in min
  public void setOrderedX(Double x1, Double x2) {
    if (max == null) {
      max = new Point(x2, 0d);
    }
    if (min == null) {
      min = new Point(x1, 0d);
    }
    if (x1 > x2) {
      min.setLongitude(x2);
      max.setLongitude(x1);
    } else {
      min.setLongitude(x1);
      max.setLongitude(x2);
    }
  }

  // Insure that the greater of the y values goes in max and the lesser in min
  public void setOrderedY(Double y1, Double y2) {
    if (max == null) {
      max = new Point(0d, y2);
    }
    if (min == null) {
      min = new Point(0d, y1);
    }
    if (y1 > y2) {
      min.setLatitude(y2);
      max.setLatitude(y1);
    } else {
      min.setLatitude(y1);
      max.setLatitude(y2);
    }
  }

  /**
   * returns the size of the surface defined by this bbox
   */
  public double surface() {
    return width() * height();
  }

  /*
   * @See http://georss.org/simple
   *
   * @return polygon string which is a space separated list of
   * latitude-longitude pairs
   */
  public String toStringGeoRSS() {
    return String.format(
        "%s   %s %s   %s   %s %s",
        min.toStringSpace(),
        max.getLongitude(),
        min.getLatitude(),
        max.toStringSpace(),
        min.getLongitude(),
        max.getLatitude());
  }

  public String toStringShort(int decimals) {
    // minY,minX maxY,maxX
    return String.format("%s %s", min.toStringShort(decimals), max.toStringShort(decimals));
  }

  /**
   * format used in WMS for bboxes: minX(longitude), minY(latitude), maxX, maxY
   */
  public String toStringWMS() {
    // minX,minY,maxX,maxY
    return String.format(
        "%s,%s,%s,%s",
        min.getLongitude(), min.getLatitude(), max.getLongitude(), max.getLatitude());
  }

  public double width() {
    if (max == null || min == null) {
      return 0.0;
    }
    return max.getX() - min.getX();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BBox bBox = (BBox) o;
    return Objects.equals(max, bBox.max) && Objects.equals(min, bBox.min);
  }

  @Override
  public int hashCode() {
    return Objects.hash(max, min);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BBox.class.getSimpleName() + "[", "]")
        .add("max=" + max)
        .add("min=" + min)
        .toString();
  }
}
