// used by FormalConceptAnalysis

package edu.jhu.features;

import java.util.*;
import java.util.stream.*;

public class Concept
{
public BitSet extent            = null;
public boolean[] extentArray    = null;
public int[] intent             = null;

public Concept(BitSet extent, int[] intent) {
	this.extent = extent;
	this.intent = intent;
}

public int hashCode() {
	return extent.hashCode();
}

public boolean equals(Object o) {
	if (o==null) return false;
	if (o==this) return true;
	if (!(o instanceof Concept)) return false;
	Concept C = (Concept) o;
	if (!C.extent.equals(extent)) return false;
	return true;
}

public String toString() {
	return "<"+ extent +":"+ Arrays.toString(intent) +">";
}

public String toString(LinkedList<String> featureNames) {
    String value = IntStream.range(0, intent.length)
        .filter(f -> intent[f]!=0)
        .mapToObj(f -> ((intent[f]==1) ? "+" : "-") + featureNames.get(f))
        .collect(Collectors.joining(",", "[", "]"));
	return value.toString();
}

// xxx todo: reverse order of arguments?
public String toString(LinkedList<String> segmentNames, LinkedList<String> featureNames) {
    StringBuffer value = new StringBuffer();
    value.append(
        toString(featureNames)
    );
    value.append("\\n");
    value.append(
        extent.stream()
        .mapToObj(id -> segmentNames.get(id))
        .collect(Collectors.joining(",", "{", "}"))
    );
    return value.toString();
}

}
