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
package de.ingrid.iplug.dsc.index;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.dbunit.DBTestCase;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;

import de.ingrid.iplug.dsc.index.mapper.IRecordMapper;
import de.ingrid.iplug.dsc.index.mapper.IgcProfileDocumentMapper;
import de.ingrid.iplug.dsc.index.producer.PlugDescriptionConfiguredDatabaseRecordSetProducer;
import de.ingrid.utils.PlugDescription;
import de.ingrid.utils.xml.PlugdescriptionSerializer;

public class IgcProfileDocumentProducerTest extends DBTestCase {

    public static String DATASOURCE_FILE_NAME = "src/test/resources/dataset_igc_profile.xml";
    
    public IgcProfileDocumentProducerTest(String name) {
        super( name );
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, "org.hsqldb.jdbcDriver" );
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, "jdbc:hsqldb:mem:sample" );
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, "sa" );
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, "" );
    }

    @Override
    protected void setUp() throws Exception {
        System.out.println("Try creating tables from data source file: " + DATASOURCE_FILE_NAME);
        IDataSet ds = new XmlDataSet(new FileInputStream(DATASOURCE_FILE_NAME));
        createHsqldbTables(ds, this.getConnection().getConnection());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.out.println("Drop all tables.");
        PreparedStatement pp = this.getConnection().getConnection().prepareStatement("DROP SCHEMA PUBLIC CASCADE");
        pp.executeUpdate();
        pp.close();
    }


    @Override
    protected IDataSet getDataSet() throws Exception {
        System.out.println("Populating from data source file: " + DATASOURCE_FILE_NAME);
        IDataSet ds = new XmlDataSet(new FileInputStream(DATASOURCE_FILE_NAME));
        return ds;
    }

    
    @Override
    protected void setUpDatabaseConfig(DatabaseConfig config) {
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
    }


    private void createHsqldbTables(IDataSet dataSet, Connection connection) throws DataSetException, SQLException {
        String[] tableNames = dataSet.getTableNames();

        String sql = "";
        for (String tableName : tableNames) {
          ITable table = dataSet.getTable(tableName);
          ITableMetaData metadata = table.getTableMetaData();
          Column[] columns = metadata.getColumns();

          sql = "create memory table " + tableName + "( ";
          boolean first = true;
          for (Column column : columns) {
            if (!first) {
              sql += ", ";
            }
            String columnName = column.getColumnName();
            String type = resolveType((String) table.getValue(0, columnName));
            sql += columnName + " " + type;
            if (first) {
              sql += " primary key";
              first = false;
            }
          }
          sql += "); ";
          PreparedStatement pp = connection.prepareStatement(sql);
          pp.executeUpdate();
          pp.close();
        }
    }

    private String resolveType(String str) {
      try {
        if (new Double(str).toString().equals(str)) {
          return "double";
        }
      } catch (Exception e) {}

      try {
        if (new Integer(str).toString().equals(str)) {
          return "int";
        }
      } catch (Exception e) {}

      return "varchar(255)";
    }
    
    
    public String getDatasourceFileName() {
        return DATASOURCE_FILE_NAME;
    }


    public void testDscRecordCreator() throws Exception {

        File plugDescriptionFile = new File(
                "src/test/resources/plugdescription_db_test.xml");
        PlugDescription pd = new PlugdescriptionSerializer()
                .deSerialize(plugDescriptionFile);

        PlugDescriptionConfiguredDatabaseRecordSetProducer p = new PlugDescriptionConfiguredDatabaseRecordSetProducer();
        p.setRecordSql("SELECT id FROM t01_object");
        p.configure(pd);

        IgcProfileDocumentMapper m = new IgcProfileDocumentMapper();
        m.setSql("SELECT VALUE AS igc_profile FROM TEST_TABLE WHERE KEY='igc_profile'");

        List<IRecordMapper> mList = new ArrayList<IRecordMapper>();
        mList.add(m);
        
        DscDocumentProducer dp = new DscDocumentProducer();
        dp.setRecordSetProducer(p);
        dp.setRecordMapperList(mList);

        if (dp.hasNext()) {
            Document doc = dp.next();
            assertNotNull(doc);
            assertEquals(doc.getField("indexName0").stringValue(), "test content for field id2");
            assertEquals(doc.getFields("indexName5").length, 2);
        } else {
            fail("No documnet produced");
        }
        
    }

}
