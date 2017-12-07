// container for natural class list / lattice / tree on a projection
// xxx todo: implement filtering of natural classes

package edu.jhu.features;

import java.util.*;
import java.util.stream.*;
import edu.jhu.util.*;

public class NaturalClasses
{

public BinaryRelation<NaturalClass> naturalClassLattice	= null;		// lattice of natural classes
public BinaryRelation<NaturalClass> naturalClassTree	= null;		// rooted tree of natural classes
public NaturalClass[] naturalClasses					= null;		// array of natural classes
public NaturalClass sigma                               = null;     // natural class containing all segments
public boolean[][] naturalClassReln                     = null;     // subset relation on natural classes
public int nNaturalClasses                              = 0;

int verbosity = 10;

public NaturalClasses(Projection proj) {
    // make concept lattice, apply transitive reduction
    BitSet segsInContext = (BitSet) proj.segmentMask.clone();
    BinaryRelation<Concept> conceptLattice =
        (new FormalConceptAnalysis()).galoisLattice(
            proj.featureMatrix,
            proj.segmentMask,
            segsInContext
        );
    conceptLattice = conceptLattice.transitiveReduction();

    // convert concept lattice to natural class lattice
    HashMap<Concept,NaturalClass> M = new HashMap<Concept,NaturalClass>();
    for (Concept x : conceptLattice.S) {
        BitSet segs = x.extent;
        int[] ftrs = NaturalClassUtil.minimalFeatures(proj, x.extent, x.intent);
        M.put(x, new NaturalClass(proj, segs, ftrs, false));
    }
    naturalClassLattice = new BinaryRelation<NaturalClass>();
    for (Concept x : conceptLattice.S) {
        for (Concept y : conceptLattice.get(x)) {
            NaturalClass cx = M.get(x);
            NaturalClass cy = M.get(y);
            naturalClassLattice.add(cx, cy);

            // set difference between class cy and
            // subset class cx (used for pruning)
            if (cy.diffs==null)
                cy.diffs = new ArrayList<BitSet>();
            BitSet xydiff = (BitSet) cx.segs.clone();
            xydiff.andNot(cy.segs);
            cy.diffs.add(xydiff);
        }
    }
    
    // convert natural class lattice to tree
    BinaryRelation<NaturalClass> naturalClassLatticeRev
        = new BinaryRelation<NaturalClass>();
    for (NaturalClass x : naturalClassLattice.S)
        for (NaturalClass y : naturalClassLattice.get(x))
            naturalClassLatticeRev.add(y,x);

    BinaryRelation<NaturalClass> naturalClassTree
        = new BinaryRelation<NaturalClass>();
    for (NaturalClass y : naturalClassLattice.S) {
        HashSet<NaturalClass> superClassesOf_y
            = naturalClassLatticeRev.get(y);
        if (superClassesOf_y.size()==0)
            continue;
        NaturalClass x = superClassesOf_y
            .stream().sorted().findFirst().get();
        naturalClassTree.add(x,y);
    }
    
    // make complement classes (which lie outside of lattice / tree)
    HashSet<NaturalClass> S
        = new HashSet<NaturalClass>(naturalClassTree.S);
    if (proj.complementClasses) {
        BitSet sigma = proj.segmentMask;
        int wordBegin = proj.getAlphabet().syms.wordBegin();
        int wordEnd = proj.getAlphabet().syms.wordEnd();

        for (NaturalClass x : naturalClassTree.S) {
            BitSet segs = (BitSet) sigma.clone();
            segs.andNot(x.segs); segs.clear(wordBegin); segs.clear(wordEnd);
            if (segs.cardinality()==0)
                continue;
            NaturalClass y = new NaturalClass(proj, segs, x.ftrs, true);
            if (!S.contains(y))
                S.add(y);
        }
    }
 
    // assign unique ids to natural classes, make natural class array
    List<NaturalClass> L = new ArrayList<NaturalClass>(S);
    Collections.sort(L);
    nNaturalClasses = L.size();
    naturalClasses = new NaturalClass[nNaturalClasses];
    for (int i=0; i<nNaturalClasses; i++) {
        naturalClasses[i] = L.get(i);
        naturalClasses[i].id = i;
    }
    
    // make natural class containing all segments
    sigma = new NaturalClass(proj, (BitSet) proj.segmentMask.clone(), null, false);
}


// convenenience functions mapping segment sets / strings to natural classes

// find natural class matching segment set
public NaturalClass get(BitSet segs) {
    for (NaturalClass x : naturalClasses) {
        if (x.segs.equals(segs)) return x;
    }
    return null;
}

// find sequence of natural classes matching segment sets
public NaturalClass[] get(BitSet[] S) {
    int n = S.length;
    NaturalClass[] X = new NaturalClass[n];
    for (int i=0; i<n; i++) {
        X[i] = get(S[i]);
    }
    return X;
}

// find natural class matching string description
// todo: remove (use NaturalClassUtil.fromString instead)
public NaturalClass get(Projection proj, String s) {
    NaturalClass x = NaturalClassUtil.fromString(proj, s);
    return get(x.segs);
}

// find sequence of natural classes matching string description
// todo: remove (use NaturalClassUtil.fromString instead)
public NaturalClass[] get(Projection proj, String[] S) {
    int n = S.length;
    NaturalClass[] X = NaturalClassUtil.fromString(proj, S);
    NaturalClass[] Y = new NaturalClass[n];
    for (int i=0; i<n; i++) {
        Y[i] = get(X[i].segs);
    }
    return Y;
}

public String toString() {
    StringBuffer val = new StringBuffer();
    for (NaturalClass nc : naturalClasses) {
        val.append(nc.toString());
        val.append("\n");
    }
    return val.toString();
}

// commandline access, for example:
// java edu.jhu.features.NaturalClasses ~/Projects/UCLAPhonotacticLearner/Wargamay/maxent2/features.txt
public static void main(String[] args) throws Exception {
    Alphabet A = FeatureMatrixReader.apply(args[0]);
    SymbolTable syms = A.syms;
    syms.setWordBegin(syms.get(0));
    syms.setWordEnd(syms.get(1));

    Projection proj = new Projection(A, false);
    NaturalClasses C = new NaturalClasses(proj);
    System.out.println("id\tfeatures\tsegments");
    for (NaturalClass x : C.naturalClasses) {
        System.out.println(x.id +"\t"+ x +"\t"+ NaturalClassUtil.toSymRegExp(syms, x));
    }
}

}
