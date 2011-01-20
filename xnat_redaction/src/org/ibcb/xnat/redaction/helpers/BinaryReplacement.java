package org.ibcb.xnat.redaction.helpers;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class BinaryReplacement {
	
	public static void main(String[] args) throws IOException{
//		byte z = (byte)'0';
//		byte one = (byte)'1';
//		byte[] find = {z, one, one, z};
//		byte[] r = {one, z, z, one};
//		File file = new File("whatever.txt");
//		LinkedList<byte[]> l = new LinkedList<byte[]>();
//		l.add(find);utulsa
//		find_and_replace(file, l, r);
		byte zero =(byte)'0';
		byte one = (byte)'1';
		byte[] r = {one,one,one};
		File file = new File("whatever.txt");
		LinkedList<Integer> offset = new LinkedList<Integer>();
		offset.add(30);
		replace(file,offset,r);
	}
	
	public static void find_and_replace(File file, List<byte[]> bytes, byte[] replace) throws IOException
	{
		int size = bytes.size();
		if (size == 0) return;
		
		int len = bytes.get(0).length;
		int rlen = Math.min(len, replace.length); //Ideally, len and replace.length (the bits to be replaced and replacing bits, respectively)
		                                          //will match, but if not, we can only use the smaller of the two for replacements.
		byte[] b = new byte[len];
				
		RandomAccessFile rw = null;
		try {
			rw = new RandomAccessFile(file, "rw");
			while (rw.read(b) == len) {
				
				label:
				for (byte[] arr : bytes) {
					for (int i = 0; i < len; i++) {
						if (arr[i] != b[i]) continue label; //If we don't have a match, go on to the next byte array to replace.
					}
                    long fp = rw.getFilePointer();
					rw.seek(fp - len); //Since testing for a match moved the file pointer to the end of where we need to replace, we move it back.
					rw.write(replace, 0, rlen); //Write rlen bytes to the file at offset 0 from the current position.
					rw.seek(fp + len); //Writing moved the file pointer rlen bytes forward. We want to move it len bytes forward.
				}
			}
		}
		finally {
			if (rw != null) rw.close();
		}
	}

	public static void replace(File file, List<Integer> offset, byte[] replace) throws IOException
	{
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