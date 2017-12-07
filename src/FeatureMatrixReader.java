package edu.jhu.features;

import java.io.*;
import java.util.*;
import com.infomata.data.*;

public class FeatureMatrixReader
{

static String PLUS      = "+";
static String MINUS     = "-";
static String ZERO      = "0";
static int verbosity    = 0;
static String enc = "UTF-8"; // "8859_1"   // feature file encoding

public static Alphabet apply(String filename) throws Exception {

DataFile read = DataFile.createReader(enc);
read.setDataFormat(new TabFormat());

LinkedList<String> featureNames = new LinkedList<String>();
LinkedHashMap<String,int[]> featureMatrix = new LinkedHashMap<String,int[]>();

try {
	read.open(new File(filename));
	boolean header  = true;
	int nFeatures   = 0;

	for (DataRow row = read.next(); row != null; row = read.next()) {
		// read header (feature names)
		if (header) {
			nFeatures   = row.size();
			String ftr  = null;
			for (int i=0; i<nFeatures; i++) {
				ftr = row.getString(i);
				if (ftr.equals("")) {
					continue;
				}
				featureNames.add(ftr);
			}
			header = false;
			continue;
		}
		nFeatures = featureNames.size();

        // read segment symbol and its features
        String segment = row.getString(0);
        int[] features = new int[nFeatures];
		String val      = null;
		for (int i=1; i<=nFeatures; i++) {
			val = row.getString(i);
			if (val.equals(PLUS)) {
				features[i-1] = 1;
			}
			else if (val.equals(MINUS)) {
				features[i-1] = -1;
			}
		}
		featureMatrix.put(segment, features);
	}

	read.close();
} catch (Exception e) { System.out.println(e); }

if (verbosity>5) {
	System.out.println(featureNames);
	System.out.println(featureMatrix.keySet());
    for (Map.Entry<String, int[]> x : featureMatrix.entrySet()) {
        System.out.println(x.getKey() +" -> "+ Arrays.toString(x.getValue()));
    }
}

return new Alphabet(featureMatrix, featureNames);
}


// commandline access
public static void main(String[] args) throws Exception {
	Alphabet A = FeatureMatrixReader.apply(args[0]);
    System.out.println(A);
}


}
