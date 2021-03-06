package org.gbif.metadata;

import org.gbif.utils.file.FileUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class MetadataFactoryTest {

  @Test
  public void testMetadataStreams() throws MetadataException {
    MetadataFactory mf = new MetadataFactory();
    BasicMetadataImpl bm;

    // quick eml test
    bm = mf.read(FileUtils.getClasspathFile("metadata/eml.xml"));
    assertNotNull(bm);

    // utf16 little endian encoded file with BOM
    bm = mf.read(FileUtils.getClasspathFile("metadata/eml-utf16LE.xml"));
    assertEquals("76", bm.getSourceId());

    // utf16 little endian encoded file with BOM, but the XML doesnt mention Little Endian
    bm = mf.read(FileUtils.getClasspathFile("metadata/eml-utf16_no_LE_declared.xml"));
    assertEquals("76", bm.getSourceId());

    // we cant parse this one. It is a utf16 encoded file but the xml declaration says its utf8
    try {
      bm = mf.read(FileUtils.getClasspathFile("metadata/eml-utf16_declared_as_utf8.xml"));
      fail("This is a utf16 xml file which we should not be able to handle as things stand");
    } catch (MetadataException e) {
    }

    // test proper archive
    bm = mf.read(FileUtils.getClasspathFile("archive-dwc/eml.xml"));
    assertEquals("Hershkovitz, Catalogue of Living Whales", bm.getTitle());
    assertEquals("IPT; GBIF", bm.getSubject());
    assertEquals("567432", bm.getSourceId());

    bm = mf.read(FileUtils.getClasspathFile("metadata/clb_eml.xml"));
    assertEquals("ITIS", bm.getTitle());
    assertEquals("2010-02-24", DateUtils.ISO_DATE_FORMAT.format(bm.getPublished()));
    assertNull(bm.getAdditionalMetadata("recordLinkUrl"));
    assertEquals(0, bm.getAdditionalMetadata().size());
    assertNull(bm.getSourceId());

    bm = mf.read(FileUtils.getClasspathFile("metadata/ipt_eml.xml"));
    assertEquals("PonTaurus", bm.getTitle());
    assertEquals("Test data set mapped to Darwin Core.", bm.getDescription().get(0));
    assertEquals("IPT; GBIF", bm.getSubject());
    assertEquals("http://www.bgbm.org", bm.getHomepageUrl());

    // test as input stream
    bm = mf.read(getClass().getResourceAsStream("/metadata/worms_dc2.xml"));
    assertEquals("World Register of Marine Species", bm.getTitle());

    // test as file
    bm = mf.read(FileUtils.getClasspathFile("metadata/worms_dc.xml"));
    assertEquals("World Register of Marine Species", bm.getTitle());
    assertEquals(7, bm.getDescription().size());
    assertTrue(bm.getDescription().get(0).startsWith("The aim of a World Register"));
    assertEquals("Marine;Taxonomy;Species Checklist;Authoritative;Specimens", bm.getSubject());
    assertEquals("2010-08-09", DateUtils.ISO_DATE_FORMAT.format(bm.getPublished()));

    bm = mf.read(FileUtils.getClasspathFile("metadata/worms_eml2.1.xml"));
    assertEquals("World Register of Marine Species", bm.getTitle());
    assertEquals(1, bm.getAdditionalMetadata().size());
    assertEquals("http://www.marinespecies.org/aphia.php?p=taxdetails&id=146230",
      bm.getAdditionalMetadata("recordLinkUrl"));
  }

}
