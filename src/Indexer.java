/* **************************************************************************************************************************************
 * Function: Make a file searchable and index/deindex it on a Solr core.
 * File types: Non-searchable PDFs (e.g., scanned PDFs) and other formats.
 *
 * Possible extensions are:
 *     pdf txt htm html csv xml json doc docx ppt pptx xls xlsx odt odp ods odg ott otp ots rtf log
 *     gif bmp png jpeg jpg tif tiff dot epub vsd msg unknownExtension
 * 
 * INPUT:
 * There are 4 strings (note: the first 3 strings are passed directly from the bash script 'runJava.sh'):
 *     absolutePath:    absolute path (from the guest's point of view) of the file to be indexed or deindexed.
 *     core:            name of the Solr core on which to index the files.
 *     indexingAction:  string that can take one of the following two values:
 *                          "index"    if I want to index the file 
 *                          "deindex"  if I want to deindex the file 
 *     queryTerm:       specifies the string to search for to determine if a PDF file is already searchable or not
 *                      (typically it is advisable to leave absolutePathit unchanged)
 *
 * OUTPUT:
 * For PDF files (OUTPUT part 1) there are two possible outputs:
 *     "The string 'Font' was found 'n' times [[where 'n' is the number of occurrences of the string "Font"]]
 *     The file /absolute/path/of/a/folder/fileName.pdf is already searchable"
 * or
 *     "The string 'Font' was found 0 times
 *     The file /absolute/path/of/a/folder/fileName.pdf is not yet searchable"
 *
 * Every PDF file consisting only of images will be made searchable.
 * Every file - whether already searchable PDF or just made searchable PDF or other extensions - will be indexed on a Solr core.
 *
 * Finally, it will be printed (OUTPUT part 2):
 *     "The file /absolute/path/of/a/folder/aFile, BEFORE making the potential pdf searchable, was a nameExtension file"
 * where nameExtension can be:
 *     pdf txt htm html csv xml json doc docx ppt pptx xls xlsx odt odp ods odg ott otp ots rtf log
 *     gif bmp png jpeg jpg tif tiff dot epub vsd msg unknownExtension
***************************************************************************************************************************************/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Indexer {

	// The string 'env' can be known by running the following command from the guest terminal: echo "$PATH"
	static String[] env = {"PATH=/usr/lib64/qt-3.3/bin:/usr/lib/jvm/java/bin:/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/usr/hdp/current/falcon-client/bin:/usr/hdp/current/hadoop-mapreduce-historyserver/bin:/usr/hdp/current/oozie-client/bin:/usr/hdp/current/falcon-server/bin:/usr/hdp/current/hadoop-yarn-client/bin:/usr/hdp/current/oozie-server/bin:/usr/hdp/current/flume-client/bin:/usr/hdp/current/hadoop-yarn-nodemanager/bin:/usr/hdp/current/pig-client/bin:/usr/hdp/current/flume-server/bin:/usr/hdp/current/hadoop-yarn-resourcemanager/bin:/usr/hdp/current/slider-client/bin:/usr/hdp/current/hadoop-client/bin:/usr/hdp/current/hadoop-yarn-timelineserver/bin:/usr/hdp/current/sqoop-client/bin:/usr/hdp/current/hadoop-hdfs-client/bin:/usr/hdp/current/hbase-client/bin:/usr/hdp/current/sqoop-server/bin:/usr/hdp/current/hadoop-hdfs-datanode/bin:/usr/hdp/current/hbase-master/bin:/usr/hdp/current/storm-client/bin:/usr/hdp/current/hadoop-hdfs-journalnode/bin:/usr/hdp/current/hbase-regionserver/bin:/usr/hdp/current/storm-nimbus/bin:/usr/hdp/current/hadoop-hdfs-namenode/bin:/usr/hdp/current/hive-client/bin:/usr/hdp/current/storm-supervisor/bin:/usr/hdp/current/hadoop-hdfs-nfs3/bin:/usr/hdp/current/hive-metastore/bin:/usr/hdp/current/zookeeper-client/bin:/usr/hdp/current/hadoop-hdfs-portmap/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/current/zookeeper-server/bin:/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin:/usr/hdp/current/hive-webhcat/bin:/usr/hdp/current/hadoop-mapreduce-client/bin:/usr/hdp/current/knox-server/bin:/usr/hdp/current/hadoop-client/sbin:/usr/hdp/current/hadoop-hdfs-nfs3/sbin:/usr/hdp/current/hadoop-yarn-client/sbin:/usr/hdp/current/hadoop-hdfs-client/sbin:/usr/hdp/current/hadoop-hdfs-portmap/sbin:/usr/hdp/current/hadoop-yarn-nodemanager/sbin:/usr/hdp/current/hadoop-hdfs-datanode/sbin:/usr/hdp/current/hadoop-hdfs-secondarynamenode/sbin:/usr/hdp/current/hadoop-yarn-resourcemanager/sbin:/usr/hdp/current/hadoop-hdfs-journalnode/sbin:/usr/hdp/current/hadoop-mapreduce-client/sbin:/usr/hdp/current/hadoop-yarn-timelineserver/sbin:/usr/hdp/current/hadoop-hdfs-namenode/sbin:/usr/hdp/current/hadoop-mapreduce-historyserver/sbin:/usr/hdp/current/hive-webhcat/sbin:/root/bin"};	// ************
	static String scriptPath = "/media/sf_VirtualBoxShared/indexer.sh";  // This is the absolute path (seen by the guest) of the folder that contains
	                                                                     // the bash script (which helps this program) 'indexer.sh'
	static String isScanned;

	// List of counters for various possible extensions
	static int indexedPdf_counter = 0;
	static int unindexedPdf_counter = 0;
	static int txt_counter = 0;
	static int contatoreHtm = 0;
	static int html_counter = 0;
	static int csv_counter = 0;
	static int xml_counter = 0;
	static int json_counter = 0;
	static int doc_counter = 0;
	static int docx_counter = 0;
	static int ppt_counter = 0;
	static int pptx_counter = 0;
	static int xls_counter = 0;
	static int xls_counterx = 0;
	static int odt_counter = 0;
	static int odp_counter = 0;
	static int ods_counter = 0;
	static int odg_counter = 0;
	static int ott_counter = 0;
	static int otp_counter = 0;
	static int ots_counter = 0;
	static int rtf_counter = 0;
	static int log_counter = 0;
	static int gif_counter = 0;
	static int bmp_counter = 0;
	static int png_counter = 0;
	static int jpeg_counter = 0;
	static int jpg_counter = 0;
	static int tiff_counter = 0;
	static int tif_counter = 0;
	static int dot_counter = 0;
	static int epub_counter = 0;
	static int vsd_counter = 0;
	static int msg_counter = 0;
	static int unknownExtension_counter = 0;

	public static void main(String[] args) throws IOException {

		// ****************************************************************************************************
		// Specify the INPUT (nb: the first 3 strings are passed directly from the 'runJava.sh' bash script)
		String absolutePath = args[0];   // absolute path (seen from the guest) of the file to be indexed
		String core = args[1];           // name of the Solr core in which to index files
		String indexingAction = args[2]; // string that can take one of the two following values:
		                                 //     "index" if I want to index the file
		                                 //     "deindex" if I want to deindex the file
		String queryTerm = "Font";       // specify the string to search for to determine if a PDF file is
		                                 // already searchable or not
		                                 // *** (typically, it is advisable to leave it unchanged) ***
		// ****************************************************************************************************
		
		try {
			String extension = "";
			int index1 = absolutePath.lastIndexOf('.');
			int index2 = Math.max(absolutePath.lastIndexOf('/'), absolutePath.lastIndexOf('\\'));
			if (index1 > index2) {
				extension = absolutePath.substring(index1+1); // obtain the file extension
			}
			
			// If the file extension is .pdf and the indexingAction is 'index', then proceed with indexing the file
			if (extension.equalsIgnoreCase("pdf") && indexingAction.equals("index")) {
				int queryTerm_length = queryTerm.length();
				int queryTerm_counter = 0; // Keep count of the number of occurrences of the queryTerm string
				BufferedReader br = null;
				try {
					br = new BufferedReader(new InputStreamReader(new FileInputStream(absolutePath), "UTF8"));
					// (Note: other encodings, such as "Cp1252", can also be used instead of "UTF8")
					String row;
					while ((row = br.readLine()) != null) { // Read the file line by line
						if (row.contains(queryTerm)) { // Check if the queryTerm string occurs at least once in the line
							String[] RowQueryTerm = row.split("\\s+"); // Split the line into an array of words separated by white space
							for (String queryTermTemp : RowQueryTerm) { // Iterate over the array of words for the queryTerm string
								int queryTerm_lengthTemp = queryTermTemp.length();
								if (queryTerm_lengthTemp >= queryTerm_length) { // If the word is at least as long as the queryTerm string...
									// ... then for each possible substring of queryTermTemp that is the same length as queryTerm...
									// ... check if the substring equals queryTerm (and if so, increment queryTerm_counter by 1)
									for (int i = 0; i <= queryTerm_lengthTemp - queryTerm_length; i++) {
										String queryTermTempSubstring = queryTermTemp.substring(i, queryTerm_length + i).trim();
										if (queryTermTempSubstring.equals(queryTerm)) { // Note that this comparison is case-sensitive
											queryTerm_counter++;
										}
									}
								}
							}
						}
					}

					// Print the output (part 1)
					System.out.println("\nThe string '" + queryTerm + "' was found " + queryTerm_counter + " times");
					System.out.println((queryTerm_counter > 0) ? "The file " + absolutePath + " is already searchable" : "The file " + absolutePath + " is not yet searchable");

					if (queryTerm_counter==0)
					{
						unindexedPdf_counter++;
						isScanned = "1";
					}
					else
					{
						indexedPdf_counter++;
						isScanned = "0";
					}

				}catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (br != null) {
						try {
							br.close();		// close BufferedReader
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			} else if (extension.equalsIgnoreCase("txt")) {
				isScanned = "0";
				txt_counter++;		
			} else if (extension.equalsIgnoreCase("htm")) {
				isScanned = "0";
				contatoreHtm++;
			} else if (extension.equalsIgnoreCase("html")) {
				isScanned = "0";
				html_counter++;
			} else if (extension.equalsIgnoreCase("csv")) {
				isScanned = "0";
				csv_counter++;
			} else if (extension.equalsIgnoreCase("xml")) {
				isScanned = "0";
				xml_counter++;
			} else if (extension.equalsIgnoreCase("json")) {
				isScanned = "0";
				json_counter++;
			} else if (extension.equalsIgnoreCase("doc")) {
				isScanned = "0";
				doc_counter++;
			} else if (extension.equalsIgnoreCase("docx")) {
				isScanned = "0";
				docx_counter++;
			} else if (extension.equalsIgnoreCase("ppt")) {
				isScanned = "0";
				ppt_counter++;
			} else if (extension.equalsIgnoreCase("pptx")) {
				isScanned = "0";
				pptx_counter++;
			} else if (extension.equalsIgnoreCase("xls")) {
				isScanned = "0";
				xls_counter++;
			} else if (extension.equalsIgnoreCase("xlsx")) {
				isScanned = "0";
				xls_counterx++;
			} else if (extension.equalsIgnoreCase("odt")) {
				isScanned = "0";
				odt_counter++;
			} else if (extension.equalsIgnoreCase("odp")) {
				isScanned = "0";
				odp_counter++;
			} else if (extension.equalsIgnoreCase("ods")) {
				isScanned = "0";
				ods_counter++;
			} else if (extension.equalsIgnoreCase("odg")) {
				isScanned = "0";
				odg_counter++;
			} else if (extension.equalsIgnoreCase("ott")) {
				isScanned = "0";
				ott_counter++;
			} else if (extension.equalsIgnoreCase("otp")) {
				isScanned = "0";
				otp_counter++;
			} else if (extension.equalsIgnoreCase("ots")) {
				isScanned = "0";
				ots_counter++;
			} else if (extension.equalsIgnoreCase("rtf")) {
				isScanned = "0";
				rtf_counter++;
			} else if (extension.equalsIgnoreCase("log")) {
				isScanned = "0";
				log_counter++;
			} else if (extension.equalsIgnoreCase("gif")) {
				isScanned = "0";
				gif_counter++;
			} else if (extension.equalsIgnoreCase("bmp")) {
				isScanned = "0";
				bmp_counter++;
			} else if (extension.equalsIgnoreCase("png")) {
				isScanned = "0";
				png_counter++;
			} else if (extension.equalsIgnoreCase("jpeg")) {
				isScanned = "0";
				jpeg_counter++;
			} else if (extension.equalsIgnoreCase("jpg")) {
				isScanned = "0";
				jpg_counter++;
			} else if (extension.equalsIgnoreCase("tiff")) {
				isScanned = "0";
				tiff_counter++;
			} else if (extension.equalsIgnoreCase("tif")) {
				isScanned = "0";
				tif_counter++;
			} else if (extension.equalsIgnoreCase("dot")) {
				isScanned = "0";
				dot_counter++;
			} else if (extension.equalsIgnoreCase("epub")) {
				isScanned = "0";
				epub_counter++;
			} else if (extension.equalsIgnoreCase("vsd")) {
				isScanned = "0";
				vsd_counter++;
			} else if (extension.equalsIgnoreCase("msg")) {
				isScanned = "0";
				msg_counter++;
			} else {
				isScanned = "0";
				unknownExtension_counter++; 
			}

			String cmd[] = new String[] {scriptPath, absolutePath, isScanned, core, indexingAction};
			try {
				Process p1 = Runtime.getRuntime().exec(cmd, env);
				String s = null;

				BufferedReader stdInputp1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
				while ((s = stdInputp1.readLine()) != null) {	// read the output of the bash script
					System.out.println(s);
				}
				BufferedReader stdErrorp1 = new BufferedReader(new InputStreamReader(p1.getErrorStream()));
				//System.out.println("Below is the (possible) error message of the program:\n"); // read any execution errors of the bash script
				while ((s = stdErrorp1.readLine()) != null) {
					System.out.println(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Print the output (part 2)
		if (indexingAction.equals("index")) {
			System.out.print("\n*** ");
			System.out.print("The file "+absolutePath+", BEFORE making the potential pdf searchable, was a ");
			if (indexedPdf_counter>0)
				System.out.print("pdf already indexed ");
			if (unindexedPdf_counter>0)
				System.out.print("NOT yet indexed pdf");
			if (txt_counter>0)
				System.out.print("txt");
			if (html_counter>0)
				System.out.print("html");
			if (csv_counter>0)
				System.out.print("csv");
			if (xml_counter>0)
				System.out.print("xml");
			if (json_counter>0)
				System.out.print("json");
			if (doc_counter>0)
				System.out.print("doc");
			if (docx_counter>0)
				System.out.print("docx");
			if (ppt_counter>0)
				System.out.print("ppt");
			if (pptx_counter>0)
				System.out.print("pptx");
			if (xls_counterx>0)
				System.out.print("xls");
			if (odt_counter>0)
				System.out.print("odt");
			if (odp_counter>0)
				System.out.print("odp");
			if (ods_counter>0)
				System.out.print("ods");
			if (odg_counter>0)
				System.out.print("odg");
			if (ott_counter>0)
				System.out.print("ott");
			if (otp_counter>0)
				System.out.print("otp");
			if (ots_counter>0)
				System.out.print("ots");
			if (rtf_counter>0)
				System.out.print("rtf");
			if (log_counter>0)
				System.out.print("log");
			if (gif_counter>0)
				System.out.print("gif");
			if (bmp_counter>0)
				System.out.print("bmp");
			if (png_counter>0)
				System.out.print("png");
			if (jpeg_counter>0)
				System.out.print("jpeg");
			if (jpg_counter>0)
				System.out.print("jpg");
			if (tiff_counter>0)
				System.out.print("tiff");
			if (tif_counter>0)
				System.out.print("tif");
			if (dot_counter>0)
				System.out.print("dot");
			if (epub_counter>0)
				System.out.print("epub");
			if (vsd_counter>0)
				System.out.print("vsd");
			if (msg_counter>0)
				System.out.print("msg");
			if (unknownExtension_counter>0)
				System.out.print("unknown extension");
			System.out.println(" ***");
		}
		System.out.println("\n\n\n\n");
	}
}
