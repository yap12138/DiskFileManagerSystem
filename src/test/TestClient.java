package test;

import entity.Disk;

public class TestClient {

	public static void main(String[] args) {
		formatDisTest();
	}
	
	public static void formatDisTest() {
		Disk var = new Disk();
		var.formatDisk();
		var.test(128*64,0);
	}
}
