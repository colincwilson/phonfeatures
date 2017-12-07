// container for a single natural class
// note: hashing, comparison, and equality are based on segments (not features)

package edu.jhu.features;

import java.util.*;

public class NaturalClass implements Comparable
{
public int id                   = 0;        // identifying integer
public String name              = null;     // feature matrix in string format
public BitSet segs              = null;     // segment set
public boolean[] segs_          = null;     // segment set (faster access than segs?)
public int[] ftrs               = null;     // feature specifications
public int size                 = 0;        // cardinality of segment set
public boolean complement       = false;    // is this a complement class?
public ArrayList<BitSet> diffs  = null;     // differences between segments in this class and those of immed. dominating classes in lattice

// empty natural class (possibly representing input or output epsilon)
public NaturalClass() { }

// natural class constructor
public NaturalClass(Projection proj, BitSet segs, int[] ftrs, boolean complement) {
    //this.id         = id;
    this.segs       = segs;
    this.segs_      = new boolean[proj.getAlphabet().nSegments];
    for (int i=segs.nextSetBit(0); i!=-1; i=segs.nextSetBit(i+1)) {
        this.segs_[i] = true;
    }
    this.ftrs       = ftrs;
    this.size       = segs.cardinality();
    this.complement = complement;
    this.name       = NaturalClassUtil.toString(proj, this);
}

// orders natural classes by size (larger first),
// then by subset relation, then lexicographically
public int compareTo(Object o) {
	NaturalClass x = (NaturalClass) o;
	if (size>x.size) return -1;
	if (size<x.size) return +1;
	if (segs.equals(x.segs)) return 0;
    if (NaturalClassUtil.subsetOf(this, x)) return -1;
    if (NaturalClassUtil.subsetOf(x, this)) return +1;
    
    int i = segs.nextSetBit(0);
    int j = x.segs.nextSetBit(0);
    while (i!=-1 && j!=-1) {
        if (i<j) return -1;
        if (i>j) return +1;
        i = segs.nextSetBit(i+1);
        j = x.segs.nextSetBit(j+1);
    }
    return 0;
}

public int hashCode() {
	return segs.hashCode();
}

public boolean equals(Object o) {
	if (o==null) return false;
	if (o==this) return true;
	if (!(o instanceof NaturalClass)) return false;
	NaturalClass x = (NaturalClass) o;
	return segs.equals(x.segs);
}

public String toString() {
    if (name!=null)
        return name;
    else
        return "<"+ segs +":"+ Arrays.toString(ftrs) +">";
}

}
