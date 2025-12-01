<#escape x as x?xml>
    <#macro xmlSchemaDateTime dt><#assign dt2=dt?datetime?string("yyyy-MM-dd'T'HH:mm:ss.SSSZ")/>${dt2?substring(0, dt2?length-2)}:${dt2?substring(dt2?length-2, dt2?length)}</#macro>
    <#assign DATEIsoFormat="yyyy-MM-dd"/>
<eml:eml xmlns:eml="https://eml.ecoinformatics.org/eml-2.2.0"
         xmlns:dc="http://purl.org/dc/terms/"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://eml.ecoinformatics.org/eml-2.2.0 https://rs.gbif.org/schema/eml-gbif-profile/1.3/eml.xsd"
         packageId="${eml.packageId}" system="http://gbif.org" scope="system"<#if (eml.metadataLanguage)??>
         xml:lang="${eml.metadataLanguage!}"</#if>>
    <dataset>
        <#list eml.getAlternateIdentifiers() as altid>
        <alternateIdentifier>${altid!}</alternateIdentifier>
        </#list>
        <#if eml.shortName?has_content>
        <shortName>${eml.shortName}</shortName>
        </#if>
        <#if eml.title?has_content>
        <title xml:lang="${eml.metadataLanguage!"en"}"><#if eml.title?has_content>${eml.title}<#else><@s.text name='eml.title'/></#if></title>
        </#if>
        <#-- The creator is the person, organization, or position who created the resource (not necessarily the author of this metadata about the resource). -->
        <#if (eml.creators?size>0) >
        <#list eml.creators as creator>
        <creator>
            <#if (creator.getLastName())?? || ((!(creator.getOrganisation())??) && (!(creator.getPosition())??))>
            <individualName>
                <#if (creator.salutation)?has_content>
                <salutation>${creator.salutation}</salutation>
                </#if>
                <#if (creator.getFirstName())??>
                <givenName>${creator.firstName}</givenName>
                </#if>
                <surName>${creator.lastName!}</surName>
            </individualName>
            </#if>
            <#if (creator.getOrganisation())??>
            <organizationName>${creator.organisation}</organizationName>
            </#if>
            <#if (creator.getPosition())?has_content>
            <#list creator.position as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            </#if>
            <#assign adr=creator.getAddress()/>
            <#if (adr.getAddress())?has_content
            || (adr.getCity())??
            || (adr.getProvince())??
            || (adr.getPostalCode())??
            || (adr.getCountry())?? >
            <address>
                <#list adr.getAddress() as ad>
                <#if ad?has_content >
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if (adr.getCity())?? >
                <city>${adr.city}</city>
                </#if>
                <#if (adr.getProvince())?? >
                <administrativeArea>${adr.province}</administrativeArea>
                </#if>
                <#if (adr.getPostalCode())?? >
                <postalCode>${adr.postalCode}</postalCode>
                </#if>
                <#if (adr.getCountry())?? >
                <country>${adr.country}</country>
                </#if>
            </address>
            </#if>
            <#list creator.getPhone() as phone>
            <#if phone??>
            <phone>${phone}</phone>
            </#if>
            </#list>
            <#list creator.getEmail() as email>
            <#if email??>
            <electronicMailAddress>${email}</electronicMailAddress>
            </#if>
            </#list>
            <#list creator.getHomepage() as homepage>
            <#if homepage??>
            <onlineUrl>${homepage}</onlineUrl>
            </#if>
            </#list>
            <#if (creator.userIds?size>0)>
            <#list creator.userIds as userId>
            <#if userId.identifier?has_content && userId.directory?has_content>
            <userId directory="${userId.directory}">${userId.identifier}</userId>
            </#if>
            </#list>
            </#if>
        </creator>
        </#list>
        </#if>
        <#-- The metadataProvider created documentation for the resource. -->
        <#if (eml.metadataProviders?size>0)>
        <#list eml.metadataProviders as metadataProvider>
        <metadataProvider>
            <#if (metadataProvider.getLastName())?? || ((!(metadataProvider.getOrganisation())??) && (!(metadataProvider.getPosition())??))>
            <individualName>
                <#if (metadataProvider.salutation)?has_content>
                <salutation>${metadataProvider.salutation}</salutation>
                </#if>
                <#if (metadataProvider.getFirstName())??>
                <givenName>${metadataProvider.firstName}</givenName>
                </#if>
                <surName>${metadataProvider.lastName!}</surName>
            </individualName>
            </#if>
            <#if (metadataProvider.getOrganisation())??>
            <organizationName>${metadataProvider.organisation}</organizationName>
            </#if>
            <#if (metadataProvider.getPosition())?has_content>
            <#list metadataProvider.position as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            </#if>
            <#assign adr=metadataProvider.getAddress()/>
            <#if (adr.getAddress())?has_content
            || (adr.getCity())??
            || (adr.getProvince())??
            || (adr.getPostalCode())??
            || (adr.getCountry())?? >
            <address>
                <#list adr.getAddress() as ad>
                <#if ad?has_content >
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if (adr.getCity())?? >
                <city>${adr.city}</city>
                </#if>
                <#if (adr.getProvince())?? >
                <administrativeArea>${adr.province}</administrativeArea>
                </#if>
                <#if (adr.getPostalCode())?? >
                <postalCode>${adr.postalCode}</postalCode>
                </#if>
                <#if (adr.getCountry())?? >
                <country>${adr.country}</country>
                </#if>
            </address>
            </#if>
            <#list metadataProvider.getPhone() as phone>
            <#if phone??>
            <phone>${phone}</phone>
            </#if>
            </#list>
            <#list metadataProvider.getEmail() as email>
            <#if email??>
            <electronicMailAddress>${email}</electronicMailAddress>
            </#if>
            </#list>
            <#list metadataProvider.getHomepage() as homepage>
            <#if homepage??>
            <onlineUrl>${homepage}</onlineUrl>
            </#if>
            </#list>
            <#if (metadataProvider.userIds?size>0)>
            <#list metadataProvider.userIds as userId>
            <#if userId.identifier?has_content && userId.directory?has_content>
            <userId directory="${userId.directory}">${userId.identifier}</userId>
            </#if>
            </#list>
            </#if>
        </metadataProvider>
        </#list>
        </#if>
        <#-- Any other party associated with the resource, along with their role. -->
        <#if (eml.associatedParties?size > 0)>
        <#list eml.associatedParties as associatedParty>
        <associatedParty>
            <#if (associatedParty.getLastName())?? || ((!(associatedParty.getOrganisation())??) && (!(associatedParty.getPosition())??))>
            <individualName>
                <#if (associatedParty.salutation)?has_content>
                <salutation>${associatedParty.salutation}</salutation>
                </#if>
                <#if (associatedParty.getFirstName())??>
                <givenName>${associatedParty.firstName}</givenName>
                </#if>
                <surName>${associatedParty.lastName!}</surName>
            </individualName>
            </#if>
            <#if (associatedParty.getOrganisation())??>
            <organizationName>${associatedParty.organisation}</organizationName>
            </#if>
            <#if (associatedParty.getPosition())?has_content>
            <#list associatedParty.position as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            </#if>
            <#assign adr=associatedParty.getAddress()/>
            <#if (adr.getAddress())?has_content
            || (adr.getCity())??
            || (adr.getProvince())??
            || (adr.getPostalCode())??
            || (adr.getCountry())?? >
            <address>
                <#list adr.getAddress() as ad>
                <#if ad?has_content >
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if (adr.getCity())?? >
                <city>${adr.city}</city>
                </#if>
                <#if (adr.getProvince())?? >
                <administrativeArea>${adr.province}</administrativeArea>
                </#if>
                <#if (adr.getPostalCode())?? >
                <postalCode>${adr.postalCode}</postalCode>
                </#if>
                <#if (adr.getCountry())?? >
                <country>${adr.country}</country>
                </#if>
            </address>
            </#if>
            <#list associatedParty.getPhone() as phone>
            <#if phone??>
            <phone>${phone}</phone>
            </#if>
            </#list>
            <#list associatedParty.getEmail() as email>
            <#if email??>
            <electronicMailAddress>${email}</electronicMailAddress>
            </#if>
            </#list>
            <#list associatedParty.getHomepage() as homepage>
            <#if homepage??>
            <onlineUrl>${homepage}</onlineUrl>
            </#if>
            </#list>
            <#if (associatedParty.userIds?size>0)>
            <#list associatedParty.userIds as userId>
            <#if userId.identifier?has_content && userId.directory?has_content>
            <userId directory="${userId.directory}">${userId.identifier}</userId>
            </#if>
            </#list>
            </#if>
            <#if (associatedParty.getRole())??>
            <role>${associatedParty.role!}</role>
            </#if>
        </associatedParty>
        </#list>
        </#if>
        <#-- The date on which the resource was published. -->
        <pubDate>
        <#if (eml.getPubDate()??)>
            <#if (eml.getPubDate()?string("SSS"))=="001">${eml.pubDate?date?string("yyyy")}<#else>${eml.pubDate?date?string(DATEIsoFormat)}</#if>
        </#if>
        </pubDate>
        <language>${eml.language!"en"}</language>
        <#-- A description of the resource -->
        <#if eml.abstract?has_content>
        <abstract>
            <#noescape>
            ${eml.getDocBookField("description")}
            </#noescape>
        </abstract>
        </#if>
        <#-- Zero or more sets of keywords and an associated thesaurus for each. -->
        <#if (eml.keywords ? size > 0)>
        <#list eml.keywords as ks>
        <#if (ks.keywordThesaurus)?has_content>
        <keywordSet>
            <#if (ks.keywords ? size > 0)>
            <#list ks.keywords as k>
            <keyword>${k!""}</keyword>
            </#list>
            </#if>
            <keywordThesaurus>${ks.keywordThesaurus!}</keywordThesaurus>
        </keywordSet>
        </#if>
        </#list>
        </#if>
        <#-- Any additional information about the resource not covered in any other element. -->
        <#if (eml.getAdditionalInfo())??>
        <additionalInfo>
            <para>${eml.additionalInfo!}</para>
        </additionalInfo>
        </#if>
        <#-- A statement of the intellectual property rights associated with the resource. -->
        <#if (eml.getIntellectualRightsXml())??>
        <intellectualRights>
            <para><#noescape>${eml.getIntellectualRightsXml()!}</#noescape></para>
        </intellectualRights>
        <#if eml.intellectualRights??>
        <#if eml.intellectualRights?contains("CC0 1.0")>
        <licensed>
            <licenseName>Creative Commons Zero v1.0 Universal</licenseName>
            <url>https://spdx.org/licenses/CC0-1.0.html</url>
            <identifier>CC0-1.0</identifier>
        </licensed>
        <#elseif eml.intellectualRights?contains("CC-BY 4.0")>
        <licensed>
            <licenseName>Creative Commons Attribution 4.0 International</licenseName>
            <url>https://spdx.org/licenses/CC-BY-4.0.html</url>
            <identifier>CC-BY-4.0</identifier>
        </licensed>
        <#elseif eml.intellectualRights?contains("CC-BY-NC 4.0")>
        <licensed>
            <licenseName>Creative Commons Attribution Non Commercial 4.0 International</licenseName>
            <url>https://spdx.org/licenses/CC-BY-NC-4.0.html</url>
            <identifier>CC-BY-NC-4.0</identifier>
        </licensed>
        </#if>
        </#if>
        </#if>
        <#if (eml.getDistributionUrl())??>
        <distribution scope="document">
            <online>
                <url function="information">${eml.distributionUrl!}</url>
            </online>
        </distribution>
        </#if>
        <#if (eml.getDistributionDownloadUrl())??>
        <distribution scope="document">
            <online>
                <url function="download">${eml.distributionDownloadUrl!}</url>
            </online>
        </distribution>
        </#if>
        <#if ((eml.geospatialCoverages ? size > 0)
        ||  (eml.taxonomicCoverages ? size > 0)
        ||  (eml.temporalCoverages ? size > 0))>
        <coverage>
            <#list eml.getGeospatialCoverages() as geocoverage>
            <geographicCoverage>
                <#if (geocoverage.getDescription())?has_content>
                <geographicDescription>${geocoverage.description}</geographicDescription>
                <#else>
                <geographicDescription>N/A</geographicDescription>
                </#if>
                <boundingCoordinates>
                    <westBoundingCoordinate>${geocoverage.boundingCoordinates.min.longitude!?string('0.######')?replace(',', '.')}</westBoundingCoordinate>
                    <eastBoundingCoordinate>${geocoverage.boundingCoordinates.max.longitude!?string('0.######')?replace(',', '.')}</eastBoundingCoordinate>
                    <northBoundingCoordinate>${geocoverage.boundingCoordinates.max.latitude!?string('0.######')?replace(',', '.')}</northBoundingCoordinate>
                    <southBoundingCoordinate>${geocoverage.boundingCoordinates.min.latitude!?string('0.######')?replace(',', '.')}</southBoundingCoordinate>
                </boundingCoordinates>
            </geographicCoverage>
            </#list>
            <#if (eml.temporalCoverages ? size > 0)>
            <#list eml.temporalCoverages as tempcoverage>
            <#if (tempcoverage.startDate)??>
            <temporalCoverage>
                <#if (tempcoverage.endDate)??>
                <rangeOfDates>
                    <#if (tempcoverage.startDate)??>
                    <beginDate>
                        <calendarDate><#if (tempcoverage.startDate?string("SSS"))=="001">${tempcoverage.startDate?string("yyyy")}<#else>${tempcoverage.startDate?string(DATEIsoFormat)}</#if></calendarDate>
                    </beginDate>
                    </#if>
                    <endDate>
                        <calendarDate><#if (tempcoverage.endDate?string("SSS"))=="001">${tempcoverage.endDate?string("yyyy")}<#else>${tempcoverage.endDate?string(DATEIsoFormat)}</#if></calendarDate>
                    </endDate>
                </rangeOfDates>
                <#else>
                    <#if (tempcoverage.startDate)??>
                    <singleDateTime>
                        <calendarDate><#if (tempcoverage.startDate?string("SSS"))=="001">${tempcoverage.startDate?string("yyyy")}<#else>${tempcoverage.startDate?string(DATEIsoFormat)}</#if></calendarDate>
                    </singleDateTime>
                    </#if>
                </#if>
            </temporalCoverage>
            </#if>
            </#list>
            </#if>
            <#if (eml.taxonomicCoverages ? size > 0)>
            <#list eml.getTaxonomicCoverages() as taxoncoverage>
            <#if (taxoncoverage.taxonKeywords ? size > 0)>
            <taxonomicCoverage>
                <#if (taxoncoverage.getDescription())?has_content>
                <generalTaxonomicCoverage>${taxoncoverage.description!}</generalTaxonomicCoverage>
                </#if>
                <#list taxoncoverage.taxonKeywords as tk>
                <taxonomicClassification>
                    <#if tk.rank?has_content>
                    <taxonRankName>${tk.rank}</taxonRankName>
                    </#if>
                    <taxonRankValue>${tk.scientificName!}</taxonRankValue>
                    <#if tk.commonName?has_content>
                    <commonName>${tk.commonName}</commonName>
                    </#if>
                </taxonomicClassification>
                </#list>
            </taxonomicCoverage>
            </#if>
            </#list>
            </#if>
        </coverage>
        </#if>
        <#if eml.purpose?has_content>
        <#noescape>
        <purpose>${eml.getDocBookField("purpose")}</purpose>
        </#noescape>
        </#if>
        <#if eml.introduction?has_content>
        <#noescape>
        <introduction>${eml.getDocBookField("introduction")}</introduction>
        </#noescape>
        </#if>
        <#if eml.gettingStarted?has_content>
        <#noescape>
        <gettingStarted>${eml.getDocBookField("gettingStarted")}</gettingStarted>
        </#noescape>
        </#if>
        <#if eml.acknowledgements?has_content>
        <#noescape>
        <acknowledgements>${eml.getDocBookField("acknowledgements")}</acknowledgements>
        </#noescape>
        </#if>
        <#if eml.updateFrequency??>
        <maintenance>
            <description>
                <para>${eml.updateFrequencyDescription!}</para>
            </description>
            <maintenanceUpdateFrequency>${eml.updateFrequency.identifier}</maintenanceUpdateFrequency>
        </maintenance>
        </#if>
        <#-- The contact is the person or institution to contact with questions about the use, interpretation of a data set. -->
        <#if (eml.contacts?size>0)>
        <#list eml.contacts as contact>
        <contact>
            <#if (contact.getLastName())?? || ((!(contact.getOrganisation())??) && (!(contact.getPosition())??))>
            <individualName>
                <#if (contact.salutation)?has_content>
                <salutation>${contact.salutation}</salutation>
                </#if>
                <#if (contact.getFirstName())??>
                <givenName>${contact.firstName}</givenName>
                </#if>
                <surName>${contact.lastName!}</surName>
            </individualName>
            </#if>
            <#if (contact.getOrganisation())??>
            <organizationName>${contact.organisation}</organizationName>
            </#if>
            <#if (contact.getPosition())?has_content>
            <#list contact.position as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            </#if>
            <#assign adr=contact.getAddress()/>
            <#if (adr.getAddress())?has_content
            || (adr.getCity())??
            || (adr.getProvince())??
            || (adr.getPostalCode())??
            || (adr.getCountry())?? >
            <address>
                <#list adr.getAddress() as ad>
                <#if ad?has_content >
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if (adr.getCity())?? >
                <city>${adr.city}</city>
                </#if>
                <#if (adr.getProvince())?? >
                <administrativeArea>${adr.province}</administrativeArea>
                </#if>
                <#if (adr.getPostalCode())?? >
                <postalCode>${adr.postalCode}</postalCode>
                </#if>
                <#if (adr.getCountry())?? >
                <country>${adr.country}</country>
                </#if>
            </address>
            </#if>
            <#list contact.getPhone() as phone>
            <#if phone??>
            <phone>${phone}</phone>
            </#if>
            </#list>
            <#list contact.getEmail() as email>
            <#if email??>
            <electronicMailAddress>${email}</electronicMailAddress>
            </#if>
            </#list>
            <#list contact.getHomepage() as homepage>
            <#if homepage??>
            <onlineUrl>${homepage}</onlineUrl>
            </#if>
            </#list>
            <#if (contact.userIds?size>0)>
            <#list contact.userIds as userId>
            <#if userId.identifier?has_content && userId.directory?has_content>
            <userId directory="${userId.directory}">${userId.identifier}</userId>
            </#if>
            </#list>
            </#if>
        </contact>
        </#list>
        </#if>
        <#if eml.publisherId?? && eml.publisherOrganizationName??>
        <publisher id="${eml.publisherId}" scope="system" system="http://gbif.org">
            <organizationName>${eml.publisherOrganizationName}</organizationName>
        </publisher>
        </#if>
        <#if (eml.getMethodSteps())?has_content>
        <methods>
            <#list eml.getMethodSteps() as methodStep>
            <methodStep>
                <description>
                    <para>${methodStep!}</para>
                </description>
            </methodStep>
            </#list>
            <#if (eml.getStudyExtent())?has_content && (eml.getSampleDescription())?has_content >
            <sampling>
                <#if (eml.getStudyExtent())?has_content>
                <studyExtent>
                    <description>
                        <para>${eml.studyExtent}</para>
                    </description>
                </studyExtent>
                </#if>
                <#if (eml.getSampleDescription())?has_content>
                <samplingDescription>
                    <para>${eml.sampleDescription}</para>
                </samplingDescription>
                </#if>
            </sampling>
            </#if>
            <#if (eml.getQualityControl())?has_content>
            <qualityControl>
                <description>
                    <para>${eml.qualityControl}</para>
                </description>
            </qualityControl>
            </#if>
        </methods>
        </#if>
        <#if eml.project.title?has_content>
        <#if eml.project.identifier?has_content><project id="${eml.project.identifier}"><#else><project></#if>
            <title>${eml.project.title}</title>
            <#list (eml.project.getPersonnel())! as personnel>
            <personnel>
                <individualName>
                    <#if (personnel.salutation)?has_content>
                    <salutation>${personnel.salutation}</salutation>
                    </#if>
                    <#if (personnel.getFirstName())??>
                    <givenName>${personnel.firstName}</givenName>
                    </#if>
                    <surName>${personnel.lastName!}</surName>
                </individualName>
                <#if (personnel.userIds?size>0)>
                <#list personnel.userIds as userId>
                <#if userId.identifier?has_content && userId.directory?has_content>
                <userId directory="${userId.directory}">${userId.identifier}</userId>
                </#if>
                </#list>
                </#if>
                <role>${personnel.role!}</role>
            </personnel>
            </#list>
            <#if eml.project.description?has_content>
            <abstract>
                <para>${eml.project.description}</para>
            </abstract>
            </#if>
            <#if eml.project.funding?has_content>
            <funding>
                <para>${eml.project.funding}</para>
            </funding>
            </#if>
            <#list eml.project.awards! as award>
            <award>
                <funderName>${award.funderName}</funderName>
                <#list award.funderIdentifiers! as fi>
                <#if fi?has_content><funderIdentifier>${fi}</funderIdentifier></#if>
                </#list>
                <#if award.awardNumber?has_content>
                <awardNumber>${award.awardNumber}</awardNumber>
                </#if>
                <title>${award.title}</title>
                <#if award.awardUrl?has_content>
                <awardUrl>${award.awardUrl}</awardUrl>
                </#if>
            </award>
            </#list>
            <#if eml.project.studyAreaDescription?? && eml.project.studyAreaDescription.descriptorValue?has_content>
            <studyAreaDescription>
                <descriptor name="${eml.project.studyAreaDescription.getName().getName()!}"
                            citableClassificationSystem="${eml.project.studyAreaDescription.citableClassificationSystem!}">
                    <descriptorValue>${eml.project.studyAreaDescription.descriptorValue}</descriptorValue>
                </descriptor>
            </studyAreaDescription>
            </#if>
            <#if eml.project.designDescription?has_content>
            <designDescription>
                <description>
                    <para>${eml.project.designDescription}</para>
                </description>
            </designDescription>
            </#if>
            <#list (eml.project.relatedProjects)! as relatedProject>
            <#if relatedProject.identifier?has_content><relatedProject id="${relatedProject.identifier}"><#else><relatedProject></#if>
                <title>${relatedProject.title}</title>
                <#list (relatedProject.personnel)! as relatedProjectPersonnel>
                <personnel>
                    <individualName>
                        <#if (relatedProjectPersonnel.salutation)?has_content>
                        <salutation>${relatedProjectPersonnel.salutation}</salutation>
                        </#if>
                        <#if (relatedProjectPersonnel.getFirstName())??>
                        <givenName>${relatedProjectPersonnel.firstName}</givenName>
                        </#if>
                        <surName>${relatedProjectPersonnel.lastName!}</surName>
                    </individualName>
                    <#if (relatedProjectPersonnel.userIds?size>0)>
                    <#list relatedProjectPersonnel.userIds as userId>
                    <#if userId.identifier?has_content && userId.directory?has_content>
                    <userId directory="${userId.directory}">${userId.identifier}</userId>
                    </#if>
                    </#list>
                    </#if>
                    <role>${relatedProjectPersonnel.role!}</role>
                </personnel>
                </#list>
            </relatedProject>
            </#list>
        </project>
        </#if>
    </dataset>
    <#if ((eml.citation)??) ||
    (eml.bibliographicCitations ? size > 0) ||
    (eml.metadataLanguage)?? ||
    (eml.hierarchyLevel)?? ||
    (eml.PhysicalData ? size > 0) ||
    ((eml.jgtiCuratorialUnit)??) ||
    (eml.specimenPreservationMethod)?? ||
    (eml.temporalCoverages ? size > 0) ||
    (eml.parentCollectionId)?? ||
    (eml.collectionId)?? ||
    (eml.collectionName)?? ||
    (eml.logoUrl)?? ||
    (eml.getEmlVersion()>1)>
    <additionalMetadata>
        <metadata>
            <gbif>
                <#if (eml.dateStamp)??>
                <dateStamp><@xmlSchemaDateTime eml.dateStamp/></dateStamp>
                </#if>
                <#if (eml.hierarchyLevel)??>
                <hierarchyLevel>${eml.hierarchyLevel!}</hierarchyLevel>
                </#if>
                <#if (eml.citation.citation)?has_content>
                <#-- How to cite the resource. -->
                <#if (eml.citation.identifier)?has_content>
                <citation identifier="${eml.citation.identifier!}">${eml.citation.citation!}</citation>
                <#else>
                <citation>${eml.citation.citation!}</citation>
                </#if>
                </#if>
                <#if (eml.bibliographicCitations ? size > 0)>
                <#-- Citations about the resource. -->
                <bibliography>
                    <#list eml.getBibliographicCitations() as bcit>
                    <#if (bcit.identifier)?has_content>
                    <citation identifier="${bcit.identifier!}">${bcit.citation!}</citation>
                    <#else>
                    <citation>${bcit.citation!}</citation>
                    </#if>
                    </#list>
                </bibliography>
                </#if>
                <#if (eml.physicalData ? size > 0)>
                <#list eml.getPhysicalData() as pdata>
                <#if pdata.name?has_content && pdata.format?has_content && pdata.distributionUrl?has_content>
                <physical>
                    <objectName>${pdata.name}</objectName>
                    <#if pdata.charset?has_content>
                    <characterEncoding>${pdata.charset}</characterEncoding>
                    </#if>
                    <dataFormat>
                        <externallyDefinedFormat>
                            <formatName>${pdata.format}</formatName>
                            <#if pdata.formatVersion?has_content>
                            <formatVersion>${pdata.formatVersion}</formatVersion>
                            </#if>
                        </externallyDefinedFormat>
                    </dataFormat>
                    <distribution>
                        <online>
                            <url function="download">${pdata.distributionUrl}</url>
                        </online>
                    </distribution>
                </physical>
                </#if>
                </#list>
                </#if>
                <#if (eml.getLogoUrl())??>
                <resourceLogoUrl>${eml.logoUrl!}</resourceLogoUrl>
                </#if>
                <#if (eml.collections?? && eml.collections?size>0) >
                <#list eml.collections as collection>
                <#if collection.collectionName?has_content>
                <collection>
                    <#if collection.parentCollectionId?has_content>
                    <parentCollectionIdentifier>${collection.parentCollectionId}</parentCollectionIdentifier>
                    </#if>
                    <#if collection.collectionId?has_content>
                    <collectionIdentifier>${collection.collectionId}</collectionIdentifier>
                    </#if>
                    <collectionName>${collection.collectionName}</collectionName>
                </collection>
                </#if>
                </#list>
                </#if>
                <#list eml.getTemporalCoverages() as tcoverage>
                <#if (tcoverage.getFormationPeriod())??>
                <formationPeriod>${tcoverage.formationPeriod}</formationPeriod>
                </#if>
                </#list>
                <#if (eml.specimenPreservationMethods?size >0)>
                <#list eml.specimenPreservationMethods as preservationMethod>
                <#if preservationMethod?has_content>
                <specimenPreservationMethod>${preservationMethod}</specimenPreservationMethod>
                </#if>
                </#list>
                </#if>
                <#if (eml.temporalCoverages ? size > 0)>
                <#list eml.getTemporalCoverages() as tcoverage>
                <#if (tcoverage.getLivingTimePeriod())??>
                <livingTimePeriod>${tcoverage.livingTimePeriod}</livingTimePeriod>
                </#if>
                </#list>
                </#if>
                <#if (eml.jgtiCuratorialUnits ? size > 0)>
                <#list eml.getJgtiCuratorialUnits() as cdata>
                <jgtiCuratorialUnit>
                    <#if (cdata.getUnitType())?has_content>
                    <jgtiUnitType>${cdata.unitType}</jgtiUnitType>
                    </#if>
                    <#if (cdata.rangeEnd)??>
                    <jgtiUnitRange>
                        <beginRange><#if (cdata.rangeStart)??>${cdata.rangeStart?string("####0")}</#if></beginRange>
                        <endRange><#if (cdata.rangeEnd)??>${cdata.rangeEnd?string("####0")}</#if></endRange>
                    </jgtiUnitRange>
                    <#else>
                    <jgtiUnits
                            uncertaintyMeasure="<#if (cdata.uncertaintyMeasure)??>${cdata.uncertaintyMeasure?string("####0")}</#if>"><#if (cdata.rangeMean)??>${cdata.rangeMean?string("####0")}</#if></jgtiUnits>
                    </#if>
                </jgtiCuratorialUnit>
                </#list>
                </#if>
                <#if (eml.getPreviousEmlVersion()?string != "1" && eml.getPreviousEmlVersion()?string != "1.0")>
                <dc:replaces>${eml.guid}/v${eml.getPreviousEmlVersion()}.xml</dc:replaces>
                </#if>
            </gbif>
        </metadata>
    </additionalMetadata>
    </#if>
</eml:eml>
</#escape>
