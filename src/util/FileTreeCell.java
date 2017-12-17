package util;

import java.util.ArrayList;

import entity.file.FileInfo;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;

public class FileTreeCell extends TreeCell<FileInfo> {
	private TextField textField;
	private final ContextMenu rootMenu = new ContextMenu();
	private final ContextMenu fileMenu = new ContextMenu();
	private final ContextMenu dirMenu = new ContextMenu();

	public FileTreeCell(ArrayList<MenuItem> list) {
		initMenu(list);
	}

	private void initMenu(ArrayList<MenuItem> list) {
		// 这部分在外面建立好再传进来
		rootMenu.getItems().add(list.get(0));
		
		fileMenu.getItems().add(list.get(1));
		fileMenu.getItems().add(list.get(2));
		
		dirMenu.getItems().add(list.get(3));
		dirMenu.getItems().add(list.get(4));
		
	}

	/*@Override
	public void startEdit() {
		super.startEdit();

		if (textField == null) {
			createTextField();
		}
		setText(null);
		setGraphic(textField);
		textField.selectAll();
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();

		setText(((FileInfo) getItem()).toString());
		setGraphic(getTreeItem().getGraphic());
	}*/

	@Override
	public void updateItem(FileInfo item, boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			if (isEditing()) {
				if (textField != null) {
					textField.setText(getString());
				}
				setText(null);
				setGraphic(textField);
			} else {
				setText(getString());
				setGraphic(getTreeItem().getGraphic());
				//在此添加菜单
				/*if (!getTreeItem().isLeaf() && getTreeItem().getParent() != null) {
					setContextMenu(addMenu);
				}*/
				if ( item.isFile() ) {
					setContextMenu(fileMenu);
				} else if ( (!item.isFile()) && getTreeItem().getParent() != null ) {
					setContextMenu(dirMenu);
				} else {
					setContextMenu(rootMenu);
				}
			}
			selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
	            String col ;
	            if (isNowSelected) {
	            	textProperty().bind(item.getFullNameProperty());
	            } else {
	            	textProperty().unbind();
	            }
	        });
			
		}
	}

	/*private void createTextField() {
		textField = new TextField(getString());
		textField.setOnKeyReleased((KeyEvent t) -> {
			if (t.getCode() == KeyCode.ENTER) {
				FileInfo temp = getItem();
				temp.setName(textField.getText());
				commitEdit(temp);
			} else if (t.getCode() == KeyCode.ESCAPE) {
				cancelEdit();
			}
		});

	}*/
	
	private String getString() {
		return getItem() == null ? "" : getItem().toString();
	}
}
