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
package de.ingrid.iplug.dsc.index.mapper;

import java.sql.Connection;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;

import de.ingrid.admin.search.Stemmer;
import de.ingrid.iplug.dsc.om.DatabaseSourceRecord;
import de.ingrid.iplug.dsc.om.SourceRecord;
import de.ingrid.iplug.dsc.utils.IndexUtils;
import de.ingrid.iplug.dsc.utils.SQLUtils;
import de.ingrid.iplug.dsc.utils.ScriptEngine;
import de.ingrid.iplug.dsc.utils.TransformationUtils;

/**
 * Script based source record to lucene document mapping. This class takes a
 * {@link Resource} as parameter to specify the mapping script. The scripting
 * engine will be automatically determined from the extension of the mapping
 * script.
 * <p />
 * If the {@link compile} parameter is set to true, the script is compiled, if
 * the ScriptEngine supports compilation.
 * 
 * @author joachim@wemove.com
 * 
 */
@Order(1)
public class ScriptedDocumentMapper implements IRecordMapper {

    private Resource[] mappingScripts;
    private boolean compile = false;

    /** The default stemmer used in IndexUtils Tool !
     * Is AUTOWIRED in spring environment via {@link #setDefaultStemmer(Stemmer)}
     */
    private static Stemmer _defaultStemmer;

    private static final Logger log = Logger.getLogger(ScriptedDocumentMapper.class);

    @Override
    public void map(SourceRecord record, Document doc) throws Exception {
        if (mappingScripts == null) {
            log.error("Mapping script(s) not set!");
            throw new IllegalArgumentException("Mapping script(s) not set!");
        }
        try {
            // create utils for script
            Connection connection = (Connection) record.get(DatabaseSourceRecord.CONNECTION);
            SQLUtils sqlUtils = new SQLUtils(connection);
            IndexUtils idxUtils = new IndexUtils(doc, _defaultStemmer);
            TransformationUtils trafoUtils = new TransformationUtils(sqlUtils);
            
			Map<String, Object> parameters = new Hashtable<String, Object>();
			parameters.put("sourceRecord", record);
			parameters.put("luceneDoc", doc);
			parameters.put("log", log);
			parameters.put("SQL", sqlUtils);
			parameters.put("IDX", idxUtils);
			parameters.put("TRANSF", trafoUtils);
			parameters.put("javaVersion", System.getProperty( "java.version" ));

			ScriptEngine.execute(this.mappingScripts, parameters, compile);
        } catch (Exception e) {
            log.error("Error mapping source record to lucene document.", e);
            throw e;
        }
    }

    public Resource[] getMappingScripts() {
        return mappingScripts;
    }

	public void setMappingScripts(Resource[] mappingScripts) {
		this.mappingScripts = mappingScripts;
	}


    public boolean isCompile() {
        return compile;
    }

    public void setCompile(boolean compile) {
        this.compile = compile;
    }

    /** Injects default stemmer via autowiring !
     * @param defaultStemmer
     */
    @Autowired
    public void setDefaultStemmer(Stemmer defaultStemmer) {
    	_defaultStemmer = defaultStemmer;
	}
}
