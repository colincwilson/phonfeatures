// used by FormalConceptAnalysis

package edu.jhu.features;

class FeatureValuePair
{
public int ftr = -1;
public int val = -1;
public FeatureValuePair(int ftr, int val) {
	this.ftr = ftr;
	this.val = val;
}
public int hashCode() {	// xxx check
	return 31*ftr + 7*val;
}
public boolean equals(Object o) {
	if (o==null) return false;
	if (o==this) return true;
	if (!(o instanceof FeatureValuePair)) return false;
	FeatureValuePair fv = (FeatureValuePair) o;
	if (fv.ftr!=ftr || fv.val!=val) return false;
	return true;
}
}
