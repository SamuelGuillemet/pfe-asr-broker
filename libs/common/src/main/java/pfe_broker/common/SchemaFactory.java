package pfe_broker.common;

import io.micronaut.context.annotation.Context;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.utils.SchemaRecord;

@Context
public class SchemaFactory {

  private static final Logger LOG = LoggerFactory.getLogger(
    SchemaFactory.class
  );

  public SchemaFactory(
    @NonNull List<SchemaRecord> schemas,
    @Client(value = "${kafka.schema.registry.url}") HttpClient httpClient
  ) {
    LOG.info("Registering schemas");
    schemas.forEach(schemaRecord -> {
      String schemaString = schemaRecord
        .schema()
        .toString()
        .replaceAll("\"", "\\\\\"");
      String subjectName = schemaRecord.topicName() + "-value";

      String contentType = "application/vnd.schemaregistry.v1+json";

      // Verify if the schema already exists and is the same
      HttpRequest<String> requestGet = HttpRequest.GET(
        "/subjects/" + subjectName + "/versions/latest"
      );

      try {
        String response = httpClient.toBlocking().retrieve(requestGet);
        if (response.contains(schemaString)) {
          LOG.info("Schema already registered with latest version");
          return;
        }
      } catch (Exception e) {
        // Do nothing
      }

      HttpRequest<String> request = HttpRequest
        .POST(
          "/subjects/" + subjectName + "/versions",
          "{\"schema\": \"" + schemaString + "\"}"
        )
        .header("Content-Type", contentType);

      try {
        httpClient.toBlocking().retrieve(request);
      } catch (Exception e) {
        LOG.error("Error while registering schema: " + e.getMessage());
      }
    });
  }
}