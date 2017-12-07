// Algorithms from formal concept analysis.
// Constructs a Galois Lattice given a feature matrix (i.e., an 
// 'attribute list' for each segment) and a set of segments that 
// are observed (e.g., in a particular language or environment).
// Slightly modified version of the algorithm in:
//		Choi, Vicky, and Yang Huang. 2006. Faster Algorithms 
//		for Constructing a Galois Lattice, Enumerating All 
//		Maximal Bipartite Cliques and Closed Frequent Sets.

package edu.jhu.features;

import java.util.*;
import java.util.stream.*;
import edu.jhu.util.*;

public class FormalConceptAnalysis
{

static LinkedHashSet<BitSet> Q = new LinkedHashSet<BitSet>();
int nSegments = 0;
int nFeatures = 0;
int[][] featureMatrix = null;
static boolean prune = true;	// prune as in edu.jhu.maxent.CorpusBasedPruner
static boolean topped = false;	// keep top concept? (includes all elements)

static int verbosity = 0;

public BinaryRelation<Concept> galoisLattice(LinkedHashMap<String,int[]> featureMatrix_, BitSet sigma, BitSet sigmaObserved) {
    nSegments = featureMatrix_.size();
    nFeatures = featureMatrix_.values().stream().findFirst().get().length;

    featureMatrix = new int[nSegments][];
    int seg_indx = 0;
    for (int[] ftrs : featureMatrix_.values()) {
        featureMatrix[seg_indx++] = ftrs;
    }

    // initialize set of extents, successor relation, blocked extents
	HashMap<BitSet,int[]> T = new HashMap<BitSet,int[]>();
	HashMap<BitSet,HashSet<BitSet>> succ = new HashMap<BitSet,HashSet<BitSet>>();
	HashSet<BitSet> blocked = new HashSet<BitSet>();

	// initialize queue with the extent containing all of the 
	// segments in Sigma
	BitSet top = (BitSet) sigma.clone();
	Q.clear();
	Q.add(top);
	if (topped) 
		T.put(top,intent(top));
	
	// make galois lattice
	while (!Q.isEmpty()) {
		Iterator<BitSet> iter = Q.iterator();
		BitSet extent = iter.next(); iter.remove();

		if (prune) {
			// check for blocking by a superset of extent 
			// (note that all supersets have already been enumerated)
			if (blocked(extent,top,succ,sigmaObserved)) {
				blocked.add(extent);
			}
			// do not enumerate children if extent (and therefore all 
			// extent subsets) has null intersection with sigmaObserved
			if (!extent.intersects(sigmaObserved)) {
				continue;
			}
		}

        // xxx todo: document (refer to Choi \& Huang 2006)
		Concept C = new Concept(extent,intent(extent));
		LinkedList<Concept> children = sprout(C);
		for (Concept child : children) {
			if (!T.containsKey(child.extent)) {
				if (completeIntent(child,C)) {
					update(succ,extent,child.extent);
					Q.add(child.extent);
				}
				child.intent = union(C.intent,child.intent);
				T.put(child.extent,child.intent);
			}
			else if (Arrays.equals(union(C.intent,child.intent), intent(child.extent))) {
				update(succ,extent,child.extent);
				Q.add(child.extent);
			}
		}
	}

	// post-processing
	// remove blocked extents
	if (verbosity>0) System.out.println("# blocked: "+ blocked.size());
	for (BitSet extent : blocked) {
		T.remove(extent);
	}
	// map extents (and associated succ relation) to concepts
	BinaryRelation<Concept> lattice = new BinaryRelation<Concept>();
	HashMap<BitSet,Concept> extentToConcept = new HashMap<BitSet,Concept>();
	for (BitSet x : T.keySet()) {
		Concept Cx = new Concept(x,intent(x));
		extentToConcept.put(x,Cx);
		lattice.add(Cx);
	}
	for (BitSet x : succ.keySet()) {
		if (!T.containsKey(x)) continue;
		for (BitSet y : succ.get(x)) {
			if (!T.containsKey(y)) continue;
			Concept Cx = extentToConcept.get(x);
			Concept Cy = extentToConcept.get(y);
			lattice.add(Cx,Cy);
		}
	}
	
	// ensure that sigmaObserved is at the top of the lattice 
	// (as if there were some property shared by all elts)
	Concept Call = new Concept(sigmaObserved,new int[nFeatures]);
	if (!lattice.S.contains(Call)) {
		for (Concept Cx : new HashSet<Concept>(lattice.S)) {
			lattice.add(Call,Cx);
		}
		lattice.S.add(Call);
	}

	return lattice;
}


// utility function: adds y to map.get(x), 
// creating map.get(x) if it does not already exist
private void update(HashMap<BitSet,HashSet<BitSet>> map, BitSet x, BitSet y) {
	HashSet<BitSet> map_x = map.get(x);
	if (map_x==null) {
		map_x = new HashSet<BitSet>();
		map.put(x,map_x);
	}
	map_x.add(y);
}

// returns a list of all 'children' of a given concept 
// (as in Choi \& Huang 2006)
private LinkedList<Concept> sprout(Concept C) {
	// identify the attributes (feature-value pairs) that are in the attribute 
	// set of at least one member of C but not in the attribute set of C
    // (because they are not shared by all members of C)
	HashMap<FeatureValuePair,BitSet> R = new HashMap<FeatureValuePair,BitSet>();
	for (int a=C.extent.nextSetBit(0); a!=-1; a=C.extent.nextSetBit(a+1)) {
		for (int f=0; f<nFeatures; f++) {
			if (featureMatrix[a][f]!=0 && featureMatrix[a][f]!=C.intent[f]) {
				FeatureValuePair fv = new FeatureValuePair(f,featureMatrix[a][f]);
				if (!R.containsKey(fv))
					R.put(fv,new BitSet());
			}
		}
	}

    // for each such attribute identified immed. above, find all members
	// of C that posssess the attribute -- this creates a 'proto-child'
    for (Map.Entry<FeatureValuePair,BitSet> protoChild : R.entrySet()) {
        FeatureValuePair fv = protoChild.getKey();
        BitSet S = protoChild.getValue();
        for (int a=C.extent.nextSetBit(0); a!=-1; a=C.extent.nextSetBit(a+1))
            if (featureMatrix[a][fv.ftr]==fv.val)
                S.set(a);
    }

	// collapse (i.e., merge the attributes of) proto-children that
    // have the same extent within C, making new concepts
	HashMap<BitSet,Concept> D = new HashMap<BitSet,Concept>();
	for (Map.Entry<FeatureValuePair,BitSet> protoChild : R.entrySet()) {
		FeatureValuePair fv = protoChild.getKey();
		BitSet S = protoChild.getValue();
		if (!D.containsKey(S)) {
			int[] intent = new int[nFeatures];
			intent[fv.ftr] = fv.val;
			D.put(S, new Concept((BitSet) S.clone(), intent));
		} else {
			Concept X = D.get(S);
			// X.extent.or(S); // X = D.get(S) => X.extent == S
			X.intent[fv.ftr] = fv.val;  // collapse (merge) attributes
		}
	}
	
	return new LinkedList<Concept>(D.values());
}

// returns true iff the induced attribute set of the child 
// is 'complete' w.r.t. the parent (as in Choi \& Huang 2006)
private boolean completeIntent(Concept child, Concept parent) {
	int[] induced = union(child.intent,parent.intent);
	int[] intent = intent(child.extent);
	boolean value = Arrays.equals(induced,intent);
	//System.out.println("completeIntent? "+ child);
	//System.out.println("induced = "+ Arrays.toString(induced));
	//System.out.println("intent  = "+ Arrays.toString(intent));
	//System.out.println((value) ? "yes" : "no");
	return value;
}

// union of attributes lists (intents), with intent2
// taking priority in case of conflicts
private int[] union(int[] intent1, int[] intent2) {
	int[] intent = Arrays.copyOf(intent1,nFeatures);
	for (int f=0; f<nFeatures; f++)
		if (intent2[f]!=0)
			intent[f] = intent2[f];
	return intent;
}

// find attributes shared by all members of set S
private int[] intent(BitSet S) {
	int[] intent = new int[nFeatures];
    if (S.cardinality()==0)
        return intent;

    int v = 0; boolean first = false;
    for (int f=0; f<nFeatures; f++) {
        v = 0; first = true;
        for (int a=S.nextSetBit(0); a!=-1; a=S.nextSetBit(a+1)) {
            if (first) { v = featureMatrix[a][f]; first = false; }
            if (v==0 || featureMatrix[a][f]!=v) { v = 0; break; }
        }
        intent[f] = v;
    }
	return intent;
}

// returns true iff S1 is a subset (proper or not) of S2
private boolean subsetOf(BitSet S1, BitSet S2) {
	for (int i=S1.nextSetBit(0); i!=-1; i=S1.nextSetBit(i+1))
		if (!S2.get(i)) return false;
	return true;
}

// returns true iff child is blocked by a superset w.r.t. sigmaObserved; 
// searches the supersets by starting at top and walking down the lattice 
// described by succ
private boolean blocked(BitSet child, BitSet top, HashMap<BitSet,HashSet<BitSet>> succ, BitSet sigmaObserved) {
	LinkedHashSet<BitSet> Q = new LinkedHashSet<BitSet>();
	Q.add(top);

	BitSet tmp = new BitSet();
	while (!Q.isEmpty()) {
		// ancestor <- dequeue(Q)
        BitSet ancestor = Q.iterator().next();
        Q.remove(ancestor);
		// if child==ancestor or ancestor is not a superset of child,
		// then no descendent of ancestor is a superset of child
		if (child.equals(ancestor) || !subsetOf(child,ancestor)) {
			continue;
		}
		// ancestor blocks child iff
		// (ancestor\child) \cap sigmaObserved == \emptyset
		tmp.clear(); tmp.or(ancestor); tmp.andNot(child);
		if (!tmp.intersects(sigmaObserved)) {
			return true;
		}
		// enqueue the immediate descendents of ancestor 
		// (which are identified by the succ relation)
		if (succ.containsKey(ancestor)) {
			Q.addAll(succ.get(ancestor));
		}
	}
	return false;
}

}
