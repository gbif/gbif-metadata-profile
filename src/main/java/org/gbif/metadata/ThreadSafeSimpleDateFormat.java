/***************************************************************************
 * Copyright 2010 Global Biodiversity Information Facility Secretariat
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
 ***************************************************************************/

package org.gbif.metadata;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author markus
 *
 */
public class ThreadSafeSimpleDateFormat{

  private DateFormat df;

  public ThreadSafeSimpleDateFormat(String format) {
    this.df = new SimpleDateFormat(format);
  }

  public synchronized String format(Date date) {
    return df.format(date);
  }

  public synchronized Date parse(String string) throws ParseException {
    return df.parse(string);
  }
}