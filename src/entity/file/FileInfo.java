package entity.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import entity.Disk;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import util.Util;

/**
 * 
 * @author Yaphets
 *
 */

public class FileInfo {
	private String absolutePath;	//绝对路径,不包含后缀
	private String name;	//存名字，不包含后缀
	private String suffix;	//文件后缀,文件夹则无
	private FileType type;
	private int attr;		//文件属性，0表示只读文件，1表示系统文件，2表示普通可读写文件，3表示目录
	private int startPos;	//目录项起始点
	
	private int contentPos;	//文件内容起始点
	private int length;		//64字节/单位
	private String content; //内容
	
	public HashMap<String, FileInfo> subMap;	//子目录项最多有8个,文件为null
	public FileInfo parent;						//文件的上层目录
	
	private byte[] catelogItem;		//当前文件的目录项
	
	public short[] childStartPos;	//当前如果是目录，则表示子目录项的存放的盘号 
	
	private TreeItem<FileInfo> Tag;			//文件绑定treeView的节点
	private StringProperty fullName;		//用来绑定treeView 单元格文本
	
	public static final byte FOE = -1;
	
	public static final int Attr_readOnly = 0;
	public static final int Attr_sysFile = 1;
	public static final int Attr_writeRead = 2;
	public static final int Attr_directory = 3;

	/**
	 * 文件的构造函数
	 * @param fileName
	 * @param attr
	 * @param startPos
	 * @param length
	 * @param content
	 */
	public FileInfo(String fileName, int attr, int startPos, int contentPos, int length, String content) {
		String[] temp = new String[2];
		temp = fileName.split("\\.");
		this.name = temp[0];
		this.suffix = temp[1];
		this.fullName = new SimpleStringProperty();
		this.fullName.set(fileName);
		this.type = FileType.File;
		this.attr = attr;
		this.startPos = startPos;
		this.contentPos = contentPos;
		this.length = length;
		this.content = content;
		
		this.subMap = null;
		this.parent = null;
		
		
		
		updataCatalogItem();
	}
	/**
	 * 文件夹的构造函数
	 * @param dirName	文件夹名称
	 * @param attr		文件夹属性
	 * @param startPos	文件夹目录项的磁盘起始位置
	 */
	public FileInfo(String dirName, int attr, int startPos) {
		this.name = dirName;
		this.fullName = new SimpleStringProperty();
		this.fullName.set(dirName);
		this.type = FileType.Directory;
		this.attr = Attr_directory;
		this.startPos = startPos;
		this.length = 0;
		
		this.subMap = new HashMap<>(8);
		this.parent = null;
		
		updataCatalogItem();
	}
	/**
	 * 根据读取的目录项创建文件/目录
	 * @param catalogItem	目录项
	 * @param fat	文件分配表
	 * @param disk	如果是文件则需要用disk来读出内容
	 */
	public FileInfo(byte[] catalogItem, byte[] fat, Disk disk) {
		try {
			this.catelogItem = catalogItem;
			this.fullName = new SimpleStringProperty();
			try(ByteArrayInputStream bais = new ByteArrayInputStream(catalogItem);
				DataInputStream dis = new DataInputStream(bais);) {
				//获取绝对路径
				byte[] absolutePathBytes = new byte[24];
				dis.read(absolutePathBytes);
				this.absolutePath = new String(absolutePathBytes);
				this.absolutePath = Util.trim(this.absolutePath, '\0');
				//文件类型 / 目录下其他文件或目录的目录项 存放的盘号
				//获取文件类型再区分
				byte[] fs_sci = new byte[16];
				dis.read(fs_sci);
				//属性
				if (dis.readInt() == 0)
					this.type = FileType.File;
				else 
					this.type = FileType.Directory;
				this.attr = dis.readInt();
				//重新处理fs_sci
				if (this.type == FileType.File) {			//填写文件后缀
					this.suffix = new String(fs_sci);
					this.suffix = Util.trim(this.suffix, '\0');
				} else {									//存子目录项 存盘号
					ByteArrayInputStream bais1 = new ByteArrayInputStream(fs_sci);
					DataInputStream dis1 = new DataInputStream(bais1);
					ArrayList<Short> aList = new ArrayList<>();
					while(dis1.available()>0) {
						short var = dis1.readShort();
						if (var != 0)
							aList.add(var);
					}
					this.childStartPos = new short[aList.size()];
					for (int i = 0; i < this.childStartPos.length; i++) {
						this.childStartPos[i] = aList.get(i);
					}
					
					bais1.close();
					dis1.close();
				}
				//起始盘块号
				if (this.type == FileType.File) {
					this.contentPos = dis.readInt();
					this.startPos = dis.readInt();
				} else {
					this.startPos = (int) dis.readLong();
				}
				//文件长度 / 保留字
				if (this.type == FileType.File)
					this.length = (int) dis.readLong();
				else
					dis.readLong();
				//如果是文件则把内容取出(若不是空文件)
				if (this.type == FileType.File) {
					if (this.contentPos != 0) {
						byte[] contents = disk.load(this.contentPos, this.length, fat);
						String str = new String(contents);
						this.content = Util.trim(str, '\0');
					}
				} else {
					this.subMap = new HashMap<>(8);
				}
				//把绝对路径转化成文件名
				this.name = Util.getNameForAbsolute(this.absolutePath);
				if (this.type == FileType.File) 
					this.fullName.set(Util.getFullNameForAbsolute(absolutePath));
				else 
					this.fullName.set(name);
			}
		}  catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		String str = this.name;
		if (this.type == FileType.File) 
			str = str + "." + this.suffix;
		return str;
	}

	/**
	 * 返回当前对象是否为文件
	 * @return	如果是文件返回true，否则返回false
	 */
	public boolean isFile() {
		if (this.type == FileType.File)
			return true;
		else 
			return false;
	}
	
	public int getAttr() {
		return attr;
	}
	public void setAttr(int attr) {
		this.attr = attr;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public int getContentPos() {
		return contentPos;
	}
	public void setContentPos(int contentPos) {
		this.contentPos = contentPos;
	}
	
	public byte getStartPos() {
		return (byte)startPos;
	}
	
	public byte[] getCatelogItem() {
		return catelogItem;
	}
	
	public TreeItem<FileInfo> getTag() {
		return Tag;
	}
	
	public void setTag(TreeItem<FileInfo> tag) {
		Tag = tag;
	}
	
	/**
	 * 更新绝对路径且返回
	 * @return	文件的绝对路径
	 */
	public String getAbsolutePath() {
		StringBuilder strBuiler = new StringBuilder();
		buildAbsolutePath(strBuiler);
		this.absolutePath = strBuiler.toString();
		return this.absolutePath;
	}
	
	public StringProperty getFullNameProperty() {
		return fullName;
	}
	/**
	 * 设置文件全名
	 * @param name
	 */
	public void setName(String name) {
		String[] temp = new String[2];
		temp = name.split("\\.");
		this.name = temp[0];
		this.suffix = temp[1];
		this.fullName.set(name);
		getAbsolutePath();
	}

	/**
	 * 获取文件的全名
	 * @return
	 */
	public String getName() {
		String str = new String(name);
		if (this.type == FileType.File) 
			str = str + "." + suffix;
		return str;
	}
	
	public String getSuffix() {
		return suffix;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	/**
	 * 返回文件内容的byte数组
	 * @return	文件内容的字节数组
	 */
	public byte[] getContentBytes() {
		if (length == 0) {
			return null;
		}
		byte[] buffer = Arrays.copyOf(content.getBytes(), length*Util.bytePerBlock);
		if (content.getBytes().length < length*Util.bytePerBlock) 
			Arrays.fill(buffer, content.getBytes().length, buffer.length, (byte)'\0');
		return buffer;
	}
	
	/**
	 * 添加子文件，并且为子文件更新目录项
	 * @param file
	 */
	public void addChild(FileInfo file) {
		file.parent = this;
		subMap.put(file.getName(), file);
		file.updataCatalogItem();
	}
	
	/**
	 * 更新文件的目录项
	 */
	public void updataCatalogItem() {
		byte[] buffer = new byte[Util.bytePerBlock];
		getAbsolutePath();
		//绝对路径	24byte
		byte[] var = Arrays.copyOf(absolutePath.getBytes(), 24);
		if (absolutePath.getBytes().length < 24) 
			Arrays.fill(var, absolutePath.getBytes().length, 24, (byte)'\0');
		Util.fillBytes(buffer, var, 0);
		//文件类型	16byte
		if (this.type == FileType.File) {
			byte[] var1 = Arrays.copyOf(suffix.getBytes(), 16);
			if (suffix.getBytes().length < 16) 
				Arrays.fill(var1, suffix.getBytes().length, 16, (byte)'\0');
			Util.fillBytes(buffer, var1, 24);
		} else {			//目录则充填目录下其他文件或目录的目录项 存放的盘号
			//Arrays.fill(buffer, buffer.length, 40, (byte)0);	
			byte[] subIndex = new byte[16];
			int pos = 0;
			Collection<FileInfo>subCatalog = subMap.values();
			for (Iterator<FileInfo> iterator = subCatalog.iterator(); iterator.hasNext();) {
				FileInfo fileInfo = iterator.next();
				subIndex[pos++] = 0;							//short 的高位
				subIndex[pos++] = fileInfo.getStartPos();		//short 的低位
			}
			Arrays.fill(subIndex, pos, 16, (byte)0);
			Util.fillBytes(buffer, subIndex, 24);
		}
		//属性	8byte
		byte[] dist;
		byte[] attr;
		if (this.type == FileType.File) 
			dist = Util.int2bytes(0);
		else 
			dist = Util.int2bytes(1);
		attr = Util.int2bytes(this.attr);
		Util.fillBytes(buffer, dist, 40);
		Util.fillBytes(buffer, attr, 44);
		//起始盘块号	8byte
		byte[] sp = new byte[8];
		Arrays.fill(sp, (byte)0);
		if(type == FileType.File)
			Util.fillBytes(sp, Util.int2bytes(contentPos), 0);
		byte[] t1 = Util.int2bytes(startPos);
		Util.fillBytes(sp, t1, 4);
		Util.fillBytes(buffer, sp, 48);
		//文件长度 / 保留字 8byte
		if (this.type == FileType.File) {
			byte[] fl = new byte[8];
			Arrays.fill(fl, (byte)0);
			byte[] t2 = Util.int2bytes(length);
			Util.fillBytes(fl, t2, 4);
			Util.fillBytes(buffer, fl, 56);
		} else {
			Arrays.fill(buffer, 56, 64, (byte)0);
		}
		
		this.catelogItem = buffer;
	}
	
	@SuppressWarnings("unused")
	private void updataCatalogItem2() throws IOException {
		getAbsolutePath();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		//绝对路径	24byte
		byte[] var = Arrays.copyOf(absolutePath.getBytes(), 24);
		if (absolutePath.getBytes().length < 24)
			Arrays.fill(var, absolutePath.getBytes().length, 24, (byte) '\0');
		dos.write(var);
		// 文件类型 16byte
		if (this.type == FileType.File) {
			byte[] var1 = Arrays.copyOf(suffix.getBytes(), 16);
			if (suffix.getBytes().length < 16)
				Arrays.fill(var1, suffix.getBytes().length, 16, (byte) '\0');
			dos.write(var1);
		} else { // 目录则充填目录下其他文件或目录的目录项 存放的盘号
			// Arrays.fill(buffer, buffer.length, 40, (byte)0);
			byte[] subIndex = new byte[16];
			int pos = 0;
			Collection<FileInfo> subCatalog = subMap.values();
			for (Iterator<FileInfo> iterator = subCatalog.iterator(); iterator.hasNext();) {
				FileInfo fileInfo = iterator.next();
				subIndex[pos++] = 0; // short 的高位
				subIndex[pos++] = fileInfo.getStartPos(); // short 的低位
			}
			Arrays.fill(subIndex, pos, 16, (byte) 0);
			dos.write(subIndex);
		}
		// 属性 8byte
		byte[] dist;
		byte[] attr;
		if (this.type == FileType.File)
			dist = Util.int2bytes(0);
		else
			dist = Util.int2bytes(1);
		attr = Util.int2bytes(this.attr);
		dos.write(dist);
		dos.write(attr);
		// 起始盘块号 8byte
		dos.writeLong((long)startPos);
		//文件长度 / 保留字 8byte
		if (this.type == FileType.File) {
			dos.writeLong((long)length);
		} else {
			dos.writeLong((long)0);
		}
		
		this.catelogItem = baos.toByteArray();
		baos.close();
		dos.close();
	}
	/**
	 * 递归获取文件的绝对路径
	 * @param builder	存放构建的路径字符串
	 */
	protected void buildAbsolutePath(StringBuilder builder) {
		if (this.parent == null) {		//根目录
			builder.append(this.name); 	
		}
		else {
			this.parent.buildAbsolutePath(builder);
			if (this.type == FileType.File) {	
				builder.append("/" + this.name + "." + this.suffix);
			} else {
				builder.append("/" + this.name);
			}
		}
	}	
}
