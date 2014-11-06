/*
 * Copyright (c) 2014 wemove GmbH
 * Licensed under the EUPL V.1.1
 *
 * This Software is provided to You under the terms of the European
 * Union Public License (the "EUPL") version 1.1 as published by the
 * European Union. Any use of this Software, other than as authorized
 * under this License is strictly prohibited (to the extent such use
 * is covered by a right of the copyright holder of this Software).
 *
 * This Software is provided under the License on an "AS IS" basis and
 * without warranties of any kind concerning the Software, including
 * without limitation merchantability, fitness for a particular purpose,
 * absence of defects or errors, accuracy, and non-infringement of
 * intellectual property rights other than copyright. This disclaimer
 * of warranty is an essential part of the License and a condition for
 * the grant of any rights to this Software.
 *
 * For more  details, see <http://joinup.ec.europa.eu/software/page/eupl>
 */
importPackage(Packages.org.w3c.dom);
importPackage(Packages.de.ingrid.iplug.dsc.om);

if (log.isDebugEnabled()) {
	log.debug("Mapping source record to idf document: " + sourceRecord.toString());
}

if (!(sourceRecord instanceof DatabaseSourceRecord)) {
    throw new IllegalArgumentException("Record is no DatabaseRecord!");
}

// ---------- <idf:body> ----------
var idfBody = XPATH.getNode(idfDoc, "/idf:html/idf:body");

// ========== punktkennzeichen ==========
var punktkennzeichenId = sourceRecord.get(DatabaseSourceRecord.ID);
var punktkennzeichenRows = SQL.all("SELECT * FROM punktkennzeichen WHERE id=?", [punktkennzeichenId]);
for (i=0; i<punktkennzeichenRows.size(); i++) {
    var punktkennzeichenRow = punktkennzeichenRows.get(i);
    var title = "Punktkennzeichen: ";

    // ---------- punktart for title ----------
    var rowsPunktart = SQL.all("SELECT * FROM punktart WHERE id=?", [punktkennzeichenRow.get("punktart")]);
    for (j=0; j<rowsPunktart.size(); j++) {
        var row = rowsPunktart.get(j);
    	title = title + row.get("name") + " (" + row.get("kurzbezeichnung") + ")";
    }

    // ---------- bundeswasserstr for title ----------
    var rowsBundeswasserstr = SQL.all("SELECT * FROM bundeswasserstr WHERE id=?", [punktkennzeichenRow.get("bundeswasserstr")]);
    for (j=0; j<rowsBundeswasserstr.size(); j++) {
    	var row = rowsBundeswasserstr.get(j);
        title = title + ", " + row.get("kurzbezeichnung");
    }

    title = title + ", " + punktkennzeichenRow.get("station");
    title = title + ", " + punktkennzeichenRow.get("punktnummer");

    DOM.addElement(idfBody, "h1").addText(title);
    DOM.addElement(idfBody, "p");

    // ---------- punktart ----------
    for (j=0; j<rowsPunktart.size(); j++) {
        var row = rowsPunktart.get(j);
        DOM.addElement(idfBody, "p").addText("Punktart: " + row.get("name") + ", " + row.get("kurzbezeichnung"));
    }

    // ---------- bundeswasserstr ----------
    for (j=0; j<rowsBundeswasserstr.size(); j++) {
        var row = rowsBundeswasserstr.get(j);
        DOM.addElement(idfBody, "p").addText("Bundeswasserstr: " + row.get("name") + ", " + row.get("kurzbezeichnung"));
    }

    // ---------- ufer ----------
    var rows = SQL.all("SELECT * FROM ufer WHERE id=?", [punktkennzeichenRow.get("ufer")]);
    for (j=0; j<rows.size(); j++) {
    	var row = rows.get(j);
        DOM.addElement(idfBody, "p").addText("Ufer: " + row.get("name") + ", " + row.get("kurzbezeichnung"));
    }

    // ---------- punktkennzeichen ----------
    DOM.addElement(idfBody, "p").addText("Station: " + punktkennzeichenRow.get("station"));
    DOM.addElement(idfBody, "p").addText("Punktnummer: " + punktkennzeichenRow.get("punktnummer"));

    // ---------- link to GEOBAS ----------
    DOM.addElement(idfBody, "p");
    DOM.addElement(idfBody, "p/a")
        .addAttribute("href", "http://geobas.wsv.bvbs.bund.de/geobas_p1/main?cmd=view_details&id=" + punktkennzeichenRow.get("id") + "&table=punktkennzeichen")
        .addAttribute("target", "_blank")
        .addText("GEOBAS")
}

function hasValue(val) {
    if (typeof val == "undefined") {
        return false; 
    } else if (val == null) {
        return false; 
    } else if (typeof val == "string" && val == "") {
        return false;
    } else if (typeof val == "object" && val.toString().equals("")) {
        return false;
    } else {
      return true;
    }
}
