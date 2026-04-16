<#-- @ftlvariable name="dataset" type="org.gbif.api.model.registry.Dataset" -->
<#-- @ftlvariable name="eml" type="org.gbif.metadata.eml.EMLWriter.EmlDatasetWrapper" -->
<#escape x as x?xml>
    <#macro interpretedEnum enum><#if enum??><#if enum.interpreted?has_content>${enum.interpreted.name()?lower_case?replace("_", " ")?capitalize}<#else>${enum.verbatim!}</#if></#if></#macro>
    <#macro citation cit>
        <#if cit??>
            <#if cit.identifier?has_content>
                <citation identifier="${cit.identifier}">${cit.text!cit.identifier}</citation>
            <#else>
                <citation>${cit.text!}</citation>
            </#if>
        </#if>
    </#macro>
    <#assign DATEIsoFormat="yyyy-MM-dd"/>
    <#macro xmlSchemaDateTime dt><#assign dt2=dt?datetime?string("yyyy-MM-dd'T'hh:mm:ss.SSSZ")/>${dt2?substring(0, dt2?length-2)}:${dt2?substring(dt2?length-2, dt2?length)}</#macro>
    <#macro isodate dt>
        <#if dt?has_content>
            <#if dt?string("SSS")=="001">
                ${dt?string("yyyy")}
            <#else>
                ${dt?string(DATEIsoFormat)}
            </#if>
        </#if>
    </#macro>

<#-- Convert GBIF ENUM MaintenanceUpdateFrequency value into EML MaintenanceUpdateFrequency value -->
    <#function maintenanceUpdateFrequencyToEmlValue enumVal>
        <#assign emlValue =""/>
        <#switch enumVal>
            <#case "AS_NEEDED">
                <#assign emlValue ="asNeeded"/>
                <#break>
            <#case "NOT_PLANNED">
                <#assign emlValue ="notPlanned"/>
                <#break>
            <#case "OTHER_MAINTENANCE_PERIOD">
                <#assign emlValue ="otherMaintenancePeriod"/>
                <#break>
            <#default>
                <#assign emlValue ="${enumVal?lower_case}"/>
        </#switch>
        <#return emlValue?trim>
    </#function>

<#-- Parse user ID into two parts (directory and id) and create EML.userID element -->
    <#macro constructUserID userID>
    <#-- List of supported user ID directories, including trailing slash to facilitate parsing identifier into parts -->
        <#assign orcidDirectory = "orcid.org/"/>
        <#assign researcherIdDirectory = "researcherid.com/rid/"/>
        <#assign googleScholarIdDirectory = "scholar.google.com/citations?user="/>
        <#assign linkedinIdDirectory = "linkedin.com/profile/view?id="/>
        <#assign linkedinIdDirectory2 = "linkedin.com/in/"/>

        <#assign directory = ""/>
        <#assign id = ""/>

    <#-- ORCID -->
        <#if userID?lower_case?contains(orcidDirectory)>
            <#assign id = userID?keep_after(orcidDirectory)/>
            <#assign directory = userID?keep_before(id)/>
        <#-- ResearcherID -->
        <#elseif userID?lower_case?contains(researcherIdDirectory)>
            <#assign id = userID?keep_after(researcherIdDirectory)/>
            <#assign directory = userID?keep_before(id)/>
        <#-- Google Scholar ID -->
        <#elseif userID?lower_case?contains(googleScholarIdDirectory)>
            <#assign id = userID?keep_after(googleScholarIdDirectory)/>
            <#assign directory = userID?keep_before(id)/>
        <#-- Linkedin ID -->
        <#elseif userID?lower_case?contains(linkedinIdDirectory)>
            <#assign id = userID?keep_after(linkedinIdDirectory)/>
            <#assign directory = userID?keep_before(id)/>
        <#-- Linkedin ID (2) -->
        <#elseif userID?lower_case?contains(linkedinIdDirectory2)>
            <#assign id = userID?keep_after(linkedinIdDirectory2)/>
            <#assign directory = userID?keep_before(id)/>
        </#if>
    <#-- Construct userID, with identifier slit into two parts: directory and id  -->
        <#if directory?has_content && id?has_content?has_content>
            <userId directory="${directory}">${id}</userId>
        </#if>
    </#macro>
<#if !omitXmlDeclaration>
<?xml version="1.0" encoding="utf-8"?>
</#if>
<eml:eml xmlns:eml="https://eml.ecoinformatics.org/eml-2.2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://eml.ecoinformatics.org/eml-2.2.0 https://rs.gbif.org/schema/eml-gbif-profile/1.3/eml.xsd"
packageId=<#if useDoiAsIdentifier && dataset.doi?has_content>"${dataset.doi.doiName}"<#else>"${dataset.key!}"</#if>  system="http://gbif.org" scope="system"
<#if dataset.language??>xml:lang="${dataset.language.getIso2LetterCode()}"</#if>>
    <dataset>
        <#if !useDoiAsIdentifier && dataset.doi?has_content>
        <alternateIdentifier>${dataset.doi}</alternateIdentifier>
        </#if>
        <#list dataset.identifiers![] as altid>
        <#if altid.identifier?has_content>
        <alternateIdentifier>${altid.identifier}</alternateIdentifier>
        </#if>
        </#list>
        <#if dataset.shortName?has_content>
        <shortName>${dataset.shortName}</shortName>
        </#if>
        <title>${dataset.title!}</title>
        <#-- The creators are the persons who created the resource (not necessarily the author of this metadata about the resource). -->
        <#list eml.getCreators() as ct>
        <creator>
            <#if ct.computeCompleteName()?has_content>
            <individualName>
                <#if ct.salutation?has_content>
                <salutation>${ct.salutation}</salutation>
                </#if>
                <#if ct.lastName?has_content>
                <#if ct.firstName?has_content>
                <givenName>${ct.firstName}</givenName>
                </#if>
                <surName>${ct.lastName!}</surName>
                <#else>
                <#--surName is mandatory in EML so but all we have if no lastname is available-->
                <surName>${ct.computeCompleteName()}</surName>
                </#if>
            </individualName>
            </#if>
            <#if ct.organization?has_content>
            <organizationName>${ct.organization!}</organizationName>
            </#if>
            <#list ct.position![] as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            <#if ct.address?has_content || ct.city?has_content || ct.province?has_content || ct.postalCode?has_content || ct.country?has_content>
            <address>
                <#list ct.address![] as ad>
                <#if ad?has_content>
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if ct.city?has_content>
                <city>${ct.city!}</city>
                </#if>
                <#if ct.province?has_content>
                <administrativeArea>${ct.province!}</administrativeArea>
                </#if>
                <#if ct.postalCode?has_content>
                <postalCode>${ct.postalCode!}</postalCode>
                </#if>
                <#if ct.country?has_content>
                <country>${ct.country!}</country>
                </#if>
            </address>
            </#if>
            <#list ct.phone![] as p>
            <#if p?has_content>
            <phone>${p}</phone>
            </#if>
            </#list>
            <#list ct.email![] as e>
            <#if e?has_content>
            <electronicMailAddress>${e}</electronicMailAddress>
            </#if>
            </#list>
            <#list ct.homepage![] as h>
            <#if h?has_content>
            <onlineUrl>${h}</onlineUrl>
            </#if>
            </#list>
            <#if ct.userId?has_content>
            <#list ct.userId as uid>
            <@constructUserID uid/>
            </#list>
            </#if>
        </creator>
        </#list>
        <#-- The contacts responsible for the creation of the metadata. -->
        <#list eml.getMetadataProviders() as ct>
        <metadataProvider>
            <#if ct.computeCompleteName()?has_content>
            <individualName>
                <#if ct.salutation?has_content>
                <salutation>${ct.salutation}</salutation>
                </#if>
                <#if ct.lastName?has_content>
                <#if ct.firstName?has_content>
                <givenName>${ct.firstName}</givenName>
                </#if>
                <surName>${ct.lastName!}</surName>
                <#else>
                <#--surName is mandatory in EML so but all we have if no lastname is available-->
                <surName>${ct.computeCompleteName()}</surName>
                </#if>
            </individualName>
            </#if>
            <#if ct.organization?has_content>
            <organizationName>${ct.organization!}</organizationName>
            </#if>
            <#list ct.position![] as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            <#if ct.address?has_content || ct.city?has_content || ct.province?has_content || ct.postalCode?has_content || ct.country?has_content>
            <address>
                <#list ct.address![] as ad>
                <#if ad?has_content>
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if ct.city?has_content>
                <city>${ct.city!}</city>
                </#if>
                <#if ct.province?has_content>
                <administrativeArea>${ct.province!}</administrativeArea>
                </#if>
                <#if ct.postalCode?has_content>
                <postalCode>${ct.postalCode!}</postalCode>
                </#if>
                <#if ct.country?has_content>
                <country>${ct.country!}</country>
                </#if>
            </address>
            </#if>
            <#list ct.phone![] as p>
            <#if p?has_content>
            <phone>${p}</phone>
            </#if>
            </#list>
            <#list ct.email![] as e>
            <#if e?has_content>
            <electronicMailAddress>${e}</electronicMailAddress>
            </#if>
            </#list>
            <#list ct.homepage![] as h>
            <#if h?has_content>
            <onlineUrl>${h}</onlineUrl>
            </#if>
            </#list>
            <#if ct.userId?has_content>
            <#list ct.userId as uid>
            <@constructUserID uid/>
            </#list>
            </#if>
        </metadataProvider>
        </#list>
        <#-- The associated party is another contact associated with the resource with a defined role. -->
        <#list eml.getAssociatedParties() as ct>
        <associatedParty>
            <#if ct.computeCompleteName()?has_content>
            <individualName>
                <#if ct.salutation?has_content>
                <salutation>${ct.salutation}</salutation>
                </#if>
                <#if ct.lastName?has_content>
                <#if ct.firstName?has_content>
                <givenName>${ct.firstName}</givenName>
                </#if>
                <surName>${ct.lastName!}</surName>
                <#else>
                <#--surName is mandatory in EML so but all we have if no lastname is available-->
                <surName>${ct.computeCompleteName()}</surName>
                </#if>
            </individualName>
            </#if>
            <#if ct.organization?has_content>
            <organizationName>${ct.organization!}</organizationName>
            </#if>
            <#list ct.position![] as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            <#if ct.address?has_content || ct.city?has_content || ct.province?has_content || ct.postalCode?has_content || ct.country?has_content>
            <address>
                <#list ct.address![] as ad>
                <#if ad?has_content>
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if ct.city?has_content>
                <city>${ct.city!}</city>
                </#if>
                <#if ct.province?has_content>
                <administrativeArea>${ct.province!}</administrativeArea>
                </#if>
                <#if ct.postalCode?has_content>
                <postalCode>${ct.postalCode!}</postalCode>
                </#if>
                <#if ct.country?has_content>
                <country>${ct.country!}</country>
                </#if>
            </address>
            </#if>
            <#list ct.phone![] as p>
            <#if p?has_content>
            <phone>${p}</phone>
            </#if>
            </#list>
            <#list ct.email![] as e>
            <#if e?has_content>
            <electronicMailAddress>${e}</electronicMailAddress>
            </#if>
            </#list>
            <#list ct.homepage![] as h>
            <#if h?has_content>
            <onlineUrl>${h}</onlineUrl>
            </#if>
            </#list>
            <#if ct.userId?has_content>
            <#list ct.userId as uid>
            <@constructUserID uid/>
            </#list>
            </#if>
            <#if ct.type?has_content>
            <role>${ct.type}</role>
            </#if>
        </associatedParty>
        </#list>
        <#-- The date on which the resource was published. -->
        <#if dataset.pubDate?has_content>
        <pubDate>
            <@isodate dataset.pubDate />
        </pubDate>
        </#if>
        <language>${dataset.dataLanguage!"en"}</language>
        <#-- A brief description of the resource. -->
        <#if eml.description?has_content>
        <abstract>
            <#noescape>
            ${eml.getDocBookField("description")}
            </#noescape>
        </abstract>
        </#if>
        <#-- Zero or more sets of keywords and an associated thesaurus for each. -->
        <#list dataset.keywordCollections![] as ks>
        <#if ks.keywords?has_content>
        <keywordSet>
            <#list ks.keywords![] as k>
            <keyword>${k!}</keyword>
            </#list>
            <#if ks.thesaurus?has_content>
            <keywordThesaurus>${ks.thesaurus}</keywordThesaurus>
            </#if>
        </keywordSet>
        </#if>
        </#list>
        <#-- Any additional information about the resource not covered in any other element. -->
        <#if dataset.additionalInfo?has_content>
        <additionalInfo>
            <para>${dataset.additionalInfo}</para>
        </additionalInfo>
        </#if>
        <#-- GBIF supported license assigned to the resource: CC0 1.0, CC-BY 4.0, CC-BY-NC 4.0 -->
        <#if dataset.license?has_content>
        <intellectualRights>
            <#if dataset.license.name() == "CC0_1_0">
            <para>To the extent possible under law, the publisher has waived all rights to these data and has dedicated them to the <ulink url="${dataset.license.getLicenseUrl()}"><citetitle>${dataset.license.getLicenseTitle()}</citetitle></ulink>. Users may copy, modify, distribute and use the work, including for commercial purposes, without restriction.</para>
            <#elseif dataset.license.name() == "CC_BY_4_0" || dataset.license.name() == "CC_BY_NC_4_0">
            <para>This work is licensed under a <ulink url="${dataset.license.getLicenseUrl()}"><citetitle>${dataset.license.getLicenseTitle()} License</citetitle></ulink>.</para>
            </#if>
        </intellectualRights>
        <licensed>
            <#if dataset.license.name() == "CC0_1_0">
            <licenseName>Creative Commons Zero v1.0 Universal</licenseName>
            <url>https://spdx.org/licenses/CC0-1.0.html</url>
            <identifier>CC0-1.0</identifier>
            <#elseif dataset.license.name() == "CC_BY_4_0">
            <licenseName>Creative Commons Attribution 4.0 International</licenseName>
            <url>https://spdx.org/licenses/CC-BY-4.0.html</url>
            <identifier>CC-BY-4.0</identifier>
            <#elseif dataset.license.name() == "CC_BY_NC_4_0">
            <licenseName>Creative Commons Attribution Non Commercial 4.0 International</licenseName>
            <url>https://spdx.org/licenses/CC-BY-NC-4.0.html</url>
            <identifier>CC-BY-NC-4.0</identifier>
            </#if>
        </licensed>
        </#if>
        <#if dataset.homepage?has_content>
        <distribution scope="document">
            <online>
                <url function="information">${dataset.homepage}</url>
            </online>
        </distribution>
        </#if>
        <#list dataset.endpoints as endpoint>
        <#if endpoint.type?? && endpoint.type == "DWC_ARCHIVE" && endpoint.url??>
        <distribution scope="document">
            <online>
                <url function="download">${endpoint.url}</url>
            </online>
        </distribution>
        </#if>
        </#list>
        <#if dataset.geographicCoverages?has_content || dataset.taxonomicCoverages?has_content || eml.singleDateAndDateRangeCoverages?has_content>
        <coverage>
            <#list dataset.geographicCoverages![] as geocoverage>
            <geographicCoverage>
                <#if geocoverage.description?has_content>
                <geographicDescription>${geocoverage.description}</geographicDescription>
                <#else>
                <geographicDescription>N/A</geographicDescription>
                </#if>
                <#if geocoverage.boundingBox?has_content>
                <boundingCoordinates>
                    <westBoundingCoordinate>${geocoverage.boundingBox.minLongitude!?string('0.######')?replace(',', '.')}</westBoundingCoordinate>
                    <eastBoundingCoordinate>${geocoverage.boundingBox.maxLongitude!?string('0.######')?replace(',', '.')}</eastBoundingCoordinate>
                    <northBoundingCoordinate>${geocoverage.boundingBox.maxLatitude!?string('0.######')?replace(',', '.')}</northBoundingCoordinate>
                    <southBoundingCoordinate>${geocoverage.boundingBox.minLatitude!?string('0.######')?replace(',', '.')}</southBoundingCoordinate>
                </boundingCoordinates>
                </#if>
            </geographicCoverage>
            </#list>
            <#list eml.singleDateAndDateRangeCoverages![] as tempcoverage>
            <temporalCoverage>
                <#if tempcoverage.start?has_content>
                <rangeOfDates>
                    <beginDate>
                        <calendarDate><@isodate tempcoverage.start! /></calendarDate>
                    </beginDate>
                    <endDate>
                        <calendarDate><@isodate tempcoverage.end! /></calendarDate>
                    </endDate>
                </rangeOfDates>
                <#elseif tempcoverage.date?has_content>
                <singleDateTime>
                    <calendarDate><@isodate tempcoverage.date! /></calendarDate>
                </singleDateTime>
                </#if>
            </temporalCoverage>
            </#list>
            <#list dataset.taxonomicCoverages![] as taxoncoverage>
            <#if taxoncoverage.coverages?has_content>
            <taxonomicCoverage>
                <#if taxoncoverage.description?has_content>
                <generalTaxonomicCoverage>${taxoncoverage.description}</generalTaxonomicCoverage>
                </#if>
                <#list taxoncoverage.coverages![] as tk>
                <taxonomicClassification>
                    <#if tk.rank?has_content>
                    <taxonRankName><@interpretedEnum tk.rank/></taxonRankName>
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
        </coverage>
        </#if>
        <#if dataset.purpose?has_content>
        <#noescape>
        <purpose>${eml.getDocBookField("purpose")}</purpose>
        </#noescape>
        </#if>
        <#if dataset.introduction?has_content>
        <#noescape>
        <introduction>${eml.getDocBookField("introduction")}</introduction>
        </#noescape>
        </#if>
        <#if dataset.gettingStarted?has_content>
        <#noescape>
        <gettingStarted>${eml.getDocBookField("gettingStarted")}</gettingStarted>
        </#noescape>
        </#if>
        <#if dataset.acknowledgements?has_content>
        <#noescape>
        <acknowledgements>${eml.getDocBookField("acknowledgements")}</acknowledgements>
        </#noescape>
        </#if>
        <#assign maintenanceUpdateFrequencyEmlValue = maintenanceUpdateFrequencyToEmlValue(dataset.maintenanceUpdateFrequency!)/>
        <#if maintenanceUpdateFrequencyEmlValue?has_content>
        <maintenance>
            <description>
                <para><#if dataset.maintenanceDescription?has_content>${dataset.maintenanceDescription}</#if></para>
            </description>
            <maintenanceUpdateFrequency>${maintenanceUpdateFrequencyEmlValue}</maintenanceUpdateFrequency>
        </maintenance>
        </#if>
        <#-- Current primary contacts for the dataset. The creator of the resource might be dead, left the organisation or doesnt want to be bothered. -->
        <#list eml.getContacts() as ct>
        <contact>
            <#if ct.computeCompleteName()?has_content>
            <individualName>
                <#if ct.salutation?has_content>
                <salutation>${ct.salutation}</salutation>
                </#if>
                <#if ct.lastName?has_content>
                <#if ct.firstName?has_content>
                <givenName>${ct.firstName}</givenName>
                </#if>
                <surName>${ct.lastName!}</surName>
                <#else>
                <#--surName is mandatory in EML so but all we have if no lastname is available-->
                <surName>${ct.computeCompleteName()}</surName>
                </#if>
            </individualName>
            </#if>
            <#if ct.organization?has_content>
            <organizationName>${ct.organization!}</organizationName>
            </#if>
            <#list ct.position![] as p>
            <#if p?has_content>
            <positionName>${p}</positionName>
            </#if>
            </#list>
            <#if ct.address?has_content || ct.city?has_content || ct.province?has_content || ct.postalCode?has_content || ct.country?has_content>
            <address>
                <#list ct.address![] as ad>
                <#if ad?has_content>
                <deliveryPoint>${ad}</deliveryPoint>
                </#if>
                </#list>
                <#if ct.city?has_content>
                <city>${ct.city!}</city>
                </#if>
                <#if ct.province?has_content>
                <administrativeArea>${ct.province!}</administrativeArea>
                </#if>
                <#if ct.postalCode?has_content>
                <postalCode>${ct.postalCode!}</postalCode>
                </#if>
                <#if ct.country?has_content>
                <country>${ct.country!}</country>
                </#if>
            </address>
            </#if>
            <#list ct.phone![] as p>
            <#if p?has_content>
            <phone>${p}</phone>
            </#if>
            </#list>
            <#list ct.email![] as e>
            <#if e?has_content>
            <electronicMailAddress>${e}</electronicMailAddress>
            </#if>
            </#list>
            <#list ct.homepage![] as h>
            <#if h?has_content>
            <onlineUrl>${h}</onlineUrl>
            </#if>
            </#list>
            <#if ct.userId?has_content>
            <#list ct.userId as uid>
            <@constructUserID uid/>
            </#list>
            </#if>
        </contact>
        </#list>
        <#if dataset.publishingOrganizationKey?? && dataset.publishingOrganizationName??>
        <publisher id="${dataset.publishingOrganizationKey}" scope="system" system="http://gbif.org">
            <organizationName>${dataset.publishingOrganizationName}</organizationName>
        </publisher>
        </#if>
        <#if (dataset.samplingDescription.methodSteps)?has_content>
        <methods>
            <#list dataset.samplingDescription.methodSteps![] as methodStep>
            <methodStep>
                <description>
                    <para>${methodStep!}</para>
                </description>
            </methodStep>
            </#list>
            <#if dataset.samplingDescription.studyExtent?has_content && dataset.samplingDescription.sampling?has_content >
            <sampling>
                <#if dataset.samplingDescription.studyExtent?has_content>
                <studyExtent>
                    <description>
                        <para>${dataset.samplingDescription.studyExtent}</para>
                    </description>
                </studyExtent>
                </#if>
                <#if dataset.samplingDescription.sampling?has_content >
                <samplingDescription>
                    <para>${dataset.samplingDescription.sampling}</para>
                </samplingDescription>
                </#if>
            </sampling>
            </#if>
            <#if dataset.samplingDescription.qualityControl?has_content>
            <qualityControl>
                <description>
                    <para>${dataset.samplingDescription.qualityControl}</para>
                </description>
            </qualityControl>
            </#if>
        </methods>
        </#if>

        <#if dataset.project??>
            <project <#if dataset.project.identifier?has_content>id="${dataset.project.identifier}"</#if>>
                <title>${dataset.project.title!}</title>
                <#-- The project personnel involved in a research project with a defined role. -->
                <#list dataset.project.contacts! as ct>
                <personnel>
                    <#if ct.computeCompleteName()?has_content>
                    <individualName>
                    <#if ct.salutation?has_content>
                    <salutation>${ct.salutation}</salutation>
                    </#if>
                    <#if ct.lastName?has_content>
                    <#if ct.firstName?has_content>
                    <givenName>${ct.firstName}</givenName>
                    </#if>
                    <surName>${ct.lastName!}</surName>
                    <#else>
                    <#--surName is mandatory in EML so but all we have if no lastname is available-->
                    <surName>${ct.computeCompleteName()}</surName>
                    </#if>
                    </individualName>
                    </#if>
                    <#if ct.organization?has_content>
                    <organizationName>${ct.organization!}</organizationName>
                    </#if>
                    <#list ct.position![] as p>
                    <#if p?has_content>
                    <positionName>${p}</positionName>
                    </#if>
                    </#list>
                    <#if ct.address?has_content || ct.city?has_content || ct.province?has_content || ct.postalCode?has_content || ct.country?has_content>
                    <address>
                        <#list ct.address![] as ad>
                        <#if ad?has_content>
                        <deliveryPoint>${ad}</deliveryPoint>
                        </#if>
                        </#list>
                        <#if ct.city?has_content>
                        <city>${ct.city!}</city>
                        </#if>
                        <#if ct.province?has_content>
                        <administrativeArea>${ct.province!}</administrativeArea>
                        </#if>
                        <#if ct.postalCode?has_content>
                        <postalCode>${ct.postalCode!}</postalCode>
                        </#if>
                        <#if ct.country?has_content>
                        <country>${ct.country!}</country>
                        </#if>
                    </address>
                    </#if>
                    <#list ct.phone![] as p>
                    <#if p?has_content>
                    <phone>${p}</phone>
                    </#if>
                    </#list>
                    <#list ct.email![] as e>
                    <#if e?has_content>
                    <electronicMailAddress>${e}</electronicMailAddress>
                    </#if>
                    </#list>
                    <#list ct.homepage![] as h>
                    <#if h?has_content>
                    <onlineUrl>${h}</onlineUrl>
                    </#if>
                    </#list>
                    <#if ct.userId?has_content>
                    <#list ct.userId as uid>
                    <@constructUserID uid/>
                    </#list>
                    </#if>
                    <#if ct.type?has_content>
                    <role>${ct.type}</role>
                    </#if>
                </personnel>
                </#list>
                <abstract>
                    <para>${dataset.project.abstract!}</para>
                </abstract>
                <funding>
                    <para>${dataset.project.funding!}</para>
                </funding>
                <#list dataset.project.awards! as award>
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
                <#if dataset.project.studyAreaDescription?has_content>
                <studyAreaDescription>
                    <descriptor name="generic" citableClassificationSystem="false">
                        <descriptorValue>${dataset.project.studyAreaDescription}</descriptorValue>
                    </descriptor>
                </studyAreaDescription>
                </#if>
                <#if dataset.project.designDescription?has_content>
                <designDescription>
                    <description>
                        <para>${dataset.project.designDescription}</para>
                    </description>
                </designDescription>
                </#if>
                <#list dataset.project.relatedProjects as relatedProject>
                <relatedProject <#if relatedProject.identifier?has_content>id="${relatedProject.identifier}"</#if>>
                    <title>${relatedProject.title}</title>
                    <#list relatedProject.contacts! as ct>
                    <personnel>
                        <#if ct.computeCompleteName()?has_content>
                        <individualName>
                            <#if ct.salutation?has_content>
                            <salutation>${ct.salutation}</salutation>
                            </#if>
                            <#if ct.lastName?has_content>
                            <#if ct.firstName?has_content>
                            <givenName>${ct.firstName}</givenName>
                            </#if>
                            <surName>${ct.lastName!}</surName>
                            <#else>
                            <#--surName is mandatory in EML so but all we have if no lastname is available-->
                            <surName>${ct.computeCompleteName()}</surName>
                            </#if>
                        </individualName>
                        </#if>
                        <#if ct.organization?has_content>
                        <organizationName>${ct.organization!}</organizationName>
                        </#if>
                        <#list ct.position![] as p>
                        <#if p?has_content>
                        <positionName>${p}</positionName>
                        </#if>
                        </#list>
                        <#if ct.address?has_content || ct.city?has_content || ct.province?has_content || ct.postalCode?has_content || ct.country?has_content>
                        <address>
                            <#list ct.address![] as ad>
                            <#if ad?has_content>
                            <deliveryPoint>${ad}</deliveryPoint>
                            </#if>
                            </#list>
                            <#if ct.city?has_content>
                            <city>${ct.city!}</city>
                            </#if>
                            <#if ct.province?has_content>
                            <administrativeArea>${ct.province!}</administrativeArea>
                            </#if>
                            <#if ct.postalCode?has_content>
                            <postalCode>${ct.postalCode!}</postalCode>
                            </#if>
                            <#if ct.country?has_content>
                            <country>${ct.country!}</country>
                            </#if>
                        </address>
                        </#if>
                        <#list ct.phone![] as p>
                        <#if p?has_content>
                        <phone>${p}</phone>
                        </#if>
                        </#list>
                        <#list ct.email![] as e>
                        <#if e?has_content>
                        <electronicMailAddress>${e}</electronicMailAddress>
                        </#if>
                        </#list>
                        <#list ct.homepage![] as h>
                        <#if h?has_content>
                        <onlineUrl>${h}</onlineUrl>
                        </#if>
                        </#list>
                        <#if ct.userId?has_content>
                        <#list ct.userId as uid>
                        <@constructUserID uid/>
                        </#list>
                        </#if>
                        <#if ct.type?has_content>
                        <role>${ct.type}</role>
                        </#if>
                    </personnel>
                    </#list>
                    <#if relatedProject.abstract?has_content>
                    <abstract>${relatedProject.abstract}</abstract>
                    </#if>
                </relatedProject>
                </#list>
            </project>
        </#if>
    </dataset>

    <additionalMetadata>
        <metadata>
            <gbif>
                <#--  The date the metadata document was created or modified. -->
                <dateStamp>${.now?iso("UTC")}</dateStamp>
                <#-- How to cite the resource. -->
                <#if dataset.citation?? && dataset.citation.text?has_content>
                <@citation dataset.citation />
                </#if>
                <#if dataset.bibliographicCitations?has_content>
                <bibliography>
                    <#list dataset.bibliographicCitations as bcit>
                    <@citation bcit />
                    </#list>
                </bibliography>
                </#if>
                <#list dataset.dataDescriptions![] as pdata>
                <#if pdata.name?has_content && pdata.format?has_content && pdata.url?has_content>
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
                            <url function="download">${pdata.url}</url>
                        </online>
                    </distribution>
                </physical>
                </#if>
                </#list>
                <#if dataset.logoUrl?has_content>
                <resourceLogoUrl>${dataset.logoUrl}</resourceLogoUrl>
                </#if>
                <#if dataset.collections?has_content >
                <#list dataset.collections as col>
                <collection>
                    <#if col.parentIdentifier?has_content><parentCollectionIdentifier>${col.parentIdentifier}</parentCollectionIdentifier></#if>
                    <#if col.identifier?has_content><collectionIdentifier>${col.identifier}</collectionIdentifier></#if>
                    <collectionName>${col.name!}</collectionName>
                </collection>
                </#list>
                </#if>
                <#list eml.formationPeriods![] as p>
                <formationPeriod>${p.period!}</formationPeriod>
                </#list>
                <#list dataset.collections![] as col>
                <#if col.specimenPreservationMethod??>
                <specimenPreservationMethod>${col.specimenPreservationMethod}</specimenPreservationMethod>
                </#if>
                </#list>
                <#list eml.livingTimePeriods![] as p>
                <livingTimePeriod>${p.period!}</livingTimePeriod>
                </#list>
                <#list dataset.collections![] as col>
                <#list col.curatorialUnits![] as unit>
                <#if unit.count gt 0>
                <jgtiCuratorialUnit>
                    <jgtiUnitType>${unit.type!unit.typeVerbatim!}</jgtiUnitType>
                    <jgtiUnits<#if unit.deviation gt 0> uncertaintyMeasure="${unit.deviation}"</#if>>${unit.count}</jgtiUnits>
                </jgtiCuratorialUnit>
                </#if>
                <#if unit.lower gt 0 || unit.upper gt 0>
                <jgtiCuratorialUnit>
                    <jgtiUnitType>${unit.type!unit.typeVerbatim!}</jgtiUnitType>
                    <jgtiUnitRange>
                        <beginRange>${unit.lower}</beginRange>
                        <endRange>${unit.upper}</endRange>
                    </jgtiUnitRange>
                </jgtiCuratorialUnit>
                </#if>
                </#list>
                </#list>
            </gbif>
        </metadata>
    </additionalMetadata>
    </eml:eml>
</#escape>
