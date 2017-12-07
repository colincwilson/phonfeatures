// container for symbols and (optional) feature matrix;
// methods for encoding/decoding string to/from int[]

package edu.jhu.features;

import java.util.*;
import java.util.stream.*;
import edu.jhu.util.*;

public class Alphabet
{

public LinkedHashMap<String, int[]> featureMatrix   = null;
public LinkedList<String> featureNames              = null;
public SymbolTable syms                             = null;
public int nSegments = 0;
public int nFeatures = 0;
public int verbosity = 0;

public Alphabet(SymbolTable syms) {
    this.syms = syms;
    nSegments = syms.size();
}

public Alphabet(LinkedHashMap<String,int[]> featureMatrix, LinkedList<String> featureNames) {
	this.featureMatrix  = featureMatrix;
	this.featureNames   = featureNames;
	nSegments           = featureMatrix.size();
	nFeatures           = featureNames.size();
	
	syms = new SymbolTable();
    for (String sym : featureMatrix.keySet()) {
        syms.add(sym);
    }
}

// encode space-delimited segment string as int[],
// adding word boundary symbols
public int[] encodeString(String s) {
    return encodeString(s, " ", true);
}

// encode space-delimited segment string as int[]
public int[] encodeString(String s, boolean addWordBoundaries) {
	return encodeString(s, " ", addWordBoundaries);
}

// encode delimited segment string as int[]
public int[] encodeString(String s, String sep, boolean addWordBoundaries) {
    if (addWordBoundaries)
        s = syms.wordBeginSym() +sep+ s +sep+ syms.wordEndSym();
	String[] elts = s.split(sep);
	int[] x = new int[elts.length];
	for (int i=0; i<elts.length; i++) {
		try {
	        x[i] = syms.get(elts[i]);
		} catch (Exception e) {
			System.out.println("\nError: unknown symbol in string "+ s);
			System.out.println("@ position "+ i +": __"+ elts[i] +"__\n");
			throw (e);
		}
	}
	return x;
}

/*
// encode delimited i/o string as int[][]
public int[][] encodeString(String s, String sep, String ioSep) {
	String[] elts = s.split(sep);
	int[] input = new int[elts.length];
    int[] output = new int[elts.length];
	for (int i=0; i<elts.length; i++) {
		String[] io = elts[i].split(ioSep);
		input[i] = encode(io[0]);
        output[i] = encode(io[1]);
	}
	return new int[][] {input,output};
}
*/

// decode int[] to space-delimited string,
// stripping word boundary symbols
public String decodeString(int[] x) {
    return decodeString(x, " ", true);
}

// decode int[] to space-delimited string
public String decodeString(int[] x, boolean stripWordBoundaries) {
    return decodeString(x, " ", stripWordBoundaries);
}

// decode int[] to delimited string
public String decodeString(int[] x, String sep, boolean stripWordBoundaries) {
    int start = (stripWordBoundaries) ? 1 : 0;
    int end   = (stripWordBoundaries) ? x.length-1 : x.length;
    String value = IntStream.range(start, end)
                    .mapToObj(i -> syms.get(x[i]))
                    .collect(Collectors.joining(sep));
    return value;
}

// commandline access, for example:
// java edu.jhu.features.Alphabet ~/Projects/UCLAPhonotacticLearner/Wargamay/WargamayFeatureChartNew.txt
public static void main(String[] args) throws Exception {
	Alphabet A = FeatureMatrixReader.apply(args[0]);
    System.out.println("features");
    System.out.println(A.featureNames);
    System.out.println("segments");
    for (String seg : A.featureMatrix.keySet())
        System.out.println(seg +" -> "+ Arrays.toString(A.featureMatrix.get(seg)));
}

}
