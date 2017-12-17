package service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import entity.Disk;
import entity.FileAllocationTable;
import entity.file.FileInfo;
import util.Util;
/**
 * 
 * @author Yaphets
 *
 */
public class DiskManager {
	private Disk disk;		//磁盘
	private FileAllocationTable FAT;	//文件分配表
	public HashMap<String, FileInfo> totalFiles;	//保存磁盘的所有目录项信息
	private final int FileInDirLimit = 8;		//每个文件夹之多有8个子文件
	
	public DiskManager() {
		disk = new Disk();
		FAT = new FileAllocationTable();
		totalFiles = new HashMap<>();
		init();
	}

	public FileAllocationTable getFAT() {
		return FAT;
	}

	private void init() {
		disk.loadFAT(FAT);
		findAllFile();
	}
	/**
	 * 以根目录为初始节点，广度优先搜索找到所有文件
	 */
	private void findAllFile() {
		FileInfo root = disk.getRootDir();
		totalFiles.put(root.getName(), root);
		//广度优先搜索
		Queue<FileInfo> queue = new LinkedList<>();
		queue.offer(root);
		
		while (!queue.isEmpty()) {
			FileInfo var = queue.poll();
			if (var.childStartPos.length != 0) {	//如果有子文件或目录，则加载
				for (int i = 0; i < var.childStartPos.length; i++) {
					short startPos = var.childStartPos[i];		//找到目录项位置
					FileInfo child = loadFileInfo(startPos);
					totalFiles.put(child.getName(), child);
					var.addChild(child);
					if (!child.isFile()) {		//是目录则推入队列，查找是否有子项
						queue.offer(child);
					}
				}
			}
		}
	}
	
	public void formatDisk() {
		disk.formatDisk();
	}
	
	/**
	 * 关闭系统前，广度优先搜索找到所有文件,并更新所有目录项
	 */
	public void updataCatalogItems() {
		FileInfo root = disk.getRootDir();
		byte[] fat = FAT.getTable();
		//广度优先搜索
		Queue<FileInfo> queue = new LinkedList<>();
		queue.offer(root);
		
		while (!queue.isEmpty()) {
			FileInfo var = queue.poll();
			//写入目录项
			var.updataCatalogItem();
			byte[] item = var.getCatelogItem();
			disk.storeCatalogItem(var.getStartPos(), item, fat);
			if (var.subMap != null && var.subMap.size() != 0) {	//如果有子文件或目录，则加载
				Collection<FileInfo> collection = var.subMap.values();
				for (Iterator<FileInfo> iterator = collection.iterator(); iterator.hasNext();) {
					FileInfo fileInfo = iterator.next();
					queue.offer(fileInfo);
				}
			}
		}
	}
	
	/**
	 * 根据目录项在磁盘的位置，找出文件/目录并初始化成FileInfo
	 * @param startPos	目录项在磁盘的位置
	 */
	private FileInfo loadFileInfo(short startPos) {
		byte[] catalogItem = disk.load(startPos, 1, FAT.getTable());
		FileInfo newFile = new FileInfo(catalogItem, FAT.getTable(), disk);
		return newFile;
	}
	
	/**
	 * 创建文件,先创建目录项的空间，再分配内容空间
	 * @param fileName	全名
	 * @param attr	读写属性
	 * @param size	占size个字节
	 * @param content	内容
	 * @param curDirectory	当前目录
	 * @return	返回文件创建的情况，大于0则创建成功，小于0则失败
	 */
	public int createFile(String fileName, int attr, int size, String content, FileInfo curDirectory) {
		if (curDirectory.subMap.containsKey(fileName)) {
			//存在同名同后缀文件
			return -1;
		} else if (attr == FileInfo.Attr_readOnly) {
			//文件属性如果是只读性质则不能建立
			return -2;
		} else {
			if (FAT.getRemainProperty().get() < 1 + size) {
				//如果不够分配一个目录项+文件内容的空间则创建失败
				return -3;
			}
			int startNum = FAT.alloc(1);
			if (startNum == -1) {
				return -3;
			}
			int contentStart;
			if (size > 0) {
				contentStart = FAT.alloc(size);
				if (contentStart == -1) {
					return -3;
				}
			} else {
				contentStart = 0;
			}
			FileInfo file = new FileInfo(fileName, attr, startNum, contentStart, size, content);
			curDirectory.addChild(file);
			totalFiles.put(fileName, file);
			//存目录项
			disk.storeCatalogItem(startNum, file.getCatelogItem(), FAT.getTable());
			//磁盘存文件
			if (size > 0)
				disk.store(file.getContentBytes(), file.getContentPos(), FAT.getTable());
			//更新FAT
			disk.updataFAT(FAT.getTable());	
			return 1;
		}
	}
	
	/**
	 * 创建目录
	 * @param dirName	目录名
	 * @param attr		属性：FileInfo的常量。可读写，系统文件等
	 * @param curDirectory	当前目录
	 * @return	返回目录创建的情况，大于0则创建成功，小于0则失败
	 */
	public int createDirectory(String dirName, int attr, FileInfo curDirectory) {
		if (curDirectory.subMap.containsKey(dirName)) {
			//存在同名同目录
			return -1;
		} else if (curDirectory.subMap.size() >= this.FileInDirLimit) {
			//超过子文件数量限制
			return -2;
		} else {
			int startNum = FAT.alloc(1);
			if (startNum == -1) {
				//磁盘空间不足
				return -3;
			}
			FileInfo dir = new FileInfo(dirName, attr, startNum);
			curDirectory.addChild(dir);
			totalFiles.put(dirName, dir);
			//存目录项
			disk.storeCatalogItem(startNum, dir.getCatelogItem(), FAT.getTable());
			//更新FAT
			disk.updataFAT(FAT.getTable());
			
			return 1;
		}
	}
	/**
	 * 删除指向的文件，更新文件分配表
	 * @param delNode	将要删除的文件/目录
	 */
	public void deleteFileInfo(FileInfo delNode) {
		FileInfo parentNode = delNode.parent;
		parentNode.subMap.remove(delNode.getName());
		totalFiles.remove(delNode.getName());
		//删除目录项
		disk.deleteRom(delNode.getStartPos(), FAT);
		//删除文件内容
		if (delNode.getLength() > 0 && delNode.isFile()) {
			disk.deleteRom(delNode.getContentPos(), FAT);
		}
		//新的FAT写回磁盘
		disk.updataFAT(FAT.getTable());
		//disk.test(128, 0);
	}
	/**
	 * 写入磁盘文件的内容，并更新文件分配表
	 * @param file	准备要写的文件
	 * @param data	文件内容
	 * @return	如果成功则返回true，空间不足返回false
	 */
	public boolean writeFileContent(FileInfo file, String data) {
		//获取数据的字节数组
		byte[] temp = data.getBytes();
		int shortestLength = temp.length/64 + 1;
		int contentStart;
		contentStart = FAT.alloc(shortestLength);
		if (contentStart == -1) {	//磁盘空间不足
			return false;
		} else {
			byte[] buffer = Util.bytesToBlock(temp, shortestLength);
			disk.store(buffer, contentStart, FAT.getTable());
			file.setContent(data);
			file.setContentPos(contentStart);
			file.setLength(shortestLength);
			//新的FAT写回磁盘
			disk.updataFAT(FAT.getTable());
			return true;
		}
	}
}
