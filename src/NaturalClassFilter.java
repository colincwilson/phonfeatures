package edu.jhu.features;

public class NaturalClassFilter
{

String[] features = null;
int[] values = null;

public NaturalClassFilter(String[] vf) {
    features = new String[vf.length];
    values = new int[vf.length];
    for (int i=0; i<vf.length; i++) {
        String vfi = vf[i];
        features[i] = vfi.substring(1,vfi.length());
        if (vfi.startsWith("+")) {
            values[i] = 1;
        }
        if (vfi.startsWith("-")) {
            values[i] = -1;
        }
    }
}

}
