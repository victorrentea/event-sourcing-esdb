package victor.training.sourcing.user.reactor;

import com.eventstore.dbclient.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.UserEvent.UserActivated;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PublishActivatedUsersOnKafkaReactor extends SubscriptionListener {
  private final EventStoreDBClient eventStore;

  @PostConstruct
  public void subscribe() {
    eventStore.subscribeToAll(this, SubscribeToAllOptions.get().fromEnd());
  }

  @Override
  public void onEvent(Subscription subscription, ResolvedEvent resolvedEvent) {
    var eventOpt = GsonUtil.tryParseEvent(resolvedEvent.getEvent(), UserActivated.class);
    if (eventOpt.isEmpty()) {
      return;
    }
    // Dual write to ESDB + Kafka:
    // a) we wrote to ESDB then in reaction try to push to Kafka <- current solution (pretend)
    // b) CDC/Debezium poll ESDB and sends to Kafka events
    // c) make Kafka the source of truth and project to ESDB = moving out
    log.info("kafka.send(User '{}' activated)", eventOpt.get().email());
  }
}
