package test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import entity.file.FileInfo;
import util.Util;

public class CollectionTest {

	public static void main(String[] args) {
		FileInfo var1 = new FileInfo("Root", FileInfo.Attr_sysFile, 3);
		FileInfo var2 = new FileInfo("bbb", FileInfo.Attr_directory, 4);
		FileInfo var3 = new FileInfo("ccc", FileInfo.Attr_directory, 5);
		FileInfo var4 = new FileInfo("rng.dat", FileInfo.Attr_writeRead, 8, 15, 64, "heiheihei");
		var1.addChild(var2);
		var1.addChild(var3);
		var3.addChild(var4);
		
		HashMap<String, FileInfo> totalFiles = new HashMap<>(8);
		totalFiles.put("rnh", var1);
		totalFiles.put("bbb", var2);
		totalFiles.put("ccc", var3);
		totalFiles.put("rng", var4);
		Collection<FileInfo> temp = totalFiles.values();
		int i = 0;
		for (Iterator iterator = temp.iterator(); iterator.hasNext();) {
			FileInfo fileInfo = (FileInfo) iterator.next();
			System.out.println(fileInfo + " id: " + i++);
		}
		
		FileInfo tFileInfo = totalFiles.get("rng");
		String an = tFileInfo.getAbsolutePath();
		System.out.println(an);
		System.out.println(Util.getNameForAbsolute(an));
	}

}
