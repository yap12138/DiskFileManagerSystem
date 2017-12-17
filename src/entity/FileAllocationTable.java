package entity;

import java.util.Arrays;
import java.util.List;

import entity.file.FileInfo;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * 
 * @author Yaphets
 *
 */

public class FileAllocationTable {

	private byte[] table;
	
	private ObservableList<FATItem> ObservableFAT;	//可观察列表，用于绑定前端UI
	
	
	private DoubleProperty totalProperty = new SimpleDoubleProperty(128);
	private DoubleProperty remainProperty = new SimpleDoubleProperty();
	private DoubleProperty usedProperty = new SimpleDoubleProperty();
	
	public FileAllocationTable() {
		// 自动生成的构造函数存根
		table = new byte[128];
		
		FATItem[] srcTable = new FATItem[128];
		for (int i = 0; i < srcTable.length; i++) {
			FATItem item = new FATItem(i);
			srcTable[i] = item;
		}
		
		List<FATItem> srcList = Arrays.asList(srcTable);
		
		ObservableFAT  = FXCollections.observableList(srcList);
	}
	
	public ObservableList<FATItem> getObservableList() {
		return ObservableFAT;
	}
	
	public byte[] getTable() {
		return table;
	}
	
	/**
	 * 更新绑定fat的可观察列表
	 */
	public void updataObservableList() {
		int count = 0;
		int used =0;
		for (int i = 0; i < table.length; i++) {
			ObservableFAT.get(i).setNextIndex(table[i]);
			if (table[i] == 0) {
				count++;
			} else {
				used++;
			}
		}
		remainProperty.set(count);
		usedProperty.set(used);
	}
	
	public DoubleProperty getTotalProperty() {
		return totalProperty;
	}

	public DoubleProperty getRemainProperty() {
		return remainProperty;
	}

	public DoubleProperty getUsedProperty() {
		return usedProperty;
	}

	/**
	 * 分配磁盘空间
	 * @param size 请求大小，单位为一块，即64字节
	 * @return	返回起始盘块的位置，-1表示请求失败
	 */
	public int alloc(int size) {
		if (size > remainProperty.get()) {	
			return -1;
		}
		
		int[] mark = new int[size];
		byte i = 3;
		for (int j = 0; j < size; i++) {
			if (this.table[i] == 0) {
				mark[j] = i;
				if (j > 0)
					this.table[mark[j-1]] = i;
				j++;
			}
		}
		this.table[i-1] = FileInfo.FOE;
		updataObservableList();
		return mark[0];	
	}
}
