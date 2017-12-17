package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import util.Util;

public class CharLengthTest {

	public static void main(String[] args) throws IOException {
		String src = "我日死你的吗我日死你的吗我日死你的吗我日死你的吗";
		byte[] srcBytes = src.getBytes();
		System.out.println(srcBytes.length);
		System.out.println(srcBytes.length/64 + 1);
	}

}
