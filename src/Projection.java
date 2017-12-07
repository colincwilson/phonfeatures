package edu.jhu.features;

import java.util.*;
import java.util.stream.*;
import edu.jhu.util.*;

public class Projection
{

public int id                           = 0;     // id of this projection
public boolean isDefault                = false; // is this the default projection?
public String name                      = null;  // name of this projection
public Alphabet A                       = null;  // alphabet (same for all projections)
public BitSet segmentMask               = null;  // segmentMask.get(i)==true iff ith segment is on this projection
public boolean[] segmentMaskBoolean     = null;	 // segmentMask[i]==true iff ith segment is on this projection
public boolean anySegmentMasked         = false; // are any segments omitted from this projection?
public int minConLength                 = 0;     // min length of constraints allowed on this projection
public int maxConLength                 = 0;     // max length of constraints allowed on this projection
public int maxWordLength                = 0;     // max length of words indexed on this projection
public boolean complementClasses        = false; // are complement classes allowed on this projection?
public LinkedHashMap<String, int[]> featureMatrix = null;
                            // segments with features represented on this projection (other features zeroed out)

public NaturalClasses naturalClasses    = null; // natural classes on this projection
public Corpus corpus                    = null; // corpus (same for all projections)
public Corpus sample                    = null; // sample (same for all projections)
//public NaturalClassFilter ncFilter      = null; // disjunctive filter applied to natural classes on this projection   // xxx broken

public IntTrie corpusIndexer            = null; // sequence indexer for the corpus on this projection
public IntTrie sampleIndexer            = null; // sequence indexer for the current sample on this projection
public IntTrie conTrie                  = null; // possible constraints over natural classes on this projection

// global variables for projected forms
public int[] projectedForm              = new int[50];  // container for projected form
public int projectedLength              = 0;            // length of projected form

static int projectionCounter            = 0;
static int verbosity                    = 6;


public Projection() { }

// constructor for default projection without natural classes
public Projection(Alphabet A) {
    id                  = projectionCounter++;
    isDefault           = true;
    name                = "default";
    this.A              = A;
    segmentMask         = new BitSet(); segmentMask.set(0,A.nSegments);
    segmentMaskBoolean  = new boolean[A.nSegments];
    Arrays.fill(segmentMaskBoolean, true);
}

// constructor for default projection
public Projection(Alphabet A, boolean complementClasses) {
    id                      = projectionCounter++;
    isDefault               = true;
	name                    = "default";
    this.A                  = A;
	segmentMask             = new BitSet(); segmentMask.set(0,A.nSegments);
	segmentMaskBoolean      = new boolean[A.nSegments]; Arrays.fill(segmentMaskBoolean, true);
    this.complementClasses  = complementClasses;
    this.featureMatrix      = A.featureMatrix;
    naturalClasses          = new NaturalClasses(this);
}

// constructor for non-default projection
// format:  [name]<tab>[comma-delimited list of criterial feature specifications]\
//            <tab>[comma-delimited list of projected features]\
//           (<tab>[min constraint length])\
//            <tab>[max constraint length]\
//           (<tab>[comma-delimited list of filter feature specifications] xxx broken)
// alt.:    [name]<tab>[{ set of projected segments }]\
//            <tab>[comma-delimited list of projected features]\
//           (<tab>[min constraint length])\
//            <tab>[max constraint length]\
//           (<tab>[comma-delimited list of filter feature specifications] xxx broken))
// notes
// * to specify that all segments are on a projection, set the criterial feature specifications equal to 'any'
// * to specify that all features are projected, set the projected features equal to 'all'
// * min constraint length field is optional
// * max constraint length field is obligatory
// * filter feature specifications are optional xxx broken
//
public Projection(Alphabet A, LinkedList<String> descriptor, boolean complementClasses) {
    this(A, descriptor.toArray(new String[0]), complementClasses);
}

public Projection(Alphabet A, String[] descriptor, boolean complementClasses) {
	if (verbosity>5)
        System.out.println("constructing projection from descriptor: "+
        java.util.Arrays.toString(descriptor) +" ...");

    id = projectionCounter++;
    isDefault = false;
    name = descriptor[0];
    this.A = A;
	// name of this projection
    if (verbosity>5) System.out.println("\tprojection name: "+ name);

    // projected segments
    parseProjectedSegments(descriptor[1]);
    anySegmentMasked = (segmentMask.cardinality()<A.nSegments);
    segmentMaskBoolean = new boolean[A.nSegments];
    for (int i=segmentMask.nextSetBit(0); i!=-1; i=segmentMask.nextSetBit(i+1))
        segmentMaskBoolean[i] = true;
    if (verbosity>5) System.out.println("\tsegment mask: " +
        A.syms.ids.stream()
            .filter(i -> segmentMaskBoolean[i])
            .map(i -> A.syms.table.get(i))
            .collect(Collectors.joining("|", "(", ")"))
    );
	if (verbosity>5) System.out.println("\tall segments projected? "+ !anySegmentMasked);
    
    // projected features
    parseProjectedFeatures(descriptor[2]);

    // maximum and [optionally] minimum length of constraints on this projection
    if (descriptor.length<5) {
        minConLength = 1;
        maxConLength = Integer.parseInt(descriptor[3]);
    } else {
        minConLength = Integer.parseInt(descriptor[3]);
        maxConLength = Integer.parseInt(descriptor[4]);
    }
    
    // maximum length of words indexed on this projection
    maxWordLength = maxConLength;

    this.complementClasses  = complementClasses;
    this.naturalClasses     = new NaturalClasses(this);
}

// parse description of projected segments
// xxx simplify this and following projection-defining methods with streams
public void parseProjectedSegments(String projectedSegments) {
    segmentMask = new BitSet(A.nSegments);
    segmentMask.set(A.syms.wordBegin()); // boundary symbols are
    segmentMask.set(A.syms.wordEnd());   // always projected
    
    // description is "any": all segments are projected
    if (projectedSegments.equals("any")) {
        segmentMask.set(0, A.nSegments);
        return;
    }
    
    // explicit description of projected segment set
    if (projectedSegments.contains("{")) {
        String[] segs = projectedSegments.replaceAll("[{}]", "").replaceAll(" ", "").split(",");
        for (String seg : segs)
            segmentMask.set(A.syms.get(seg));
        return;
    }

    // feature description of projected segments
    int[] criterialFtrs = FeatureUtil.fromString(A, projectedSegments);
    if (verbosity>5) System.out.println("\tcriterial features: "+
        IntStream.range(0, A.nFeatures)
            .filter(i -> criterialFtrs[i]!=0)
            .mapToObj(i -> ((criterialFtrs[i]==1) ? "+" : "-") + A.featureNames.get(i))
            .collect(Collectors.joining(","))
    );
    for (String seg : A.featureMatrix.keySet()) {
        int[] ftrs = A.featureMatrix.get(seg);
        if (FeatureUtil.subsumes(criterialFtrs, ftrs))
            segmentMask.set(A.syms.get(seg));
    }
}

// parse description of projected features
public void parseProjectedFeatures(String projectedFeatures) {
    // get projected features
    boolean[] featureMask = new boolean[A.nFeatures];
    featureMask[0] = true;  // word-boundary feature
    if (projectedFeatures.equals("all")) {
        Arrays.fill(featureMask, true);
    } else {
        String[] ftrs = projectedFeatures.replaceAll(" *", "").split(",");
        for (String ftr : ftrs) {
            featureMask[A.featureNames.indexOf(ftr)] = true;
        }
    }
    if (verbosity>5) System.out.println("\tprojected features: "+
        IntStream.range(0, A.nFeatures)
            .filter(i -> featureMask[i])
            .mapToObj(i -> A.featureNames.get(i))
            .collect(Collectors.joining(","))
    );
    int nProjectedFtrs = (int) IntStream.range(0, A.nFeatures).filter(i -> featureMask[i]).count();
    if (verbosity>5) System.out.println("\tall features projected? "+ (nProjectedFtrs==A.nFeatures));

    // reduced feature matrix specific to this projection
    String seg = null;
    int[] ftrs = null;
    featureMatrix = new LinkedHashMap<String, int[]>();
    for (int i=0; i<A.nSegments; i++) {
        seg  = A.syms.get(i);
        if (!segmentMask.get(i)) {
            featureMatrix.put(seg, null);
            continue;
        }
        ftrs = A.featureMatrix.get(seg);
        ftrs = Arrays.copyOf(ftrs, ftrs.length);
        for (int j=0; j<A.nFeatures; j++)
            if (!featureMask[j]) ftrs[j] = 0;
        featureMatrix.put(seg, ftrs);
        
    }
}

// set maximum word length on this projection
public Projection setMaxWordLength(int l) {
    maxWordLength = l;
    return this;
}

// set the minimum constraint length on this projection
public Projection setMinConLength(int l) {
    minConLength = l;
    return this;
}

// set the maximum constraint length on this projection
public Projection setMaxConLength(int l) {
    maxConLength = l;
    return this;
}

// (re)set the corpus on this projection and index its substrings
// note: only substrings up to length maxConLength+2 are indexed in the corpus (only
// these substrings are needed to evaluate possible constraints)
public Projection setCorpus(Corpus corpus) {
    this.corpus = corpus;
    if (corpus==null) return this;

    int form_id = 0;
    int[] enform = null;
    corpusIndexer = new IntTrie(maxConLength+2);
    for (String form : corpus.data.keySet()) {
        enform = corpus.endata.get(form);
        project(enform);
        corpusIndexer.update(projectedForm, projectedLength, form_id, 1);
        form_id++;
    }

    return this;
}

// (re)set sample on this projection and index its substrings
// note: only substrings up to length maxConLength+2 are indexed in the sample (only
// these substrings are needed to evaluate possible constraints)
// xxx consolidate with setCorpus
public Projection setSample(Corpus sample) {
    this.sample = sample;
    if (sample==null) return this;
    
    int form_id = 0;
    int[] enform = null;
    sampleIndexer = new IntTrie(maxConLength+2);
    for (String form : sample.data.keySet()) {
        enform = sample.endata.get(form);
        project(enform);
        sampleIndexer.update(projectedForm, projectedLength, form_id, 1);
        form_id++;
    }

    return this;
}

// determine projected representation of form y
// (sets global variables projectedForm and projectedLength)
public void project(int[] y) {
    projectedLength = 0;
    for (int yi : y) {
        if (!segmentMask.get(yi)) continue;
        projectedForm[projectedLength++] = yi;
    }
    if (verbosity>9) System.out.println("projection of "+ java.util.Arrays.toString(y) +" is "+ java.util.Arrays.toString(projectedForm) +" ("+ projectedLength +")");
}

public Alphabet getAlphabet() { return A; }

public String toString() {
    StringBuffer val = new StringBuffer();
    val.append("projection: "); val.append(name); val.append("\n");
    val.append(A.syms.toString());
    if (naturalClasses!=null)
        val.append(naturalClasses.toString());
    return val.toString();
}

}
