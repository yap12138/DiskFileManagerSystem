package test;

import java.util.HashMap;

import application.Main;
import entity.file.FileInfo;
import javafx.application.Application;
import javafx.stage.Stage;
import ui.CreateFileDialog;
import ui.OpenFileDialog;

public class TestDialog extends Application {

	@Override
	public void start(Stage arg0) throws Exception {
		//testCreate(arg0);
		testOpen(arg0);
		
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static void testCreate(Stage arg0) {
		HashMap<String, Object> map = new HashMap<>();
		CreateFileDialog ui = CreateFileDialog.getInstance(arg0,map);
		arg0.show();
		ui.showAndWait();
		System.out.println("7777777");
	}
	
	public static void testOpen(Stage stage) {
		HashMap<String, Object> map = new HashMap<>();
		FileInfo fileInfo = new FileInfo("rng.dat", FileInfo.Attr_readOnly, 4, 0, 0, null);
		OpenFileDialog ui = OpenFileDialog.getInstance(stage, fileInfo, map);
		stage.show();
		ui.show();
	}

}
