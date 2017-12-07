// static methods mainly for converting natural classes to/from strings

package edu.jhu.features;

import java.util.*;
import java.util.stream.*;

import edu.jhu.util.*;

public class NaturalClassUtil
{

// return a minimal feature representation of natural class
// on this projection (xxx only for non-complement classes!)
public static int[] minimalFeatures(Projection proj, NaturalClass x) {
    return minimalFeatures(proj, x.segs, x.ftrs);
}

public static int[] minimalFeatures(Projection proj, BitSet segs, int[] ftrs) {
    BitSet segs_reduced = null;
    int[] ftrs_reduced = Arrays.copyOf(ftrs, ftrs.length);
    int n = ftrs_reduced.length;
    for (int i=(n-1); i>=0; i--) {
        if (ftrs_reduced[i]==0) continue;
        ftrs_reduced[i] = 0;
        segs_reduced = denotation(proj, ftrs_reduced);
        if (!segs_reduced.equals(segs)) {
            ftrs_reduced[i] = ftrs[i];
        }
    }
    return ftrs_reduced;
}

// convert feature vector to segment set on specified projection
public static BitSet denotation(Projection proj, int[] ftrs) {
    return denotation(proj, ftrs, false);
}

// convert feature vector (possibly complemented) to segment set on specified projection
public static BitSet denotation(Projection proj, int[] ftrs, boolean complement) {
    Alphabet A       = proj.getAlphabet();
    SymbolTable syms = A.syms;
    BitSet segs      = new BitSet(A.nSegments);
    int[] ftrs_i     = null;
    for (int i=proj.segmentMask.nextSetBit(0); i!=-1; i=proj.segmentMask.nextSetBit(i+1)) {
        ftrs_i = proj.featureMatrix.get( syms.get(i) );
        if (FeatureUtil.subsumes(ftrs, ftrs_i))
            segs.set(i);
    }
    if (complement)
        segs.flip(0, segs.size());
    if (proj.segmentMask!=null)
        segs.and(proj.segmentMask);
    return segs;
}

// is natural class closed w.r.t. segment set?
// def. a natural class Y is `closed' (unblocked) with respect to a
// segment set segs iff, for every immed. dominating class X there 
// is at least one member of segs in the set (X-Y), 
// i.e., for all such X: (X-Y) \intersect segs \neq \emptyset
// Note that the differences (X-Y) are precomputed as Y.diffs
public static boolean closed(NaturalClass x, BitSet segs) {
    ArrayList<BitSet> diffs = x.diffs;
	if (diffs==null || diffs.isEmpty()) return true;
	for (BitSet diff_i : diffs) {
		if (!diff_i.intersects(segs)) {
			//System.out.println("natural class "+ x +" is not closed");
			//System.out.println("... because "+ diff_i +" has null intersection with "+ segs);
			return false;
		}
	}
	return true;
}

// is natural class nc1 a subset of nc2?
public static boolean subsetOf(NaturalClass nc1, NaturalClass nc2) {
	// empty set is a subset of all sets
	if (nc1.size==0) return true;
	// subset cannot be larger than superset
	if (nc1.size > nc2.size) return false;
	// a set is a subset of itself
	if (nc1.segs.equals(nc2.segs)) return true;
	// exhaustive check
	for (int i=nc1.segs.nextSetBit(0); i!=-1; i=nc1.segs.nextSetBit(i+1)) {
		if (!nc2.segs.get(i)) return false;
	}
	return true;
}

// convert natural class to string
public static String toString(Projection proj, NaturalClass x) {
    Alphabet A = proj.getAlphabet();

    // special case: ad-hoc class for arbitrary segment set
    if (x.ftrs==null) {
        String value = x.segs.stream()
            .mapToObj(i -> A.syms.get(i))
            .collect(Collectors.joining(",", "{", "}"));
        return value;
    }

    // general case
    return FeatureUtil.toString(A, x.ftrs, x.complement);
}

// convert sequence of natural classes to string
public static String toString(Projection proj, NaturalClass[] X) {
    String value = Arrays.stream(X)
        .map(x -> toString(proj, x))
        .collect(Collectors.joining());
    return value;
}

// convert string to natural class (inverse of toString())
public static NaturalClass fromString(Projection proj, String s) {
    Alphabet A = proj.getAlphabet();

    // special case: ad-hoc class for arbitrary segment set
	// todo: verify that all segments in the set are on the projection
    if (s.startsWith("{")) {
        BitSet segs = new BitSet(A.nSegments);
        s = s.replaceAll("[{} ]", "");
        for (String seg : s.split(",")) {
            segs.set(A.syms.get(seg));
        }
        return new NaturalClass(proj, segs, null, false);
    }
    
    // special case: regular expression with segment symbols
    if (s.startsWith("(")) {
        BitSet segs = new BitSet(A.nSegments);
        s = s.replaceAll("[() ]", "");
        for (String seg : s.split("[|]"))
            segs.set(A.syms.get(seg));
        return proj.naturalClasses.get(segs);
    }

    // general case
    int[] ftrs = FeatureUtil.fromString(A, s);
    boolean complement = s.startsWith("\\[^");
    BitSet segs = denotation(proj, ftrs, complement);
    if (proj.naturalClasses!=null) {
        return proj.naturalClasses.get(segs);
    }
    NaturalClass x = new NaturalClass(proj, segs, ftrs, complement);
    return new NaturalClass(proj, segs, ftrs, complement);
}

// convert string array to sequence of natural classes (inverse of toString())
public static NaturalClass[] fromString(Projection proj, String[] S) {
    int n = S.length;
    NaturalClass[] X = new NaturalClass[n];
    for (int i=0; i<n; i++) {
        X[i] = fromString(proj, S[i]);
    }
    return X;
}

// convert natural class to regular expression (stated over segment ids)
public static String toRegExp(NaturalClass x) {
    String value = x.segs.stream()
        .mapToObj(Integer::toString)
        .collect(Collectors.joining("|", "(", ")"));
    return value;
}

// convert sequence of natural classes to regular expression (stated over segment ids)
public static String toRegExp(NaturalClass[] X) {
    String value = Arrays.stream(X)
        .map(x -> toRegExp(x))
        .collect(Collectors.joining());
    return value;
}

// convert natural class to regular expression (stated over segment symbols)
public static String toSymRegExp(SymbolTable syms, NaturalClass x) {
    String value = x.segs.stream()
        .mapToObj(id -> syms.get(id))
        .collect(Collectors.joining("|", "(", ")"));
    return value;
}

// convert feature matrix to regular expression (stated over segment symbols)
public static String toSymRegExp(Projection proj, int[] ftrs) {
    BitSet segs = denotation(proj, ftrs);
    String value = segs.stream()
        .mapToObj(id -> proj.A.syms.get(id))
        .collect(Collectors.joining("|", "(", ")"));
    return value;
}

}
