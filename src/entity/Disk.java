package entity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import entity.file.FileInfo;
import util.Util;

/**
 * 
 * @author Yaphets
 *
 */

public class Disk {
	
	private File simulateDisk;	//模拟磁盘的文件对象
	private FileInfo rootDir;	//磁盘的根目录

	public Disk() {
		initField();
		loadRootDir();
	}
	/**
	 * 初始化类属型
	 */
	private void initField() {
		simulateDisk = new File("storage/disk.dat");
		File supPath = simulateDisk.getParentFile();
		if (!supPath.exists()) {
			supPath.mkdirs();
		}
	}

	/*public String getCurrentPath() {
		String path = null;
		try {
			path = simulateDisk.toURI().toURL().toExternalForm();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return path;
	}*/
	
	/**
	 * 格式化磁盘
	 */
	public void formatDisk() {
		//inout.seek( physicalBlock*bytePerBlock - 4 );
		try {
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "rw");) {
				 inout.setLength(0);
				 byte[] fat = new byte[128];//文件分配表，占用磁盘前两个物理块
				 fat[0] = (byte) 255;		//前两块用来存放FAT
				 fat[1] = (byte) 255;
				 fat[2] = (byte) 255;		//此为根目录
				 for (int i = 3; i <fat.length; i++) {
					 fat[i] = 0;			//剩下的全部空闲
				 }
				 inout.write(fat);
				 FileInfo root = new FileInfo("root", FileInfo.Attr_sysFile, 2);	//初始化根目录
				 byte[] rootCatelog = root.getCatelogItem();
				 inout.write(rootCatelog);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 加载根目录
	 */
	private void loadRootDir() {
		try {
			RandomAccessFile inout = new RandomAccessFile(simulateDisk, "r");
			inout.seek(0);
			byte[] fat = new byte[Util.bytePerBlock*2];
			inout.read(fat);
			byte[] rootItem = new byte[Util.bytePerBlock];
			inout.read(rootItem);
			inout.close();
			rootDir = new FileInfo(rootItem, fat, this);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 读取 文件分配表，保存在FileAllocationTable对象中
	 * @param fat	文件分配表对象
	 */
	public void loadFAT(FileAllocationTable fat) {
		try {
			byte[] table = fat.getTable();
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "r");) {
				inout.read(table);
			}
			fat.updataObservableList();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 更新文件分配表，写入磁盘
	 * @param table 新的文件分配表
	 * @return
	 */
	public boolean updataFAT(byte[] table) {
		try {
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "rw");) {
				 inout.seek(0);
				 inout.write(table);
				 return true;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * 根据FAT将文件的内容存入磁盘
	 * @param buffer	缓存的字节数组
	 * @param startPos	磁盘块起始号
	 * @param table		文件分配表
	 */
	public void store(byte[] buffer, int startPos, byte[] table) {
		try {
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "rw");) {
				for (int i = 0; startPos != -1; i++) {		//用startPos != -1判定结束
					inout.seek(startPos*Util.bytePerBlock);
					inout.write(buffer, i*Util.bytePerBlock, Util.bytePerBlock);
					startPos = table[startPos];
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 *读取磁盘，从开始位置，根据索引表读取长度为length的btye数组
	 * @param startPos	盘块开始位置
	 * @param length	读取长度
	 * @param fat		文件分配表
	 * @return
	 */
	public byte[] load(int startPos, int length, byte[] fat) {
		try {
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "r");) {
				byte[] res = new byte[0];
				byte[] buffer = new byte[Util.bytePerBlock];
				for (int i = 0; i < length; i++) {		//或者用startPos != -1判定结束
					inout.seek(startPos*Util.bytePerBlock);
					inout.read(buffer);
					res = Util.arrayCopy(buffer, res);
					startPos = fat[startPos];
				}
				return res;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 写入目录项
	 * @param startPos	开始的盘块
	 * @param item		目录项
	 * @return
	 */
	public boolean storeCatalogItem(int startPos, byte[] item) {
		try {
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "rw");) {
				inout.seek(startPos*Util.bytePerBlock);
				inout.write(item);
				return true;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * 从开始点位置，根据文件分配表连续删除磁盘块内容
	 * @param startPos	删除的起始点
	 * @param fat		文件分配表
	 */
	public void deleteRom(int startPos, FileAllocationTable fat) {
		if (startPos == 0 || startPos == 1) {
			return;
		}
		try {
			byte[] table = fat.getTable();
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "rw");) {
				//一个盘块的空数据
				byte[] nulldata = new byte[Util.bytePerBlock];
				Arrays.fill(nulldata, (byte)0);
				while (startPos != -1) {
					inout.seek(startPos*Util.bytePerBlock);
					inout.write(nulldata);
					byte nextPointer = table[startPos];
					table[startPos] = 0;
					startPos = nextPointer;
				}
			}
			fat.updataObservableList();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 测试当前磁盘存储情况，读取内容并输出到控制台
	 * @param size	读取的盘块大小
	 * @param pos	盘块起始位置
	 */
	public void test(int size, int pos) {
		try {
			try (RandomAccessFile inout = new RandomAccessFile(simulateDisk, "r");) {
				
				 byte[] sys = new byte[size];
				 inout.seek(pos);
				 inout.read(sys);
				 for (int i = 0; i < sys.length; i++) {
					System.out.print(sys[i] + " ");
					if ((i+1)%64 == 0) 
						System.out.println();
				}
			}
		} catch (FileNotFoundException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取根目录
	 * @return
	 */
	public FileInfo getRootDir() {
		return rootDir;
	}
}
