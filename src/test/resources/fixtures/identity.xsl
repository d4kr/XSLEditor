<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format">
  <xsl:template match="/doc">
    <fo:root>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="p" page-width="100mm" page-height="100mm">
          <fo:region-body/>
        </fo:simple-page-master>
      </fo:layout-master-set>
      <fo:page-sequence master-reference="p">
        <fo:flow flow-name="xsl-region-body">
          <fo:block><xsl:value-of select="item"/></fo:block>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>
