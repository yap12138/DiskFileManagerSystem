package entity;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * 
 * @author Yaphets
 *
 */

public class FATItem {
	
	final IntegerProperty curIndex = new SimpleIntegerProperty();	//当前项索引
	final IntegerProperty nextIndex = new SimpleIntegerProperty(0);	//0表示该项未使用，否则指向下一盘块
	
	public FATItem(int index) {
		curIndex.set(index);
	}
	
	public IntegerProperty curIndexProperty() {
		return curIndex;
	}
	public IntegerProperty nextIndexProperty() {
		return nextIndex;
	}
	public void setCurIndex(int curIndex) {
		this.curIndex.set(curIndex);;
	}
	public void setNextIndex(int nextIndex) {
		this.nextIndex.set(nextIndex);
	}
	public int getCurIndex() {
		return curIndex.get();
	}
	public int getNextIndex() {
		return nextIndex.get();
	}
	
	
}
