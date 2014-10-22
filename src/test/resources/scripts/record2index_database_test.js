/**
 * SourceRecord to Lucene Document mapping
 * Copyright (c) 2008 wemove digital solutions. All rights reserved.
 *
 * The following global variable are passed from the application:
 *
 * @param sourceRecord A SourceRecord instance, that defines the input
 * @param luceneDoc A lucene Document instance, that defines the output
 * @param log A Log instance
 *
 */
if (javaVersion.indexOf( "1.8" ) === 0) {
	load("nashorn:mozilla_compat.js");
}

importPackage(Packages.java.sql);
importPackage(Packages.org.apache.lucene.document);
importPackage(Packages.de.ingrid.iplug.dsc.om);

if (log.isDebugEnabled()) {
	log.debug("Mapping source record to lucene document: " + sourceRecord.toString());
}


if (!(sourceRecord instanceof DatabaseSourceRecord)) {
    throw new IllegalArgumentException("Record is no DatabaseRecord!");
}

var id = sourceRecord.get(DatabaseSourceRecord.ID);
var connection = sourceRecord.get(DatabaseSourceRecord.CONNECTION);
try {
    var ps = connection.prepareStatement("SELECT * FROM TEST_TABLE WHERE id=?");
    ps.setString(1, id);
    var rs = ps.executeQuery();
    rs.next();

    for (var i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
        var colName = rs.getMetaData().getColumnName(i);
        var colValue = rs.getString(i);
        luceneDoc.add(new Field(colName, colValue, Field.Store.YES,
                Field.Index.ANALYZED));
    }
} catch (e) {
    log.error("Error mapping Record." + e);
    throw e;
}
