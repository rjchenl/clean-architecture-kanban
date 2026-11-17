package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * 正式設定檔的排程開關寫成 YAML 的 true，轉成字串必須是 "true"，才對得上 havingValue = "true"。
 * 排程測試的開關是行內帶入的字串，證明不到這件事，所以另外讀正式設定檔本身。
 */
class ApplicationYamlTest {

    @Test
    void 正式設定檔的排程開關是字串true() throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));

        assertThat(String.valueOf(sources.getFirst().getProperty("kanban.flow-analysis.background"))).isEqualTo("true");
    }
}
