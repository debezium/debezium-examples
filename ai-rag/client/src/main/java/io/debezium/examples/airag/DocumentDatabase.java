package io.debezium.examples.airag;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DocumentDatabase {

    static final String SAMPLE_ARXIV_DOCUMENT = "2504.05309v1";

    @ConfigProperty(name = "debezium.rag.demo.document.truncate", defaultValue = "512")
    int documentSize;

    @Inject
    DataSource dataSource;

    public void init() throws Exception {
        try (final var conn = dataSource.getConnection()) {
            conn.createStatement().execute("TRUNCATE TABLE ai.documents");
        }
    }

    public void delete(String arXivId) throws Exception {
        final var sql = "DELETE FROM ai.documents WHERE id = ?";

        try (final var conn = dataSource.getConnection();
                final var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, arXivId);
            stmt.executeUpdate();
        }
    }

    public void insert(String arXivId) throws Exception {
        final var sql = "INSERT INTO ai.documents VALUES (?, ?::json, ?)";

        try (final var conn = dataSource.getConnection();
                final var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, arXivId);
            stmt.setString(2, String.format("""
                {
                    "id": "%s"
                }""", arXivId));
            stmt.setString(3, downloadArXiv(arXivId).substring(0, documentSize));
            stmt.executeUpdate();

        }
    }

    private String downloadArXiv(String arXivId) throws Exception {
        final var client = HttpClient.newHttpClient();

        final var request = HttpRequest.newBuilder().uri(URI.create("https://www.arxiv-txt.org/raw/abs/%s".formatted(arXivId)))
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        }
        else {
            throw new Exception("Failed to download with HTTP code " + response.statusCode());
        }
    }
}
