package edu.jhu.features;

import java.util.*;
import java.util.stream.*;
import edu.jhu.util.*;

public class Corpus
{

public Alphabet A = null;
public LinkedHashMap<String,Counter> data = null;
public LinkedHashMap<String,int[]> endata = null;
public TreeMap<Integer,Counter> lengthDistrib = null;
public int size = 0;
boolean addWordBoundaries = true;

// set by compile()
public int data_size = 0;
public double total_data_freq = 0.0;
public double[] data_freq = null;
public int[][] rawData = null;

   
public Corpus(Alphabet A) {
    this(A, false);
}

public Corpus(Alphabet A, boolean addWordBoundaries) {
    this.A = A;
    this.addWordBoundaries = addWordBoundaries;
    data = new LinkedHashMap<String,Counter>();
    endata = new LinkedHashMap<String,int[]>();
    lengthDistrib = new TreeMap<Integer,Counter>();
    size = 0;
}

public void update(String form, int freq) {
    int[] enform = A.encodeString(form, " ", addWordBoundaries);
    if (data.containsKey(form)) {
        data.get(form).increment(freq);
    } else {
        data.put(form, new Counter(freq));
        endata.put(form, enform);
    }

    int n = enform.length;
    if (lengthDistrib.containsKey(n))
        lengthDistrib.get(n).increment(freq);
    else
        lengthDistrib.put(n, new Counter(freq));
    
    size += freq;
}

// xxx todo: why are start and end needed? why no freq argument?
public void updateCounts(int[] x, int start, int end) {
    int[] enform = null;

    // todo: replace with version of Alphabet.decodeString
    String form = IntStream.range(start, end)
        .mapToObj(i -> A.syms.get(x[i]))
        .collect(Collectors.joining(" "));
    if (data.containsKey(form)) {
        data.get(form).increment();
        enform = endata.get(form);
    }
    else {
        enform = A.encodeString(form, " ", addWordBoundaries);
        data.put(form, new Counter(1));
        endata.put(form, enform);
    }

    int n = enform.length;
    if (lengthDistrib.containsKey(n))
        lengthDistrib.get(n).increment(1);
    else
        lengthDistrib.put(n, new Counter(1));

    size++;
}

// do not call update() or updateCounts() after
// calling compile() on this corpus
public void compile() {
    data_size       = data.size();
    rawData         = new int[data_size][];
    data_freq       = new double[data_size];
    total_data_freq = 0.0;
    int i = 0;
    for (Map.Entry<String,Counter> entry : data.entrySet()) {
        rawData[i]      =   endata.get(entry.getKey());
        data_freq[i]    =   entry.getValue().intValue();
        total_data_freq +=  data_freq[i];
        i++;
    }
}

}
