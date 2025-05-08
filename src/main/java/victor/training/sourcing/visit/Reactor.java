package victor.training.sourcing.visit;

import com.eventstore.dbclient.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Reactor {
  private final EventStoreDBClient eventStore;

  @EventListener(ApplicationStartedEvent.class)
  public void subscribe() {
    eventStore.subscribeToAll(new SubscriptionListener() {
      @Override
      public void onEvent(Subscription subscription, ResolvedEvent event) {
        System.out.println("Got: " + event);
      }
    }, SubscribeToAllOptions.get().fromEnd());
  }
}
