package ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import entity.file.FileInfo;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import util.Util;

public class OpenFileDialog extends Stage {
	private FXMLLoader loader;
	private Pane root;
	
	private Image fileIcon;
	
	private ImageView iconImage;
	private TextField nameField;
	private Label suffixLabel;
	private Label pathLabel;
	private Label sizeLabel;
	
	private ToggleGroup group;
	private RadioButton rRadio;
	private RadioButton sysRadio;
	private RadioButton rwRadio;
	
	private TextArea contentArea;
	
	private Button confirmBtn;
	private Button cancelBtn;
	
	private FileInfo openFile;
	
	private HashMap<String, Object> outParams;
	
	private boolean alterName;
	private boolean alterAttr;
	private boolean alterContext;
	
	/**
	 * 获取打开文件对话框的实例
	 * @param parent	父窗口
	 * @param file		打开的File
	 * @param outParams 给父窗口传递参数的容器
	 * @return			新创建的窗口
	 */
	public static OpenFileDialog getInstance(Stage parent, FileInfo file, HashMap<String, Object> outParams) {
		OpenFileDialog instance = new OpenFileDialog(parent, file, outParams);
		return instance;
	}
	
	private OpenFileDialog(Stage parent, FileInfo file, HashMap<String, Object> outParams) {
		this.outParams = outParams;
		this.openFile = file;
		this.initOwner(parent);
		this.getIcons().add(parent.getIcons().get(0));
		this.initModality(Modality.WINDOW_MODAL);
		this.setResizable(false);
		
		initStage();
		initField();
		initListener();
	}
	
	private void initStage() {
		try {
			loader = new FXMLLoader(getClass().getResource("openDialogUi.fxml"));
			root = loader.load();
			Scene scene = new Scene(root);
			this.setScene(scene);
			this.setTitle(openFile.getName() + " 属性");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initField() {
		try {
			fileIcon = new Image(new File("res/icons/big_file_style1.png").toURI().toURL().toExternalForm());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		iconImage = (ImageView) root.lookup("#iconImage");
		iconImage.setImage(fileIcon);
		
		nameField = (TextField) root.lookup("#nameField");
		nameField.setText(openFile.getName());
		nameField.setTooltip(new Tooltip("系统文件如不输入后缀，后缀默认为.dat，其他文件默认为.txt"));
		nameField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				//文件名修改后标记
				if (!newValue.equals(openFile.getName())) 
					alterName = true;
				else 
					alterName = false;
			}
		});
		
		suffixLabel = (Label) root.lookup("#suffixLabel");
		suffixLabel.setText(openFile.getSuffix());
		
		pathLabel = (Label) root.lookup("#pathLabel");
		pathLabel.setText(openFile.getAbsolutePath());
		
		sizeLabel = (Label) root.lookup("#sizeLabel");
		sizeLabel.setText(openFile.getLength()*Util.bytePerBlock + " 字节");
		
		rRadio = (RadioButton) root.lookup("#rRadio");
		sysRadio = (RadioButton) root.lookup("#sysRadio");
		rwRadio = (RadioButton) root.lookup("#rwRadio");
		
		contentArea = (TextArea) root.lookup("#contentArea");
		
		group = new ToggleGroup();
		rRadio.setToggleGroup(group);
		rRadio.setUserData(FileInfo.Attr_readOnly);
		sysRadio.setToggleGroup(group);
		sysRadio.setUserData(FileInfo.Attr_sysFile);
		rwRadio.setToggleGroup(group);
		rwRadio.setUserData(FileInfo.Attr_writeRead);
		
		//设置改变监听
		group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
				//只读则不可编辑内容 , 系统二进制文件不可修改
				if (newValue.getUserData().equals(FileInfo.Attr_readOnly)) {
					contentArea.setEditable(false);
					contentArea.setTooltip(null);
				} else if (!openFile.getSuffix().equals("txt") 
						&& newValue.getUserData().equals(FileInfo.Attr_sysFile)) {
					contentArea.setEditable(false);
					contentArea.setTooltip(new Tooltip("系统二进制文件不可修改"));
				} else {
					contentArea.setEditable(true);
					contentArea.setTooltip(null);
				}
				if ( !(newValue.getUserData().equals(openFile.getAttr())) ) {
					alterAttr = true;
				} else {
					alterAttr = false;
				}		
			}
		});
		
		// 选择属性对应的Radio Button，以及请求焦点
		if (openFile.getAttr() == FileInfo.Attr_writeRead) {
			rwRadio.setSelected(true);
			rwRadio.requestFocus();
		} else if (openFile.getAttr() == FileInfo.Attr_sysFile) {
			sysRadio.setSelected(true);
			sysRadio.requestFocus();
		} else {
			rRadio.setSelected(true);
			rRadio.requestFocus();
		}
		
		contentArea.setWrapText(true);
		if (openFile.getContent() != null) {
			if (openFile.getSuffix().equals("txt")) {
				contentArea.setText(openFile.getContent());
			} else {
				contentArea.setText(Util.getBinStringFromBytes(openFile.getContentBytes()));
				if (openFile.getAttr() == FileInfo.Attr_sysFile) {		//系统二进制则不给修改
					contentArea.setEditable(false);
				}
			}
		}
		contentArea.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				// 如果内容被修改过，则标记true
				if (openFile.getContent() == null && !newValue.equals("") 
						|| openFile.getContent() != null && !newValue.equals(openFile.getContent())) {
					alterContext = true;
				} else {
					alterContext = false;
				}
			}
		});
		
		confirmBtn = (Button) root.lookup("#confirmBtn");
		confirmBtn.setDefaultButton(true);
		cancelBtn = (Button) root.lookup("#cancelBtn");
		cancelBtn.setCancelButton(true);
	}
	
	private void initListener() {
		confirmBtn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				//获取修改的数据,未修改则直接返回
				if (!alterAttr && !alterName && !alterContext) {
					outParams.put("alter", false);
				} else {		//如果修改了文件，则请求磁盘写入
					outParams.put("alter", true);
					if (alterContext) {		
						outParams.put("newContext", contentArea.getText());
					}
					if (alterAttr) {
						outParams.put("newAttr", new Integer((int) group.getSelectedToggle().getUserData()));
					}
					if (alterName) {
						outParams.put("newName", dealFileName(nameField.getText()));
					}
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
		if (group.getSelectedToggle().getUserData().equals(FileInfo.Attr_writeRead))
			builder.append(".txt");
		else
			builder.append(".dat");
		return builder.toString();
	}
}
