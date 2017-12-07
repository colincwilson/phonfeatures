/* read corpus, possibly with frequency annotations and storing the data items, 
possibly adding word boundary symbols to each form

Format: All non-empty lines of the corpus must have the following form:
		<form><tab><frequency>(...)
where
	<form> is a sequence of segments separated by single spaces, 
	<frequency> is a single integer
	(...) is an optional sequence of tab-delimited annotations, ignored by this class

Empty lines are silently skipped.
*/

package edu.jhu.features;

import java.io.*;
import java.util.*;
import com.infomata.data.*;
import edu.jhu.util.*;

public class CorpusReader
{

static String encoding = "UTF-8"; // "8859_1"   // corpus file encoding
static int MAXIMUM_WORD_SIZE = 100;

static int verbosity = 0;

public static Corpus apply(String filename, Alphabet A, boolean addWordBoundaries) throws Exception {
    Corpus corpus = new Corpus(A, addWordBoundaries);
    
    DataFile reader = DataFile.createReader(encoding);
    reader.setDataFormat(new TabFormat());
    try {
        reader.open(new File(filename));
        for (DataRow row=reader.next(); row!=null; row=reader.next()) {
            if (row.size()==0) continue;
            String form = row.getString(0);
            int freq = 1;
            try {
                freq = Integer.parseInt(row.getString(1));
            } catch(Exception e) { }
            corpus.update(form, freq);
        }
    } finally { reader.close(); }

    return corpus;
}

// commandline access
public static void main(String[] args) throws Exception {
    Alphabet A = FeatureMatrixReader.apply(args[0]);
    A.syms.setWordBegin("<#");
    A.syms.setWordEnd("#>");
    Corpus corpus = apply(args[1], A, true);

    Counter freq = null;
    int[] enform = null;
    for (String form : corpus.data.keySet()) {
        freq = corpus.data.get(form);
        enform = corpus.endata.get(form);
        System.out.println(form +" -> "+ Arrays.toString(enform) +" / "+ freq);
    }
    System.out.println("corpus length distrib: "+ corpus.lengthDistrib);
}

}
