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
		

		br.close();
	}
	
}
