package ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import entity.FATItem;
import entity.file.FileInfo;
import entity.file.FileType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import service.DiskManager;
import util.FileTreeCell;
import util.Util;

public class MainUi {
	private FXMLLoader loader;
	private Pane root;
	private Image icon;
	private Stage primaryStage;
	//显示文件的树状视图容器控件
	private TreeView<FileInfo> fileTree;
	//饼图
	private PieChart romChart;
	private PieChart.Data remainArc;
	private PieChart.Data useArc;
	//显示磁盘空间信息
	private Label totalLabel;
	private Label remainLabel;
	//搜索框和按钮
	private TextField sreachField;
	private Button sreachBtn;
	//显示FAT的容器
	private TableView<FATItem> fatView;
	//文件和文件夹图标
	private Image folderIcon;
	private Image fileIcon;
	
	private DiskManager diskManager;
	//格式化按钮
	private Button formatBtn;
	//关于按钮
	private Button aboutBtn;
	
	public MainUi(Stage primaryStage) {
		this.primaryStage = primaryStage;
		try {
			loader = new FXMLLoader(getClass().getResource("mainUi.fxml"));
			root = loader.load();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("磁盘文件管理系统");
			initService();
			findAllViews();
			initAllViews();
			primaryStage.getIcons().add(icon);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 初始化服务，设置关闭事件的处理器
	 */
	private void initService() {
		this.diskManager = new DiskManager();
		//确认关闭窗口弹窗
		this.primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				event.consume();	//阻止消息传递
				
				Alert _alert = new Alert(Alert.AlertType.WARNING,"... ...",
						new ButtonType("取消", ButtonData.NO),
		                new ButtonType("确定", ButtonData.YES));
		        _alert.setTitle("关闭窗口？");
		        _alert.setHeaderText("确认关闭系统？");
		        _alert.initOwner(primaryStage);
		        Optional<ButtonType> _buttonType = _alert.showAndWait();

		        if(_buttonType.get().getButtonData().equals(ButtonData.YES)){
		        	diskManager.updataCatalogItems();
		            primaryStage.close();
		        }
		        else {
		            return ;
		        }
			}
		});
	}
	/**
	 * 找到所有ui控件
	 */
	@SuppressWarnings("unchecked")
	private void findAllViews() {
		sreachField = (TextField) root.lookup("#sreachField");
		sreachField.setTooltip(new Tooltip("绝对路径或相对路径，如：root/yap.dat 或 yap.dat"));
		sreachBtn = (Button) root.lookup("#sreachBtn");
		sreachField.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.ENTER) {
					sreachBtn.fireEvent(new ActionEvent());
				}
			}
		});
		
		romChart = (PieChart) root.lookup("#pieChart");
		
		totalLabel = (Label) root.lookup("#total");
		remainLabel = (Label) root.lookup("#remain");
		totalLabel.textProperty().bind(diskManager.getFAT().getTotalProperty().multiply(64).asString().concat(" KB"));
		remainLabel.textProperty().bind(diskManager.getFAT().getRemainProperty().multiply(64).asString().concat(" KB"));
		
		fileTree = (TreeView<FileInfo>) root.lookup("#fileTreeView");
		fileTree.setEditable(false);
		/*fileTree.setCellFactory( (TreeView<FileInfo> p) ->
		new FileTreeCell() );*/
		fileTree.setCellFactory(new Callback<TreeView<FileInfo>, TreeCell<FileInfo>>() {
			@Override
			public TreeCell<FileInfo> call(TreeView<FileInfo> param) {
				return new FileTreeCell(initTreeMenu());
			}
		});
		
		fatView = (TableView<FATItem>) root.lookup("#fatTable");
		
		try {
			folderIcon = new Image(new File("res/icons/folder_style1.png").toURI().toURL().toExternalForm());
			fileIcon = new Image(new File("res/icons/file_style1.png").toURI().toURL().toExternalForm());
			icon = new Image(new File("res/icons/disk_style1.png").toURI().toURL().toExternalForm());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		formatBtn = (Button) root.lookup("#formatBtn");
		aboutBtn = (Button) root.lookup("#aboutBtn");
	}
	
	/**
	 * 初始化ui控件
	 */
	private void initAllViews() {
		initTreeView();
		initChart();
		initTableView();
		
		sreachBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				//判断输入的是绝对路径还是文件/目录名
				String path = sreachField.getText();
				int chIndex = path.indexOf('/');
				if (chIndex != -1) {
					path = Util.getFullNameForAbsolute(path);
				}
				FileInfo fileInfo = diskManager.totalFiles.get(path);
				if (fileInfo == null) {
					Util.callAlert(AlertType.WARNING, "查找错误", "查找的文件/目录名格式不正确！ 请使用绝对路径或相对路径", primaryStage);
					return;
				}
				fileTree.getSelectionModel().select(fileInfo.getTag());
			}
		});
		
		formatBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {			
				Alert _alert = new Alert(AlertType.WARNING, "",
						new ButtonType("取消", ButtonData.NO),
		                new ButtonType("确定", ButtonData.YES));
		        _alert.setTitle("格式化磁盘");
		        _alert.setHeaderText("格式化磁盘，清空所有内容，系统将关闭");
		        _alert.initOwner(primaryStage);
		        Optional<ButtonType> _buttonType = _alert.showAndWait();

				if(_buttonType.get().getButtonData().equals(ButtonData.YES)){
					diskManager.formatDisk();
					primaryStage.close();
		        }
		        else {
		            return ;
		        }
			}
		});
		
		aboutBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				//显示作者信息
				Util.callAlert(AlertType.INFORMATION, "作者", "  此文件磁盘管理系统，由华南农业大学15级软件工程1班何增杰，刘孟莹开发", primaryStage);
			}
		});
	}
	
	private void initTreeView() {
		FileInfo rootDir = diskManager.totalFiles.get("root");
		TreeItem<FileInfo> rootItem = new TreeItem<>(rootDir, new ImageView(folderIcon));
		rootItem.setExpanded(true);
		rootDir.setTag(rootItem);
		fileTree.setRoot(rootItem);
		BSF_createTreeItem();
	}
	/**
	 * 构造饼图显示磁盘使用信息
	 */
	private void initChart() {
		remainArc = new PieChart.Data("剩余空间", ((128-(diskManager.totalFiles.size()+2))/128.0)*100);
		useArc = new PieChart.Data("已用空间", ((diskManager.totalFiles.size()+2)/128.0)*100);
		remainArc.pieValueProperty().bind(diskManager.getFAT().getRemainProperty().divide(128).multiply(100));
		useArc.pieValueProperty().bind(diskManager.getFAT().getUsedProperty().divide(128).multiply(100));

		ObservableList<PieChart.Data> list = FXCollections.observableArrayList(
				remainArc, useArc);
		romChart.setData(list);
		romChart.setTitle("磁盘空间使用情况");
		
		final Label caption = new Label("");
		root.getChildren().add(caption);
		caption.setTextFill(Color.PINK);
		caption.setStyle("-fx-font: 24 arial;");
		 
		for (final PieChart.Data data : romChart.getData()) {
			data.getNode().addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					caption.setVisible(true);
					caption.setTranslateX(e.getSceneX());
					caption.setTranslateY(e.getSceneY());
					caption.setText(String.format("%.2f", data.getPieValue()) + "%");
					//caption.setText(String.valueOf(data.getPieValue()) + "%");
				}
			});
			data.getNode().addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					caption.setVisible(false);

				}
			});
		}
	}
	
	private void initTableView() {
		ObservableList< TableColumn<FATItem, ?> > columns = fatView.getColumns();
		columns.get(0).setCellValueFactory(new PropertyValueFactory<>("curIndex"));
		columns.get(1).setCellValueFactory(new PropertyValueFactory<>("nextIndex"));
		fatView.setItems(diskManager.getFAT().getObservableList());
	}
	
	/**
	 * 创建文件/目录的菜单项，并且为其设置事件处理器
	 * @return	返回一个装着所有创建的菜单项的集合
	 */
	private ArrayList<MenuItem> initTreeMenu() {
		
		MenuItem addRootMenuItem = new MenuItem("新建");
		
		MenuItem openMenuItem = new MenuItem("打开文件");
		MenuItem delFileMenuItem = new MenuItem("删除");
		
		MenuItem addDirMenuItem = new MenuItem("新建");
		MenuItem delDirMenuItem = new MenuItem("删除");
		
		ArrayList<MenuItem> list = new ArrayList<>();
		list.add(addRootMenuItem);
		
		list.add(openMenuItem);
		list.add(delFileMenuItem);
		
		list.add(addDirMenuItem);
		list.add(delDirMenuItem);
		
		addRootMenuItem.setOnAction(new CreateAction());
		addDirMenuItem.setOnAction(new CreateAction());
		
		delDirMenuItem.setOnAction(new DeleteAction());
		delFileMenuItem.setOnAction(new DeleteAction());
		
		openMenuItem.setOnAction(new openAction());
		
		return list;
	}
	
	/**
	 * 广度优先搜索 从根节点找出所有节点，并初始化TreeView
	 */
	private void BSF_createTreeItem() {
		Queue<FileInfo> queue = new LinkedList<>();
		queue.offer(diskManager.totalFiles.get("root"));
		while (!queue.isEmpty()) {
			FileInfo var = queue.poll();
			if (!var.subMap.isEmpty()) {
				TreeItem<FileInfo> parentTree = var.getTag();
				Collection<FileInfo> childern = var.subMap.values();
				for (Iterator<FileInfo> iterator = childern.iterator(); iterator.hasNext();) {
					FileInfo child = iterator.next();
					TreeItem<FileInfo> childleaf = new TreeItem<FileInfo>(child);
					if (!child.isFile()) {
						childleaf.setGraphic(new ImageView(folderIcon));
						child.setTag(childleaf);
						queue.offer(child);
					} else {
						childleaf.setGraphic(new ImageView(fileIcon));
						child.setTag(childleaf);
					}
					parentTree.getChildren().add(childleaf); 	//把叶子接入父节点
				}
			}
		}
	}

	public Pane getRootPane() {
		return root;
	}
	/**
	 * 事件处理器内部类,分别为新建事件，删除事件，打开（读/写）事件
	 */
	private final class CreateAction implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent event) {
			// 获取选中的节点
			TreeItem<FileInfo> curTreeNode = fileTree.getSelectionModel().getSelectedItem();
			FileInfo curNode = curTreeNode.getValue();
			//建立传参容器
			HashMap<String, Object> params = new HashMap<>();
			//取得对话框实例
			CreateFileDialog cfd = CreateFileDialog.getInstance(primaryStage, params);
			cfd.showAndWait();
			//判断是否创建参数正确
			if (params.size() != 0) {
				String name = (String) params.get("name");
				int attr = (int) params.get("attr");
				//写入磁盘	
				int flag;
				if (params.get("type") == FileType.File)
					flag = diskManager.createFile(name, attr, 0, null, curNode);
				else 
					flag = diskManager.createDirectory(name, attr, curNode);
				//创建失败则提示用户
				if (flag < 0) {
					if (flag == -1) {	
						//存在同名同后缀文件
						Util.callAlert(AlertType.ERROR, "文件创建失败", "存在同名文件", primaryStage);
					} else if(flag == -3) {
						//磁盘空间不足
						Util.callAlert(AlertType.ERROR, "文件创建失败", "磁盘空间不足", primaryStage);
					} else if(flag == -2) {
						//超过当前文件夹的子文件数量限制
						Util.callAlert(AlertType.ERROR, "文件创建失败", "超过当前文件夹的子文件数量限制", primaryStage);
					} else if(flag == -4) {
						//文件属性如果是只读性质则不能建立
						Util.callAlert(AlertType.ERROR, "文件创建失败", "只读文件无法创建", primaryStage);
					} else {
						Util.callAlert(AlertType.ERROR, "错误", "文件创建失败", primaryStage);
					}
					
					return;
				}
				//更新treeview
				FileInfo newFile = curNode.subMap.get(name);
				TreeItem<FileInfo> childleaf = new TreeItem<FileInfo>(newFile);
				newFile.setTag(childleaf);
				if (!newFile.isFile()) {
					childleaf.setGraphic(new ImageView(folderIcon));
				} else {
					childleaf.setGraphic(new ImageView(fileIcon));
				}
				curTreeNode.getChildren().add(childleaf);
				curTreeNode.setExpanded(true);
			} else {
				return;
			}
		}
	};
	
	private final class DeleteAction implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent event) {
			// 获取选中的节点
			TreeItem<FileInfo> curTreeNode = fileTree.getSelectionModel().getSelectedItem();
			FileInfo curNode = curTreeNode.getValue();
			//判断是否为空目录，不是则无法删除
			if (!curNode.isFile() && curNode.subMap.size() != 0) {
				Util.callAlert(AlertType.WARNING, "删除失败", "该目录无法删除，请确保无子项再尝试", primaryStage);
				return;
			} else if(curNode.getAttr() == FileInfo.Attr_sysFile) {
				//提示无法删除并返回
				Util.callAlert(AlertType.ERROR, "删除失败", "系统文件无法删除，请修改属性再尝试", primaryStage);
				return;
			}
			//TreeView节点删除
			TreeItem<FileInfo> parentTreeNode = curTreeNode.getParent();
			parentTreeNode.getChildren().remove(curTreeNode);
			//删除目录项
			diskManager.deleteFileInfo(curNode);	
		}
	}
	
	private final class openAction implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent event) {
			// 获取选中的节点
			TreeItem<FileInfo> curTreeNode = fileTree.getSelectionModel().getSelectedItem();
			FileInfo curNode = curTreeNode.getValue();
			//建立传参容器
			HashMap<String, Object> params = new HashMap<>();
			//取得对话框实例
			OpenFileDialog ofd = OpenFileDialog.getInstance(primaryStage, curNode, params);
			ofd.showAndWait();
			//判断是否创建参数正确
			if (params.size() != 0) {						
				boolean alter = (boolean) params.get("alter");
				if (!alter) {	//未修改
					return;
				} else {
					String newName = (String) params.get("newName");
					if (newName != null) {
						curNode.setName(newName);
					}
					Integer newAttr = (Integer) params.get("newAttr");
					if (newAttr != null) {
						curNode.setAttr(newAttr.intValue());
					}
					String newContext = (String) params.get("newContext");
					if (newContext != null) {	//修改了内容则将原来的内容删除后，新内容写回
						boolean flag = diskManager.writeFileContent(curNode, newContext);
						if (!flag) {	//写入失败
							Util.callAlert(AlertType.ERROR, "错误", "磁盘空间不足", primaryStage);
							return;
						}
					}
				}
				
			} else {
				return;
			}
		}
	}
	
}
