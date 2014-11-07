/*
 * **************************************************-
 * ingrid-iplug-dsc-scripted
 * ==================================================
 * Copyright (C) 2014 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
package de.ingrid.iplug.dsc.record.producer;

import org.apache.lucene.document.Document;

import de.ingrid.iplug.dsc.om.IClosableDataSource;
import de.ingrid.iplug.dsc.om.SourceRecord;

/**
 * Defines all aspects a record producer must implement. The record producer is
 * used to retrieve ONE record from the data source, based on a
 * LuceneDocument for further processing.
 *
 * @author joachim@wemove.com
 *
 */
public interface IRecordProducer {

    /**
     * Open the data source. The functionality depends on the type of data
     * source. Returns a closable data source.
     *
     * The parameters in {@link SourceRecord} returned by {@link getRecord} may
     * contain a reference to the data source, so that the following mapping
     * step can access the data source as well.
     *
     */
    IClosableDataSource openDatasource();

    /**
     * Get a record from the data source. How the record must be derived from
     * the fields of the lucene document.
     *
     * @param doc
     * @param ds
     * @return
     */
    SourceRecord getRecord(Document doc, IClosableDataSource ds);

}
