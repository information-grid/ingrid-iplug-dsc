package de.ingrid.iplug.dsc.record;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.springframework.core.io.FileSystemResource;

import de.ingrid.iplug.dsc.record.mapper.CreateIdfMapper;
import de.ingrid.iplug.dsc.record.mapper.IIdfMapper;
import de.ingrid.iplug.dsc.record.mapper.ScriptedIdfMapper;
import de.ingrid.iplug.dsc.record.producer.PlugDescriptionConfiguredDatabaseRecordProducer;
import de.ingrid.utils.PlugDescription;
import de.ingrid.utils.dsc.Record;
import de.ingrid.utils.xml.PlugdescriptionSerializer;

public class Igc_3_0_0_To_Idf_1_0_RecordCreatorTestLocal extends TestCase {

    public void testDscRecordCreator() throws Exception {

        File plugDescriptionFile = new File(
                "src/test/resources/plugdescription_igc-3.0.0_to_idf-1.0_test.xml");
        PlugDescription pd = new PlugdescriptionSerializer()
                .deSerialize(plugDescriptionFile);

        PlugDescriptionConfiguredDatabaseRecordProducer p = new PlugDescriptionConfiguredDatabaseRecordProducer();
        p.setIndexFieldID("ID");
        p.configure(pd);

        CreateIdfMapper m1 = new CreateIdfMapper();
        ScriptedIdfMapper m2 = new ScriptedIdfMapper();
        m2.setMappingScript(new FileSystemResource("src/main/resources/mapping/igc-3.0.0_to_idf-1.0.js"));

        List<IIdfMapper> mList = new ArrayList<IIdfMapper>();
        mList.add(m1);
        mList.add(m2);
        
        DscRecordCreator dc = new DscRecordCreator();
        dc.setRecordProducer(p);
        dc.setRecord2IdfMapperList(mList);

        Document idxDoc = new Document();
        idxDoc.add(new Field("ID", "3786", Field.Store.YES,
                        Field.Index.ANALYZED));
        Record r = dc.getRecord(idxDoc);
        assertNotNull(r.get("data"));
        assertTrue(r.getString("compressed").equals("false"));
        System.out.println("Size of uncompressed IDF document: " + r.getString("data").length());
        
    }

}
