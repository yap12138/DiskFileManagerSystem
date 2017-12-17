package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import util.Util;

public class redixConvertTest {

	public static void main(String[] args) throws IOException {
		testShorts2shorts();
	}
	
	public static void testShorts2shorts() throws IOException {
		byte[] temp = {0,8,0,1,0,28,0,0,0,0,0,0};
		ByteArrayInputStream bais = new ByteArrayInputStream(temp);
		DataInputStream dis = new DataInputStream(bais);
		ArrayList<Short> aList = new ArrayList<>();
		while(dis.available()>0) {
			short var = dis.readShort();
			if (var != 0)
				aList.add(var);
		}
		short[] aa = new short[aList.size()];
		for (int i = 0; i < aa.length; i++) {
			aa[i] = aList.get(i);
		}
		System.out.println(Arrays.toString(aa));
		
		
		bais.close();
		dis.close();
	}
	
	
	public static void test() throws IOException {
		byte[] temp = {0,0,0,1};
		ByteArrayInputStream bais = new ByteArrayInputStream(temp);
		DataInputStream dis = new DataInputStream(bais);
		short var;
		ArrayList<Short> arrayList = new ArrayList<>();
		while(dis.available()>0) {
			var = dis.readShort();
			System.out.println(var + " rest:"  + dis.available());
			arrayList.add(var);
		}
		
		System.out.println("\n" + Util.bytes2int(temp));
		bais.close();
		dis.close();
		
		int testInt = 1;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(testInt);
		byte[] readB = baos.toByteArray();
		for (int i = 0; i < readB.length; i++) {
			System.out.print(readB[i] + " ");
		}
		System.out.println();
		/**
		 * Util的错了！
		 */
		readB = Util.int2bytes(testInt);
		for (int i = 0; i < readB.length; i++) {
			System.out.print(readB[i] + " ");
		}
		
		baos.close();
		dos.close();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	

}
