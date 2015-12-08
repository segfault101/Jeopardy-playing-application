import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


public class WIkiIndexer {
	public static void main(String[] args) throws IOException, ParseException {

		File FILE_DIR = new File("D:\\Desktop\\IR docs\\wiki-subset-20140602\\");
//		System.out.println("Enter absolute file path:");
//		String FILE_PATH = System.console().readLine();

		//create lemmatizer
		StanfordLemmatizer lemmatizer = new StanfordLemmatizer();

//		System.out.println("Enter query :");
//		String querystr = System.console().readLine();

		String clue = "Daniel Hertzberg & James B. Stewart of this paper shared a 1988 Pulitzer for their stories about insider trading";
		String category = "NEWSPAPERS";

		//lemmatize the parts of query
		clue = lemmatizer.lemmatize(clue);
		category = lemmatizer.lemmatize(category);
		
		//concat to form query string
		String querystr = clue + " " + category ;

		
		//construct a term to boost map to use with multifield query parser
		Map<String, Float> boostMap = new HashMap<String, Float>();
		
		StringTokenizer catTokens = new StringTokenizer(category);
		
		while(catTokens.hasMoreTokens())
			boostMap.put(catTokens.nextToken(), 1.2f);
		
		StringTokenizer clueTokens = new StringTokenizer(clue);

		while(clueTokens.hasMoreTokens())
			boostMap.put(clueTokens.nextToken(), 1.0f);
		
		
		//flag
		int useExistingIndex = 0;
		
		// Specify the analyzer for tokenizing text when indexing and searching
		Analyzer analyzer = createAnalyzer();//new EnglishAnalyzer(Version.LUCENE_40);
		
		//specify index location
		File indexFile = new File("./tmp/index"); 
				
		//check if index already exists and set flag
		if(indexFile.list() != null)
			useExistingIndex = 1;
		
		// load the index or create it if it doesn't exist
		FSDirectory index = FSDirectory.open(indexFile);

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
		IndexWriter w = new IndexWriter(index, config);

		
		
		
		
//		String lem = lemmatizer.lemmatize("[[British Standards]] CATEGORIES: 1901 establishments in the United Kingdom, British Standards, International Electrotechnical Commission, Standards organizations, Certification marks, Organisations based in the United Kingdom British Standards are the standards produced by BSI Group which is incorporated under a Royal Charter (and which is formally designated as the National Standards Body (NSB) for the UK). The BSI Group produces British Standards under the authority of the Charter, which lays down as one of the BSI's objectives to: Formally, as per the 2002 Memorandum of Understanding between the BSI and the United Kingdom Government, British Standards are defined as: Products and services which BSI certifies as having met the requirements of specific standards within designated schemes are awarded the Kitemark.[tpl]cite web|url=http://www.kitemark.com/about-kitemark/ |title=Kitemark.com |publisher=Kitemark.com |date= |accessdate=2012-04-03[/tpl] ==How British Standards are made== The BSI Group as a whole does not produce British Standards, as standards work within the BSI is decentralized.  The governing Board of BSI establishes a Standards Board.  The Standards Board does little apart from setting up Sector Boards (a Sector in BSI parlance being a field of standardization such as ICT, Quality, Agriculture, Manufacturing, or Fire).  Each Sector Board in turn constitutes several Technical Committees.  It is the Technical Committees that, formally, approve a British Standard, which is then presented to the Secretary of the supervisory Sector Board for endorsement of the fact that the Technical Committee has indeed completed a task for which it was constituted. ==The standards== The standards produced are titled British Standard XXXX-P:YYYY where XXXX is the number of the standard, P is the number of the part of the standard (where the standard is split into multiple parts) and YYYY is the year in which the standard came into effect.  BSI Group currently has over 27,000 active standards. Products are commonly specified as meeting a particular British Standard, and in general this can be done without any certification or independent testing. The standard simply provides a shorthand way of claiming that certain specifications are met, while encouraging manufacturers to adhere to a common method for such a specification. The Kitemark can be used to indicate certification by BSI, but only where a Kitemark scheme has been set up around a particular standard. It is mainly applicable to safety and quality management standards. There is a common misunderstanding that Kitemarks are necessary to prove compliance with any BS standard, but in general it is neither desirable nor possible that every standard be 'policed' in this way. [ref][tpl]cite conference|booktitle=International Symposium on Plastics Testing and Standardization|volume=247|series=ASTM special technical publication|publisher=American Society for Testing Materials International|year=1959|title=Standardization in the United Kingdom|author=H.M. Glass G. Weston|pages=37–38[/tpl][tpl]cite book|chapter=Standards, Specifications, and codes of practice|author=J.M. Faller and M.H. Graham|title=Handbook of Electrical Installation Practice|editor=Geoffrey Stokes|edition=4th|publisher=Wiley-Blackwell|year=2003|isbn=0-632-06002-6|isbn=978-0-632-06002-3|pages=305–306[/tpl][tpl]cite book|title=The constitution of private governance: product standards in the regulation of integrating markets|volume=4|series=International studies in the theory of private law|author=Harm Schepel|publisher=Hart Publishing|year=2005|isbn=1-84113-487-2|isbn=978-1-84113-487-1|pages=121–124[/tpl][tpl]Cite journal|title=MEMORANDUM OF UNDERSTANDING BETWEEN THE UNITED KINGDOM GOVERNMENT AND THE BRITISH STANDARDS INSTITUTION IN RESPECT OF ITS ACTIVITIES AS THE UNITED KINGDOM'S NATIONAL STANDARDS BODY|publisher=United Kingdom Department for Business, Innovation, and Skills|year=2002|format=PDF|url=http://bis.gov.uk/files/file11950.pdf[/tpl]==External links==");
		
		// if an index doesnt already exists 
		// index the docs of files in specified directory
		if(useExistingIndex == 0)
			for(File f : FILE_DIR.listFiles())
			{
				System.out.println("Processing File : "+ f.toString());
				indexDocsInFile(f, w, lemmatizer);
			}

		//close index writer
		w.close();

		// the "content" arg specifies the default field to use
		// when no field is explicitly specified in the query.
		
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_40, new String[] {"categories", "content"}, analyzer, boostMap); //, boostMap );
		Query q = queryParser.parse(querystr);


		// search
		int hitsPerPage = 30;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);

		// CHANGED THE SIMILIARITY
		searcher.setSimilarity(new BM25Similarity());

		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// display results
		System.out.println("Found " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) {
			int docId = hits[i].doc;
			float score = hits[i].score;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("title") + "\t" + " Score: " + score);
		}

		// is no need to access the documents any more.
		reader.close();
		
		//clean up
//		for(File f: indexFile.listFiles())
//		{
//			Files.delete(f.toPath());
//		}
		
	}

	
	
	private static Analyzer createAnalyzer() {
		Analyzer analyzer = new Analyzer() {
			  @Override
			   protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				 
				  Version ver = Version.LUCENE_40;
			     
				 Tokenizer source = new StandardTokenizer(ver, reader);
			     TokenStream result = new StandardFilter(ver, source);
			     
			     result = new EnglishPossessiveFilter(ver, result);
			     result = new LowerCaseFilter(ver, result);
			     result = new StopFilter(ver, result, StandardAnalyzer.STOP_WORDS_SET);
			     result = new PorterStemFilter(result);
			     
			     return new TokenStreamComponents(source, result);
			   }
			 };
		return analyzer;
		
	}



	private static void indexDocsInFile(File file, IndexWriter w, StanfordLemmatizer lemmatizer) throws FileNotFoundException, IOException {
		
		BufferedReader br = new BufferedReader(new FileReader(file));

		TextField content = new TextField("","",Field.Store.NO);		//	Did this to prevent an extra comparison operation for each line
		TextField categories = new TextField("", "", Field.Store.NO);
		StringField title = new StringField("","",Field.Store.NO);
		
		String tempContent = "";
		String tempCategories = "";
		
		for(String line; (line = br.readLine())!=null;)
		{

			//if the line contains [[ it must be the title of a wiki article
			//store the prev doc and update the title
			if(line.startsWith("[[") && line.endsWith("]]"))
			{

				// INDEX THE PREV DOC
								
				//store the prev doc
				Document doc = new Document();
								
				categories = new TextField("categories", lemmatizer.lemmatize(tempCategories), Field.Store.YES);
				categories.setBoost(1.2f);	//categories act as tier 1
				
				content = new TextField("content", lemmatizer.lemmatize(tempContent), Field.Store.YES);				
				
				doc.add(title);
				doc.add(categories);
				doc.add(content);				
				
				//index prev doc
				w.addDocument(doc);
				
				
				
				// UPDATE TITLE AND AUX VARS

				title = new StringField("title", line.substring(2, line.length()-2), Field.Store.YES);
								
				tempContent = "";
				tempCategories = "";
			}
			
			else
			{
				
				if(line.startsWith("CATEGORIES:"))
					tempCategories += " " +line;
				
				else
					tempContent += " " + line;
			}
						
		}
		
//		System.out.println(tempContent);	
		
		br.close();
	}
	
}
