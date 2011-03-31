/**
 * 
 */
package de.ingrid.iplug.dsc.utils;

import java.io.IOException;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.util.Version;

/**
 * Singleton helper class encapsulating functionality on Lucene Index (e.g. used in mapping script).
 *  
 * @author Martin
 */
public class IndexUtils {

    private static final Logger log = Logger.getLogger(IndexUtils.class);

    private static String CONTENT_FIELD_NAME = "content";

    /** the Lucene Document where the fields are added ! */
    private Document luceneDoc = null;

    /** analyzer for stemming ! always german one ???  NO WE USE STANDARD ANALYZER due to different languages ! */
//    private static GermanAnalyzer fAnalyzer = new GermanAnalyzer(new String[0]);
    private static StandardAnalyzer fAnalyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

    // our single instance !
	private static IndexUtils myInstance;

	/** Get The Singleton.
	 * NOTICE: Resets internal state (uses passed luceneDoc). */
	public static synchronized IndexUtils getInstance(Document luceneDoc) {
		if (myInstance == null) {
	        myInstance = new IndexUtils();
		}
		myInstance.initialize(luceneDoc);

		return myInstance;
	}

	private IndexUtils() {
	}
	private void initialize(Document luceneDoc) {
		this.luceneDoc = luceneDoc;
	}
	
	/** Add a index field with the value to the index document.
	 * The field will be TOKENIZE and STORE and will be added to a separate
	 * "content" field (ADD_TO_CONTENT_FIELD) by default.
	 * @param fieldName name of the field in the index
	 * @param value content of the field !
	 */
	public void add(String fieldName, String value) throws IOException {
		if (value == null) {
			value = "";
		}
		add(fieldName, value, Field.Store.YES, Field.Index.ANALYZED);
		add(CONTENT_FIELD_NAME, value, Field.Store.NO, Field.Index.ANALYZED);
		add(CONTENT_FIELD_NAME, filterTerm(value), Field.Store.NO, Field.Index.ANALYZED);
	}

	private void add(String fieldName, String value,
			Field.Store stored,
			Field.Index tokenized) {
		if (log.isDebugEnabled()) {
	        log.debug("Add field '" + fieldName + "' with value '" + value + "' to lucene document " +
	        		"(Field.Index=" + tokenized + ", Field.Store=" + stored + ")");			
		}
        
		luceneDoc.add(new Field(fieldName, value, stored, tokenized));
	}

	/** Remove Fields.
	 * @param fieldName name of field to remove.
	 */
	public void removeFields(String fieldName) {
		if (log.isDebugEnabled()) {
	        log.debug("Removed ALL fields with name '" + fieldName + "'.");			
		}

		luceneDoc.removeFields(fieldName);
	}
    private static String filterTerm(String term) {
        String result = "";

        TokenStream stream = fAnalyzer.tokenStream(null, new StringReader(term));
        // get the TermAttribute from the TokenStream
        TermAttribute termAtt = (TermAttribute) stream.addAttribute(TermAttribute.class);

        try {
            stream.reset();
            // add all tokens until stream is exhausted
            while (stream.incrementToken()) {
            	result = result + " " + termAtt.term();
            }
            stream.end();
            stream.close();
        } catch (IOException ex) {
        	log.error("Problems tokenizing term " + term + ", we return full term.", ex);
        	result = term;
        }

        return result.trim();
    }
}
