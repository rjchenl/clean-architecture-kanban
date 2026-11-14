package kanban.main;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 整合測試共用：用真正的 HTTP 打啟動起來的應用程式。 */
abstract class HttpTestSupport {

    record Reply(int status, JsonNode body) {
        String text(String field) {
            return body.get(field).asString();
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;

    Reply get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    Reply post(String path, String json) {
        return send(withJson(HttpRequest.newBuilder(uri(path)), "POST", json));
    }

    Reply put(String path, String json) {
        return send(withJson(HttpRequest.newBuilder(uri(path)), "PUT", json));
    }

    Reply delete(String path) {
        return send(HttpRequest.newBuilder(uri(path)).DELETE());
    }

    /** 建立看板並回傳它的識別碼。 */
    String createBoard(String name) {
        return post("/api/boards", "{\"name\":\"" + name + "\"}").text("createdId");
    }

    /** 新增欄位並回傳它的識別碼。 */
    String addColumn(String boardId, String name) {
        return post("/api/boards/" + boardId + "/columns", "{\"name\":\"" + name + "\"}").text("createdId");
    }

    /** 新增卡片並回傳它的識別碼。 */
    String addCard(String boardId, String columnId, String title) {
        return post("/api/boards/" + boardId + "/cards",
                "{\"columnId\":\"" + columnId + "\",\"title\":\"" + title + "\",\"description\":\"\"}")
                .text("createdId");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static HttpRequest.Builder withJson(HttpRequest.Builder builder, String method, String json) {
        HttpRequest.BodyPublisher body = json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
        return builder.method(method, body).header("Content-Type", "application/json");
    }

    private Reply send(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = client.send(request.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            return new Reply(response.statusCode(), body == null || body.isBlank() ? null : JSON.readTree(body));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
