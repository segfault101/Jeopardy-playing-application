import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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


public class WIkiIndexer {
	public static void main(String[] args) throws IOException, ParseException {

		File FILE_DIR = new File("D:\\Desktop\\IR docs\\wiki-subset-20140602\\");
//		System.out.println("Enter absolute file path:");
//		String FILE_PATH = System.console().readLine();
		
		String querystr =  "red hat"; //"\"from retrieval\"~4";
//		System.out.println("Enter query :");
//		String querystr = System.console().readLine();
		
		int useExistingIndex = 0;
		
		// Specify the analyzer for tokenizing text when indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
		
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
				indexDocsInFile(f, w);
		
		//close index writer
		w.close();

		// the "content" arg specifies the default field to use
		// when no field is explicitly specified in the query.
		Query q = new QueryParser(Version.LUCENE_40, "content", analyzer).parse(querystr);

		// search
		int hitsPerPage = 20;
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

		// reader can only be closed when there
		// is no need to access the documents any more.
		reader.close();
		
		//clean up
//		for(File f: indexFile.listFiles())
//		{
//			Files.delete(f.toPath());
//		}
		
	}
	

	
	
	private static void indexDocsInFile(File file, IndexWriter w) throws FileNotFoundException, IOException {
		
		BufferedReader br = new BufferedReader(new FileReader(file));

		TextField content = new TextField("","",Field.Store.NO);		//	Did this to prevent an extra comparison operation for each line
		StringField title = new StringField("","",Field.Store.NO);
		String tempContent = "";
		
		for(String line; (line = br.readLine())!=null;)
		{

			//if the line contains [[ it must be the title of a wiki article
			//store the prev doc and update the title
			if(line.contains("[["))
			{
				//store the prev doc
				Document doc = new Document();
				content = new TextField("content", tempContent, Field.Store.YES);				
				doc.add(title);
				doc.add(content);
				
				//index prev doc
				w.addDocument(doc);
				
				
				//update the title to the new doc's
				title = new StringField("title", line.substring(2, line.length()-2), Field.Store.YES);
								
				//empty the tempContent buffer
				tempContent = "";
				
			}
			
			else
			{
				//if the line doesn't contains '==' i.e. subsection titles or is not empty, store it 
				if(!line.contains("==") && !line.equals(""))
					tempContent += line;
			}
						
		}
		
//		System.out.println(tempContent);	
		
		br.close();
	}
	
}
