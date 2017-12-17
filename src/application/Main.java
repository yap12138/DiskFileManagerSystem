package application;
	
import javafx.application.Application;
import javafx.stage.Stage;
import ui.MainUi;


public class Main extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		try {
			MainUi primaryUi = new MainUi(primaryStage);
			primaryStage.show();
			primaryStage.setMinWidth(primaryStage.getWidth());
			primaryStage.setMinHeight(primaryStage.getHeight());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
