package util;

import java.util.Arrays;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Window;

public class Util {
	
	public static final int physicalBlockCount = 128;
	public static final int bytePerBlock = 64;
	
	/**
	 * 复制src数组到dest数组后，返回新的数组
	 * @param src
	 * @param dest
	 * @return
	 */
	public static byte[] arrayCopy(byte[] src, byte[] dest) {
		byte[] temp = new byte[src.length + dest.length];
		System.arraycopy(dest, 0, temp, 0, dest.length);
		System.arraycopy(src, 0, temp, dest.length, src.length);
		return temp;
	}
	
	/**
	 * byte[] 转化为 int
	 * @param res 字节数组 res[0]表示最高位
	 * @return	返回对应int
	 */
	public static int bytes2int(byte[] res) {   
		// 一个byte数据左移24位变成0x??000000，再右移8位变成0x00??0000   

		int targets = (res[3] & 0xff) | ((res[2] << 8) & 0xff00) // | 表示安位或   
		| ((res[1] << 24) >>> 8) | (res[0] << 24);   
		return targets;   
	}   
	
	/**
	 * int 转化为byte[]
	 * @param res 
	 * @return 返回对应byte[]
	 */
	public static byte[] int2bytes(int res) {  
		byte[] targets = new byte[4];  
		  
		targets[3] = (byte) (res & 0xff);// 最低位   
		targets[2] = (byte) ((res >> 8) & 0xff);// 次低位   
		targets[1] = (byte) ((res >> 16) & 0xff);// 次高位   
		targets[0] = (byte) (res >>> 24);// 最高位,无符号右移。   
		return targets;   
	} 
	
	/**
	 * 除去字符串src 第一个ch后的所有字符，返回新字符串
	 * @param src
	 * @param ch
	 * @return
	 */
	public static String trim(String src, char ch) {
		String res;
		int sindex = src.indexOf(ch);
		if (sindex > 0) {
			res = src.substring(0, sindex);
			return res;
		} else {
			res = src;
			return res;
		}
	}
	
	/**
	 * 对源byte数组，扩充数组长度并返回
	 * @param srcBytes	源数组
	 * @param length	数组长度的1/64
	 * @return
	 */
	public static byte[] bytesToBlock(byte[] srcBytes, int length) {
		byte[] buffer = Arrays.copyOf(srcBytes, length*Util.bytePerBlock);
		Arrays.fill(buffer, srcBytes.length, buffer.length, (byte)'\0');
		return buffer;
	}
	
	/**
	 * 从绝对路径获取文件名/目录名
	 * @param absolutePath	绝对路径
	 * @return	如果是文件则返回不包含后缀的文件名
	 */
	public static String getNameForAbsolute(String absolutePath) {
		String res;
		int lindex = absolutePath.lastIndexOf('/');
		res = absolutePath.substring(lindex+1, absolutePath.length());
		int dotindex = res.indexOf('.');
		if (dotindex == -1) {
			return res;
		}
		res = res.substring(0, dotindex);
		return res;
	}
	/**
	 * 从绝对路径获取文件
	 * @param absolutePath
	 * @return
	 */
	public static String getFullNameForAbsolute(String absolutePath) {
		String res;
		int lindex = absolutePath.lastIndexOf('/');
		res = absolutePath.substring(lindex+1, absolutePath.length());
		return res;
	}
	
	/**
	 * 将byte数组 转化成相应的二进制文本
	 * @param array
	 * @return
	 */
	public static String getBinStringFromBytes(byte[] array) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			builder.append(Integer.toBinaryString((array[i] &  0xFF) + 0x100).substring(1));
		}
		return builder.toString();
	}
	
	/**
	 * 从disArray数组的startPos开始充填整个src数组，
	 * @param disArray
	 * @param src
	 * @param startPos
	 */
	public static void fillBytes(byte[] disArray, byte[] src, int startPos) {
		for (int i = 0; i < src.length; i++) {
			disArray[startPos++] = src[i];
		}
	}
	/**
	 * 调用提示对话框
	 * @param type	对话框类型
	 * @param title	对话框标题
	 * @param header	显示内容
	 * @param stage		父窗口
	 */
	public static void callAlert(AlertType type, String title, String header, Window stage) {
		Alert _alert = new Alert(type);
        _alert.setTitle(title);
        _alert.setHeaderText(header);
        _alert.initOwner(stage);
        _alert.show();
	}
	
}
