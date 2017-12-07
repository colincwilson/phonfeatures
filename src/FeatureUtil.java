// static methods mainly for converting feature vectors to/from strings

package edu.jhu.features;

import java.util.*;
import java.util.stream.*;

public class FeatureUtil
{

static final String PLUS = "+";
static final String MINUS = "-";

// does feature vector ftrs1 subsume feature vector ftrs2?
public static boolean subsumes(int[] ftrs1, int[] ftrs2) {
    for (int i=(ftrs1.length-1); i>=0; i--) {
        if (ftrs1[i]!=0 && ftrs2[i]!=ftrs1[i])
            return false;
    }
    return true;
}

// minimal generalization of ftrs1 given ftrs2
// (note: modifies and returns ftrs1, pass a copy if
// want to retain original ftrs1)
public static int[] minimalGeneralization(int[] ftrs1, int[] ftrs2) {
    for (int i=(ftrs1.length-1); i>=0; i--) {
        if (ftrs2[i]!=ftrs1[i])
            ftrs1[i] = 0;
    }
    return ftrs1;
}

// convert feature vector (in extended sense allowing complements) to string
public static String toString(Alphabet A, int[] ftrs, boolean complement) {
    String value = IntStream.range(0, A.nFeatures)
        .filter(i -> ftrs[i]!=0)
        .mapToObj(i -> ((ftrs[i]==1) ? PLUS : MINUS) + A.featureNames.get(i))
        .collect(Collectors.joining(",", complement ? "[^" : "[", "]"));
    return value;
}

// convert string (possibly including complement symbol) to feature vector
public static int[] fromString(Alphabet A, String s) {
    int[] ftrs = new int[A.nFeatures];
    s = s.replace("[", "").replace("]", "").replace("[^]", "").replaceAll(" ", "");
    if (s.equals("")) return ftrs;  // maximally underspecified feature matrix
    String ftr = null;
    String val = null;
    for (String fv : s.split(",")) {
        ftr = fv.substring(1);
        val = fv.substring(0,1);
        try {
        ftrs[A.featureNames.indexOf(ftr)] =
            (val.equals(PLUS)) ? 1 : val.equals(MINUS) ? -1 : 0;
        } catch (Exception e) { System.out.println("Error: "+ ftr +" "+ val); System.exit(1); }
    }
    return ftrs;
}

}
