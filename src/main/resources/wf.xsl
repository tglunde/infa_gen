<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml"
                doctype-system="powrmart.dtd"/>
<xsl:template name="main">
 <POWERMART CREATION_DATE="02/19/2019 17:09:41" REPOSITORY_VERSION="187.96">
 <REPOSITORY NAME="REP_ETLD" VERSION="187" CODEPAGE="Latin1" DATABASETYPE="DB2">
    <xsl:copy-of select="collection(iri-to-uri(concat('./target/wf', '?select=*.XML')))//FOLDER"/>
</REPOSITORY>
</POWERMART>
</xsl:template>
</xsl:stylesheet>