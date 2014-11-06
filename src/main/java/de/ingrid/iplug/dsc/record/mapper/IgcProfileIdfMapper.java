/**
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
/**
 * 
 */
package de.ingrid.iplug.dsc.record.mapper;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.ingrid.iplug.dsc.om.DatabaseSourceRecord;
import de.ingrid.iplug.dsc.om.SourceRecord;
import de.ingrid.iplug.dsc.utils.DOMUtils;
import de.ingrid.iplug.dsc.utils.IdfUtils;
import de.ingrid.iplug.dsc.utils.SQLUtils;
import de.ingrid.iplug.dsc.utils.TransformationUtils;
import de.ingrid.utils.xml.ConfigurableNamespaceContext;
import de.ingrid.utils.xml.IDFNamespaceContext;
import de.ingrid.utils.xml.IgcProfileNamespaceContext;
import de.ingrid.utils.xpath.XPathUtils;

/**
 * Analyzes a IGC Profile, obtained from an IGC database and executes all
 * scripted on a supplied InGrid Detail data Format (IDF).
 * <p />
 * A SQL string can be set to retrieve the IGC profile. The profile is expected
 * to be in a SQL record property named "igc_profile".
 * <p/>
 * The mapper expects a base IDF format already present in {@link doc}.
 * 
 * @author joachim@wemove.com
 * 
 */
@Order(3)
public class IgcProfileIdfMapper implements IIdfMapper {

    protected static final Logger log = Logger.getLogger(IgcProfileIdfMapper.class);

    private String sql;
    
    private ScriptEngine engine = null;

    @Override
    public void map(SourceRecord record, Document doc) throws Exception {
        if (!(record instanceof DatabaseSourceRecord)) {
            throw new IllegalArgumentException("Record is no DatabaseRecord!");
        }
        ConfigurableNamespaceContext cnc = new ConfigurableNamespaceContext();
        cnc.addNamespaceContext(new IDFNamespaceContext());
        cnc.addNamespaceContext(new IgcProfileNamespaceContext());
        
        XPathUtils xpathUtils = new XPathUtils(cnc);
        if (!(xpathUtils.nodeExists(doc, "//idf:html"))) {
            throw new IllegalArgumentException("Document is no IDF!");
        }
        Connection connection = (Connection) record.get(DatabaseSourceRecord.CONNECTION);
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            rs.next();
            String igcProfileStr = rs.getString("igc_profile");
            if (log.isDebugEnabled()) {
                log.debug("igc profile found: " + igcProfileStr);
            }
            ps.close();
            if (igcProfileStr != null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder db;
                db = dbf.newDocumentBuilder();
                Document igcProfile = db.parse(new InputSource(new StringReader(igcProfileStr)));
                NodeList igcProfileCswMappings = xpathUtils.getNodeList(igcProfile, "//igcp:controls/*/igcp:scriptedCswMapping");
                if (log.isDebugEnabled()) {
                    log.debug("cswMappings found: " + igcProfileCswMappings.getLength());
                }
                for (int i=0; i<igcProfileCswMappings.getLength(); i++) {
                    String igcProfileCswMapping = igcProfileCswMappings.item(i).getTextContent();
                    if (log.isDebugEnabled()) {
                        log.debug("Found Mapping Script: \n" + igcProfileCswMapping);
                    }
                    if (igcProfileCswMapping != null && igcProfileCswMapping.trim().length() > 0) {
                        Node igcProfileNode = igcProfileCswMappings.item(i).getParentNode();
                        try {
                            if (engine == null) {
                                ScriptEngineManager mgr = new ScriptEngineManager();
                                engine = mgr.getEngineByExtension("js");
                            }
    
                            // create utils for script
                            SQLUtils sqlUtils = new SQLUtils(connection);
                            // get initialized XPathUtils (see above)
                            TransformationUtils trafoUtils = new TransformationUtils(sqlUtils);
                            DOMUtils domUtils = new DOMUtils(doc, xpathUtils);
                            domUtils.addNS("idf", "http://www.portalu.de/IDF/1.0");
                            
                            IdfUtils idfUtils = new IdfUtils(sqlUtils, domUtils, xpathUtils);
    
                            Bindings bindings = engine.createBindings();
                            bindings.put("sourceRecord", record);
                            bindings.put("idfDoc", doc);
                            bindings.put("igcProfileControlNode", igcProfileNode);
                            bindings.put("log", log);
                            bindings.put("SQL", sqlUtils);
                            bindings.put("XPATH", xpathUtils);
                            bindings.put("TRANSF", trafoUtils);
                            bindings.put("DOM", domUtils);
                            bindings.put("IDF", idfUtils);
    
                            // backwards compatibility
                            if (System.getProperty( "java.version" ).startsWith( "1.8" )) {
                                igcProfileCswMapping = "load('nashorn:mozilla_compat.js');" + igcProfileCswMapping;
                                // somehow the contant cannot be accessed!?
                                igcProfileCswMapping = igcProfileCswMapping.replaceAll( "DatabaseSourceRecord.ID", "'" + DatabaseSourceRecord.ID + "'" );
                            }
                            engine.eval(new StringReader(igcProfileCswMapping), bindings);
                        } catch (Exception e) {
                            log.error("Error mapping source record to idf document.", e);
                            throw e;
                        }
                    }
                }
                
            }
        } catch (SQLException e) {
            log.error("Error mapping IGC profile.", e);
            throw e;
        } finally {
        	// isClosed() CAUSES EXCEPTION ON ORACLE !!!
            // if (ps != null && !ps.isClosed()) {
        	if (ps != null) {
                ps.close();
            }
        }

    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

}
