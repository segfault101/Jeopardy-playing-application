import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class WIkiIndexer {
	public static void main(String[] args) throws IOException, ParseException {

		System.out.println("Important! When you want to turn on and off stemming or lemmatization or both, delete the \"tmp\" folder in the current directory before running the program.");
		System.out.println("Note: This is not required when changing other settings like SIMILIARITY and MODE and NUMBER_OF_RESULTS ");

		//TODO specify wiki directory
		System.out.println("\nEnter absolute file path of the wiki-subset-20140602 folder : ");
		String FILE_PATH = System.console().readLine();
		File FILE_DIR = new File(FILE_PATH); //new File(".\\wiki-subset-20140602\\");

		File indexFileFolder = new File("./tmp/index"); 

//		System.out.println("Enter absolute file path:");
//		String FILE_PATH = System.console().readLine();

		int ENABLE_BENCHMARK_MODE = 0;	// default is 0
		System.out.println("Choose Mode: 1-Benchmanrk  2-Retrieval : ");
		String m = System.console().readLine();
		if(Integer.parseInt(m) == 1)
			ENABLE_BENCHMARK_MODE = 1;
		else if (Integer.parseInt(m) == 2)
			ENABLE_BENCHMARK_MODE = 0;


		int ENABLE_STEMMING =1;			// default is on
		System.out.println("Stermming: 1-ON 2-OFF : ");
		String s = System.console().readLine();
		if(Integer.parseInt(s) == 1)
			ENABLE_STEMMING = 1;
		else if (Integer.parseInt(s) == 2)
			ENABLE_STEMMING = 0;


		int ENABLE_LEMMATIZATION =0;	//default is off
		System.out.println("Lemmatization: 1-ON 2-OFF : ");
		String l = System.console().readLine();
		if(Integer.parseInt(l) == 1)
			ENABLE_LEMMATIZATION = 1;
		else if (Integer.parseInt(l) == 2)
			ENABLE_LEMMATIZATION = 0;


		int ENABLE_BM25 =0;				//default is off
		System.out.println("Change Similarity to BM25: 1-YES 2-NO : ");
		String b = System.console().readLine();
		if(Integer.parseInt(b) == 1)
			ENABLE_BM25 = 1;
		else if (Integer.parseInt(b) == 2)
			ENABLE_BM25 = 0;
		
	
		
		int NUMBER_OF_RESULTS_FOR_RETRIEVAL = 1;	//default 1
		System.out.println("Number of results to retrieve: ");
		String n = System.console().readLine();
		NUMBER_OF_RESULTS_FOR_RETRIEVAL = Integer.parseInt(n);
		
		/*
		 * CREATE COMPONENTS
		 */
		//create lemmatizer
		StanfordLemmatizer lemmatizer = new StanfordLemmatizer();
		
		Analyzer analyzer;

		// Specify the analyzer for tokenizing text when indexing and searching
		if(ENABLE_STEMMING == 1)
			analyzer = createStemAnalyzer();//new EnglishAnalyzer(Version.LUCENE_40);
		else
			analyzer = createNoStemAnalyzer();

		//create reader
		IndexReader reader = null; 
		
		//create searcher;
		IndexSearcher searcher;
		
		//flag
		int useExistingIndex = 0;				
				
		//check if index already exists and set flag
		if(indexFileFolder.list() != null)
			useExistingIndex = 1;
		
		// load the index or create it if it doesn't exist
		FSDirectory index = FSDirectory.open(indexFileFolder);

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
		IndexWriter w = new IndexWriter(index, config);
	
		
		
		/*
		 * Begin Selected Mode
		 */
		
		if(ENABLE_BENCHMARK_MODE == 1)
			beginBenchmarkMode(FILE_DIR, ENABLE_LEMMATIZATION, ENABLE_BM25, lemmatizer, analyzer, reader,
					useExistingIndex, index, w);
		else
			beginRetrievalMode(FILE_DIR, ENABLE_LEMMATIZATION, ENABLE_BM25, lemmatizer, analyzer, reader,
					useExistingIndex, index, w, NUMBER_OF_RESULTS_FOR_RETRIEVAL);
		

	}

	private static void beginBenchmarkMode(File FILE_DIR, int ENABLE_LEMMATIZATION, int ENABLE_BM25,
			StanfordLemmatizer lemmatizer, Analyzer analyzer, IndexReader reader, int useExistingIndex,
			FSDirectory index, IndexWriter w)
					throws IOException, FileNotFoundException, ParseException {

		//to count the hits
		int hitCount = 0;
		
		double R_Prec = 0.0d;
		
		//TODO read queries from the SPECIFIED file
		File queries;
		System.out.println("Enter absolute path of the queries' text file : ");
		String qf = System.console().readLine();
		queries = new File(qf);
		
		BufferedReader queriesFileReader = new BufferedReader(new FileReader(queries));
		
		IndexSearcher searcher;
		String clue;
		String category;
		List<String> answers;
		String querystr;
		String line = new String();
		while((line = queriesFileReader.readLine())!=null)
		{
			if(!line.equals(""))
			{			
				category = line.replaceAll("[^A-Za-z ]+", " ");
				clue = queriesFileReader.readLine().replaceAll("[^A-Za-z ]+", " ");
				answers = Arrays.asList(queriesFileReader.readLine().split("\\|"));

				//lemmatize the parts of query
				if(ENABLE_LEMMATIZATION == 1)
				{					
					clue = lemmatizer.lemmatize(clue);
					category = lemmatizer.lemmatize(category);
				}

				//concat to form query string
				querystr = clue + " " + category ;


				//construct a term to boost map to use with multifield query parser
				HashMap<String, Float> boostMap = new HashMap<String, Float>();
				
				boostMap.put("categories", 7.5f);	//best 7.5 nl-ns
				boostMap.put("content", 17.5f);		//best 17.5 nl-ns


				// if an index doesnt already exists 
				// index the docs of files in specified directory
				if(useExistingIndex == 0)
					for(File f : FILE_DIR.listFiles())
					{
						System.out.println("Processing File : "+ f.toString());
						indexDocsInFile(f, w, lemmatizer, ENABLE_LEMMATIZATION);
						useExistingIndex = 1;
					}			
				//close index writer
				w.close();

				// the "content" arg specifies the default field to use
				// when no field is explicitly specified in the query.

				MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_40, new String[] {"categories", "content"}, analyzer, boostMap);
				Query q = queryParser.parse(querystr);


				// search
				int HITS_PER_PAGE = 1;	//best 5
				reader = DirectoryReader.open(index);
				searcher = new IndexSearcher(reader);

				// CHANGED THE SIMILIARITY
				if(ENABLE_BM25 == 1)
					searcher.setSimilarity(new BM25Similarity());

				TopScoreDocCollector collector = TopScoreDocCollector.create(HITS_PER_PAGE, true);
				searcher.search(q, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;

				//CALC : P@1				
				for(int i=0;i<hits.length;++i) 
				{
					int docId = hits[i].doc;
					float score = hits[i].score;
					Document d = searcher.doc(docId);

					for(String answer: answers)
						if(d.get("title").equalsIgnoreCase(answer))
						{
							hitCount++;					
							break;
						}
				}
				
				
				collector = TopScoreDocCollector.create(answers.size(), true);
				searcher.search(q, collector);
				hits = collector.topDocs().scoreDocs;

				//CALC : R_Prec				
				for(int i=0;i<answers.size();++i)
				{
					int docId = hits[i].doc;
					float score = hits[i].score;
					Document d = searcher.doc(docId);
					
					int localCount = 0;

					for(String answer: answers)
						if(d.get("title").equalsIgnoreCase(answer))
						{
							localCount++;					
							break;
						}
					
					R_Prec += (double)localCount/(double)answers.size();
					
				}
				
				
			}
			

		}
		
		System.out.println(/*"The answer: " + answer + */"P@1 :" + hitCount /*" out of 100 " + "\t Score : " + score */ );
		System.out.println(/*"The answer: " + answer + */"R_Precision :" + R_Prec /*" out of 100 " + "\t Score : " + score */ );
		
		reader.close();
	}



	private static void beginRetrievalMode(File FILE_DIR, int ENABLE_LEMMATIZATION, int ENABLE_BM25,
			StanfordLemmatizer lemmatizer, Analyzer analyzer, IndexReader reader, int useExistingIndex,
			FSDirectory index, IndexWriter w, int NUMBER_OF_RESULTS_FOR_RETRIEVAL) 
					throws IOException, FileNotFoundException, ParseException {

		

//		float score = 0.0f;
				
		IndexSearcher searcher;

		//take input from console

		String category; // = "GOLDEN GLOBE WINNERS";
		System.out.println("Enter the category : ");
		category = System.console().readLine();

		
		String clue; 	//= "In 2010: As Sherlock Holmes on film";
		System.out.println("Enter the clue : ");
		clue = System.console().readLine();
		
		
		
		
		String querystr;
		String line = new String();

		category = line.replaceAll("[^A-Za-z ]+", " ");
		clue = clue.replaceAll("[^A-Za-z ]+", " ");
		//			answers = Arrays.asList(queriesFileReader.readLine().split("\\|"));

		//lemmatize the parts of query
		if(ENABLE_LEMMATIZATION == 1)
		{					
			clue = lemmatizer.lemmatize(clue);
			category = lemmatizer.lemmatize(category);
		}

		//concat to form query string
		querystr = clue + " " + category ;


		//construct a term to boost map to use with multifield query parser
		HashMap<String, Float> boostMap = new HashMap<String, Float>();

		boostMap.put("categories", 7.5f);	//best 7.5 nl-ns
		boostMap.put("content", 17.5f);		//best 17.5 nl-ns


		// if an index doesnt already exists 
		// index the docs of files in specified directory
		if(useExistingIndex == 0)
			for(File f : FILE_DIR.listFiles())
			{
				System.out.println("Processing File : "+ f.toString());
				indexDocsInFile(f, w, lemmatizer, ENABLE_LEMMATIZATION);
				useExistingIndex = 1;
			}			
		//close index writer
		w.close();

		// the "content" arg specifies the default field to use
		// when no field is explicitly specified in the query.

		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_40, new String[] {"categories", "content"}, analyzer, boostMap);
		Query q = queryParser.parse(querystr);


		// search
		int hitsPerPage = NUMBER_OF_RESULTS_FOR_RETRIEVAL;	//best 5
		reader = DirectoryReader.open(index);
		searcher = new IndexSearcher(reader);

		// CHANGED THE SIMILIARITY
		if(ENABLE_BM25 == 1)
			searcher.setSimilarity(new BM25Similarity());

		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		System.out.println("Found " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) 
		{
			int docId = hits[i].doc;
			float score = hits[i].score;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("title") + "\t" + " Score: " + score);
		}

//		System.out.println(/*"The answer: " + answer + */" Hit count : " + hitCount +" out of 100 " + "\t Score : " + score );
	
	reader.close();
		
		
	}
	
	
	private static Analyzer createNoStemAnalyzer() {
		Analyzer analyzer = new Analyzer() {
			  @Override
			   protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
				 
				  Version ver = Version.LUCENE_40;
			     
				 Tokenizer source = new StandardTokenizer(ver, reader);
			     TokenStream result = new StandardFilter(ver, source);
			     
			     result = new EnglishPossessiveFilter(ver, result);
			     result = new LowerCaseFilter(ver, result);
			     result = new StopFilter(ver, result, StandardAnalyzer.STOP_WORDS_SET);
			     			     
			     return new TokenStreamComponents(source, result);
			   }
			 };
		return analyzer;
		
	}
	
	private static Analyzer createStemAnalyzer() {
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



	private static void indexDocsInFile(File file, IndexWriter w, StanfordLemmatizer lemmatizer, int ENABLE_LEMMATIZATION) throws FileNotFoundException, IOException {
		
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
				
				
				if(ENABLE_LEMMATIZATION == 1)
					categories = new TextField("categories", lemmatizer.lemmatize(tempCategories), Field.Store.YES);
				else
					categories = new TextField("categories", tempCategories, Field.Store.YES);
				
				categories.setBoost(1.2f);
				
				
				if(ENABLE_LEMMATIZATION == 1)
					content = new TextField("content", lemmatizer.lemmatize(tempContent), Field.Store.YES);				
				else
					content = new TextField("content", tempContent, Field.Store.YES);				
				
				
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
				{
					line.replaceAll("#REDIRECT", " ");					
					tempContent += " " + line;
					
				}
			}
						
		}
		
		br.close();
	}
	
}
