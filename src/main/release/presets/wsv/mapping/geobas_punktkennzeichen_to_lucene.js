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
importPackage(Packages.org.apache.lucene.document);
importPackage(Packages.de.ingrid.iplug.dsc.om);
importPackage(Packages.de.ingrid.geo.utils.transformation);

if (log.isDebugEnabled()) {
	log.debug("Mapping source record to lucene document: " + sourceRecord.toString());
}

if (!(sourceRecord instanceof DatabaseSourceRecord)) {
    throw new IllegalArgumentException("Record is no DatabaseRecord!");
}

// ---------- punktkennzeichen ----------
var punktkennzeichenId = sourceRecord.get(DatabaseSourceRecord.ID);
var punktkennzeichenRows = SQL.all("SELECT * FROM punktkennzeichen WHERE id=?", [punktkennzeichenId]);
for (i=0; i<punktkennzeichenRows.size(); i++) {
    var punktkennzeichenRow = punktkennzeichenRows.get(i);
    var title = "Punktkennzeichen: ";
    var summary = "Punkt: ";

    addPunktkennzeichen(punktkennzeichenRow);

    // ---------- punktart ----------
    if (hasValue(punktkennzeichenRow.get("punktart"))) {
        var rows = SQL.all("SELECT * FROM punktart WHERE id=?", [punktkennzeichenRow.get("punktart")]);
        for (j=0; j<rows.size(); j++) {
        	addPunktart(rows.get(j));

        	title = title + rows.get(j).get("name") + " (" + rows.get(j).get("kurzbezeichnung") + ")";
        	summary = summary + rows.get(j).get("name");
        }    	
    } else {
    	title = title + punktkennzeichenRow.get("punktart");
    	summary = summary + ", " + punktkennzeichenRow.get("punktart");
    }

    // ---------- bundeswasserstr ----------
    if (hasValue(punktkennzeichenRow.get("bundeswasserstr"))) {
        var rows = SQL.all("SELECT * FROM bundeswasserstr WHERE id=?", [punktkennzeichenRow.get("bundeswasserstr")]);
        for (j=0; j<rows.size(); j++) {
        	addBundeswasserstr(rows.get(j));
        	summary = summary + ", " + rows.get(j).get("name") + " (" + rows.get(j).get("kurzbezeichnung") + ")";
        	title = title + ", " + rows.get(j).get("kurzbezeichnung");
        }    	
    } else {
    	summary = summary + ", " + punktkennzeichenRow.get("bundeswasserstr");
    }

    // ---------- ufer ----------
    if (hasValue(punktkennzeichenRow.get("ufer"))) {
        var rows = SQL.all("SELECT * FROM ufer WHERE id=?", [punktkennzeichenRow.get("ufer")]);
        for (j=0; j<rows.size(); j++) {
        	addUfer(rows.get(j));
        	summary = summary + ", " + rows.get(j).get("name");
        }    	
    } else {
    	summary = summary + ", " + punktkennzeichenRow.get("ufer");
    }

    // ---------- punktkennzeichen ----------
	title = title + ", " + punktkennzeichenRow.get("station");
	title = title + ", " + punktkennzeichenRow.get("punktnummer");
	summary = summary + ", " + punktkennzeichenRow.get("station");
	summary = summary + ", " + punktkennzeichenRow.get("punktnummer");


    IDX.add("title", title);
    IDX.add("summary", summary);
}

function addPunktkennzeichen(row) {
    IDX.add("punktkennzeichen.id", row.get("id"));
    IDX.add("punktkennzeichen.station", row.get("station"));
    IDX.add("punktkennzeichen.punktnummer", row.get("punktnummer"));
    IDX.add("punktkennzeichen.valid", row.get("valid"));
}
function addPunktart(row) {
    IDX.add("punktart.id", row.get("id"));
    IDX.add("punktart.kurzbezeichnung", row.get("kurzbezeichnung"));
    IDX.add("punktart.name", row.get("name"));
}
function addBundeswasserstr(row) {
    IDX.add("bundeswasserstr.id", row.get("id"));
    IDX.add("bundeswasserstr.kurzbezeichnung", row.get("kurzbezeichnung"));
    IDX.add("bundeswasserstr.name", row.get("name"));
}
function addUfer(row) {
    IDX.add("ufer.id", row.get("id"));
    IDX.add("ufer.kurzbezeichnung", row.get("kurzbezeichnung"));
    IDX.add("ufer.name", row.get("name"));
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
