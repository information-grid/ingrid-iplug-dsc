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

// ---------- hoehensystem ----------
var hoehensystemId = sourceRecord.get(DatabaseSourceRecord.ID);
var hoehensystemRows = SQL.all("SELECT * FROM hoehensystem WHERE id=?", [hoehensystemId]);
for (i=0; i<hoehensystemRows.size(); i++) {
    var hoehensystemRow = hoehensystemRows.get(i);
    var row = hoehensystemRow;
    var title = "Stammdaten HOEHENSYSTEM: ";
    var summary = "";
	var geobasUrl = "http://10.140.105.57:8080/geobas_q1/main?cmd=view_details&id=";

    IDX.add("hoehensystem.id", row.get("id"));
    IDX.add("hoehensystem.bundesland", row.get("bundesland"));
    IDX.add("hoehensystem.historie", row.get("historie"));
    IDX.add("hoehensystem.aktion", row.get("aktion"));
    IDX.add("hoehensystem.hoehensystemdef", row.get("hoehensystemdef"));
    IDX.add("hoehensystem.organisation", row.get("organisation"));
    IDX.add("hoehensystem.hoehensystemnummer", row.get("hoehensystemnummer"));
    IDX.add("hoehensystem.original", row.get("original"));

	title = title + row.get("hoehensystemnummer");
	geobasUrl = geobasUrl + row.get("id") + "&table=hoehensystem";

    // ---------- bundesland ----------
    var rows = SQL.all("SELECT * FROM bundesland WHERE id=?", [hoehensystemRow.get("bundesland")]);
    for (j=0; j<rows.size(); j++) {
    	var row = rows.get(j);
        IDX.add("bundesland.id", row.get("id"));
        IDX.add("bundesland.name", row.get("name"));
    	summary = summary + row.get("name");
   }

    // ---------- hoehensystemdef ----------
    var rows = SQL.all("SELECT * FROM hoehensystemdef WHERE id=?", [hoehensystemRow.get("hoehensystemdef")]);
    for (j=0; j<rows.size(); j++) {
    	var row = rows.get(j);
        IDX.add("hoehensystemdef.id", row.get("id"));
        IDX.add("hoehensystemdef.name", row.get("name"));
    	summary = summary + ", " + row.get("name");
    }

	summary = summary + ", " + hoehensystemRow.get("hoehensystemnummer");

    IDX.add("title", title);
    IDX.add("summary", summary);
    
    // deliver url or NOT !?
    // changes display in result list !
    // with URL the url is displayed below summary and title links to URL
    // without url title links to detail view !
    //IDX.add("url", geobasUrl);
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
