import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import com.nexora.bank.SceneRouter;

public class FxmlProbe {
    public static void main(String[] args) {
        Platform.startup(() -> {});
        try {
            var url = SceneRouter.class.getResource("/fxml/UserDashboard.fxml");
            System.out.println("URL=" + url);
            FXMLLoader loader = new FXMLLoader(url);
            loader.load();
            System.out.println("LOAD_OK");
        } catch (Throwable t) {
            t.printStackTrace();
            Throwable c = t.getCause();
            while (c != null) {
                System.out.println("CAUSE=> " + c.getClass().getName() + ": " + c.getMessage());
                c = c.getCause();
            }
            System.exit(1);
        } finally {
            Platform.exit();
        }
    }
}
