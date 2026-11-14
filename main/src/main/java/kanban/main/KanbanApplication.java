package kanban.main;

import kanban.boardmanagement.frameworks.web.BoardManagementRequests;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main 元件的進入點（CA-8）。
 * 內三圈沒有任何 Spring 註解，所以只掃描 Main 自己與最外圈的 HTTP 薄殼；其餘物件都在 BoardManagementWiring 用 new 建立。
 */
@SpringBootApplication(scanBasePackageClasses = {KanbanApplication.class, BoardManagementRequests.class})
public class KanbanApplication {

    public static void main(String[] args) {
        SpringApplication.run(KanbanApplication.class, args);
    }
}
