package victor.training.sourcing;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import static org.springframework.boot.SpringApplication.*;

@SpringBootApplication
public class SpringApp {
  public static void main(String[] args) {
    run(SpringApp.class, args);
  }

  @Bean
  public EventStoreDBClient eventStore(@Value("${event.store.db.url}") String url) {
    return EventStoreDBClient.create(EventStoreDBConnectionString.parseOrThrow(url));
  }
}
