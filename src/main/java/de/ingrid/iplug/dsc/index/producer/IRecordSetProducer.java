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
package de.ingrid.iplug.dsc.index.producer;

import de.ingrid.iplug.dsc.om.SourceRecord;

/**
 * This interface must be implemented from all record producing classes. Record
 * producer are objects that know how to produce a list of source records, that
 * can be mapped into other formats later (i.e. Lucene Documents).
 * They need to be configured with the appropriate datasource access parameters.
 *
 * @author joachim@wemove.com
 *
 */
public interface IRecordSetProducer {

    /**
     * Returns true if more records are available and false if not.
     *
     * @return
     */
    public boolean hasNext() throws Exception;

    /**
     * Retrieves the next record from the data source and returns it.
     *
     * @return
     * @throws Exception
     */
    public SourceRecord next() throws Exception;


}
