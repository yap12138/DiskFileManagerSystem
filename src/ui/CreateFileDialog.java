package ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import entity.file.FileInfo;
import entity.file.FileType;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import util.Util;

public class CreateFileDialog extends Stage {
	
	private FXMLLoader loader;
	private Pane root;
	
	private Image folderIcon;
	private Image fileIcon;
	
	private RadioButton selFile;
	private RadioButton selDir;
	private ImageView preview;
	
	private TextField fileName;
	private ChoiceBox attrBox;
	
	private Button confirmBtn;
	private Button cancelBtn;
	
	private HashMap<String, Object> outParams;
	
	/**
	 * 通过静态方法获取对话框的实例，指定父窗口
	 * @param parent	父窗口
	 * @param map		要传递的参数Map
	 * @return			对话框实例
	 */
	public static CreateFileDialog getInstance(Stage parent, HashMap<String, Object> map) {
		CreateFileDialog instance = new CreateFileDialog(parent, map);
		return instance;
	}
	
	private CreateFileDialog(Stage parent, HashMap<String, Object> map) {
		this.outParams = map;
		this.initOwner(parent);
		this.initModality(Modality.WINDOW_MODAL);
		this.setResizable(false);
		this.getIcons().add(parent.getIcons().get(0));
		
		initStage();
		initField();
		initListener();
	}
	/**
	 * 初始化对话框窗口
	 */
	private void initStage() {
		try {
			loader = new FXMLLoader(getClass().getResource("createDialogUi.fxml"));
			root = loader.load();
			Scene scene = new Scene(root);
			this.setScene(scene);
			this.setTitle("新建");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 初始化属性
	 */
	private void initField() {
		try {
			folderIcon = new Image(new File("res/icons/big_folder_style1.png").toURI().toURL().toExternalForm());
			fileIcon = new Image(new File("res/icons/big_file_style1.png").toURI().toURL().toExternalForm());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		fileName = (TextField) root.lookup("#fileNameField");
		fileName.setTooltip(new Tooltip("系统文件如不输入后缀，后缀默认为.dat，其他文件默认为.txt"));
		
		confirmBtn = (Button) root.lookup("#confirmBtn");
		confirmBtn.setDefaultButton(true);
		cancelBtn = (Button) root.lookup("#cancelBtn");
		cancelBtn.setCancelButton(true);
		
		attrBox = (ChoiceBox) root.lookup("#attrBox");
		attrBox.setTooltip(new Tooltip("选择属性"));
		
		preview = (ImageView) root.lookup("#preViewFile");
		selFile = (RadioButton) root.lookup("#fileRadio");
		selDir = (RadioButton) root.lookup("#dirRadio");
		selFile.setUserData(fileIcon);
		selDir.setUserData(folderIcon);
		
		//将radiobutton设置到同一个组
		ToggleGroup group = new ToggleGroup();
		selFile.setToggleGroup(group);
		selDir.setToggleGroup(group);
		//设置监听，当选中的radio button改变时，更改choicebox中的内容
		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
				if(group.getSelectedToggle() != null) {
					preview.setImage((Image) group.getSelectedToggle().getUserData());
					if (newValue.getUserData() == fileIcon) {
						attrBox.setItems(FXCollections.observableArrayList("普通文件",new Separator(),"系统文件"));
						fileName.setPromptText("目录名");
					}
					else {
						attrBox.setItems(FXCollections.observableArrayList("目录"));
						fileName.setPromptText("目录名");
					}
				}
			}
		});
		
		selFile.setSelected(true);
		selFile.requestFocus();
	}
	/**
	 * 初始化按钮事件处理器
	 */
	private void initListener() {
		confirmBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				//检测输入是否正确，不正确则弹出提示框
				if (fileName.getText().equals("") || attrBox.getSelectionModel().getSelectedIndex()==-1) {
					Util.callAlert(AlertType.ERROR, "错误", "文件参数错误！", CreateFileDialog.this);
					return;
				}
				//存入文件参数返回给父窗口处理
				if (selFile.isSelected()) {
					outParams.put("type", FileType.File);
					String name = dealFileName(fileName.getText());
					outParams.put("name", name);
					if (attrBox.getSelectionModel().getSelectedIndex() == 0)
						outParams.put("attr", FileInfo.Attr_writeRead);
					else
						outParams.put("attr", FileInfo.Attr_sysFile);
				} else {
					outParams.put("name", fileName.getText());
					outParams.put("type", FileType.Directory);
					outParams.put("attr", FileInfo.Attr_directory);
				}
				
				close();
			}
		});
		
		cancelBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				//返回父窗口
				close();
			}
		});
	}
	/**
	 * 系统文件如不输入后缀，后缀默认为.dat，其他文件默认为.txt
	 * @param src 用户输入的原始文件名
	 * @return 处理好的正确文件名
	 */
	private String dealFileName(String src) {
		int index = src.indexOf('.');
		if (index != -1) {
			return src;
		}
		StringBuilder builder = new StringBuilder(src);
		if (attrBox.getSelectionModel().getSelectedIndex() == 0)
			builder.append(".txt");
		else
			builder.append(".dat");
		return builder.toString();
	}
	
}
