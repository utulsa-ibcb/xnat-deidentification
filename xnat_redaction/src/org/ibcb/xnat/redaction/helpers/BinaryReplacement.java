package org.ibcb.xnat.redaction.helpers;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class BinaryReplacement {
	
	public static void main(String[] args) throws IOException{
		File file = new File("whatever.txt");
        byte[] rep = byteArray("yak");
		LinkedList<byte[]> patts = new LinkedList<byte[]>();
		patts.add(byteArray("ame"));
		patts.add(byteArray("eis"));
		patts.add(byteArray("ply"));
		patts.add(byteArray("lyt"));
		find_and_replace(file, patts, rep);
	}

    public static byte[] byteArray(String s) {
        char[] carr = s.toCharArray();
        byte[] barr = new byte[carr.length];
        for (int i = 0; i < carr.length; i++) barr[i] = (byte)carr[i];
        return barr;
    }

    //Calculates prefix functions (also known as overlap functions) for the modified KMP matcher
    public static int[][] prefixFuncs(List<byte[]> patterns, int m) {
        int i = 0;
        int[][] pfuncs = new int[patterns.size()][m];
        for (byte[] patt : patterns) {
            int[] pi = pfuncs[i];
            pi[0] = -1;
            int k = -1;
            for (int q = 1; q < m; q++) {
                while (k >= 0 && patt[k+1] != patt[q]) k = pi[k];
                if (patt[k+1] == patt[q]) k++;
                pi[q] = k;
            }
            i++;
        }
        return pfuncs;
    }
	
    //This method assumes that everything in List<byte[]> bytes
    //and that byte[] replace are all of the same lengths.
    //It uses the KMP string searching algorithm to find matches.
	public static void find_and_replace(File file, List<byte[]> bytes, byte[] replace) throws IOException {
        int size = bytes.size();
        if (size == 0) return;

        int len = replace.length;

        //Modified KMP (Knuth-Morris-Pratt) Matcher
        int[][] pfuncs = prefixFuncs(bytes, len);
        int[] matchindex = new int[size];
        for (int i = 0; i < size; i++) matchindex[i] = -1;

        int b;
        RandomAccessFile rw = null;
        try {
            rw = new RandomAccessFile(file, "rw");
            long fp = 0; //File pointer
            while ((b = rw.read()) != -1) {
                fp++; //We read a byte, so advance the file pointer
                int i = 0;
                for (byte[] patt : bytes) {
                    int[] pi = pfuncs[i]; //Get the prefix function corresonding to the current pattern
                    int q = matchindex[i]; //Number of bytes currently matching the pattern minus one
                    while (q >= 0 && patt[q+1] != b) q = pi[q];
                    if (patt[q+1] == b) q++;
                    if (q == len-1) { //We found a match
                        rw.seek(fp-len);
                        rw.write(replace);
                        //Since we replaced the matched patterns, we reset
                        //the number the number of bytes matching each pattern
                        for (int j = 0; j < size; j++) matchindex[j] = -1; 
                        break;                                             
                    }
                    matchindex[i] = q; //Write the index of the last element of the longest matching prefix back to the array
                    i++; //Increment the loop counter
                }
            }
        }
		finally {
			if (rw != null) rw.close();
		}
	}

	public static void replace(File file, List<Integer> offset, byte[] replace) throws IOException {
		if(offset.size() == 0) return;
		RandomAccessFile rw = null;

		try {
			rw = new RandomAccessFile(file,"rw");
			for (int off : offset) { //Go through each offset in the list of offsets
				rw.seek(off);
                for (int j = 0; j < replace.length; j++) {
                    if (rw.read() == -1) break; //Check to see if you're at the end-of-file. If so, nothing left to do.
                    rw.seek(off+j); //Move the file pointer back to its previous position (reading moved it one forward)
                    rw.write(replace[j]); //Replace the byte at position (off+j) with replace[j]
                }
			}
		}
		finally {
			if (rw!=null) rw.close();
		}
	}

}