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
package de.ingrid.iplug.dsc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;

import com.tngtech.configbuilder.annotation.propertyloaderconfiguration.PropertiesFiles;
import com.tngtech.configbuilder.annotation.propertyloaderconfiguration.PropertyLocations;
import com.tngtech.configbuilder.annotation.valueextractor.DefaultValue;
import com.tngtech.configbuilder.annotation.valueextractor.PropertyValue;

import de.ingrid.admin.IConfig;
import de.ingrid.admin.command.PlugdescriptionCommandObject;
import de.ingrid.iplug.dsc.index.DatabaseConnection;
import de.ingrid.utils.PlugDescription;

@PropertiesFiles( {"config"} )
@PropertyLocations(directories = {"conf"}, fromClassLoader = true)
public class Configuration implements IConfig {

    private static Log log = LogFactory.getLog(Configuration.class);

    @PropertyValue("iplug.database.driver")
    @DefaultValue("com.mysql.jdbc.Driver")
    public String databaseDriver;

    @PropertyValue("iplug.database.url")
    @DefaultValue("jdbc:mysql://localhost:3306/igc")
    public String databaseUrl;

    @PropertyValue("iplug.database.username")
    public String databaseUsername;

    @PropertyValue("iplug.database.password")
    public String databasePassword;

    @PropertyValue("iplug.database.schema")
    public String databaseSchema;


    @PropertyValue("spring.profile")
    public String springProfile;

    @PropertyValue("plugdescription.CORRESPONDENT_PROXY_SERVICE_URL")
    public String correspondentIPlug;

    @Override
    public void initialize() {

        // activate the configured spring profile defined in SpringConfiguration.java
        if ( springProfile != null ) {
            System.setProperty( "spring.profiles.active", springProfile );
        } else {
            log.error( "Spring profile not set! In configuration set 'spring.profile' to one of 'object_internet', 'object_intranet', 'address_internet' or 'address_intranet'" );
            System.exit( 1 );
        }
    }

    @Override
    public void addPlugdescriptionValues( PlugdescriptionCommandObject pdObject ) {
        pdObject.put( "iPlugClass", "de.ingrid.iplug.dsc.DscSearchPlug" );

        String fieldFile = "fields_object.data";
        if (springProfile.startsWith( "address" )) {
            fieldFile = "fields_address.data";
        }
        List<String> fields = getFieldsFromFile( fieldFile );

        for (String field : fields) {
            pdObject.removeFromList( PlugDescription.FIELDS, field );
            pdObject.addField( field );
        }
        // add necessary fields so iBus actually will query us
        // remove field first to prevent multiple equal entries
        pdObject.removeFromList(PlugDescription.FIELDS, "incl_meta");
        pdObject.addField("incl_meta");
        pdObject.removeFromList(PlugDescription.FIELDS, "t01_object.obj_class");
        pdObject.addField("t01_object.obj_class");
        pdObject.removeFromList(PlugDescription.FIELDS, "metaclass");
        pdObject.addField("metaclass");

        DatabaseConnection dbc = new DatabaseConnection( databaseDriver, databaseUrl, databaseUsername, databasePassword, databaseSchema );
        pdObject.setConnection( dbc );

        pdObject.setCorrespondentProxyServiceURL( correspondentIPlug );
    }

    private List<String> getFieldsFromFile(String fieldsFileName) {
        List<String> fieldsAsLine = new ArrayList<String>();
        ClassPathResource fieldsFile = new ClassPathResource( fieldsFileName );
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(fieldsFile.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                 fieldsAsLine.add( line );
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fieldsAsLine;
    }

    @Override
    public void setPropertiesFromPlugdescription( Properties props, PlugdescriptionCommandObject pd ) {
        DatabaseConnection connection = (DatabaseConnection) pd.getConnection();
        databaseDriver = connection.getDataBaseDriver();
        databaseUrl = connection.getConnectionURL();
        databaseUsername = connection.getUser();
        databasePassword = connection.getPassword();
        databaseSchema = connection.getSchema();

        props.setProperty( "iplug.database.driver", databaseDriver);
        props.setProperty( "iplug.database.url", databaseUrl);
        props.setProperty( "iplug.database.username", databaseUsername);
        props.setProperty( "iplug.database.password", databasePassword);
        props.setProperty( "iplug.database.schema", databaseSchema);
    }


}
