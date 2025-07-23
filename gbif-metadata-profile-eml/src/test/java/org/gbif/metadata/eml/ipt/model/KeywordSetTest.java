package org.gbif.metadata.eml.ipt.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeywordSetTest {

  @Test
  public void testSetKeywordsStringEmptyKeywords() {
    KeywordSet set = new KeywordSet();
    set.setKeywordsString("checklist, \n, ", ", ");

    assertEquals(List.of("checklist"), set.getKeywords());
  }
}