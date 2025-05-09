package victor.training.sourcing.user.reactor;

import com.eventstore.dbclient.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent.UserCreated;

import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class SendConfirmationEmailReactor extends SubscriptionListener {

  private final EventStoreDBClient eventStore;

  @PostConstruct
  public void subscribe() {
    eventStore.subscribeToAll(this, SubscribeToAllOptions.get().fromEnd());
  }

  @SneakyThrows
  @Override
  public void onEvent(Subscription subscription, ResolvedEvent resolvedEvent) {
    var eventOpt = GsonUtil.tryParseEvent(resolvedEvent.getEvent(), UserCreated.class);
    if (eventOpt.isEmpty()) {
      return;
    }
    log.info("Got Created event " + eventOpt.get());
    String email = User.emailFromStreamName(resolvedEvent.getEvent().getStreamId());
    String emailConfirmationToken = UUID.randomUUID().toString();
    // TODO fri
    // TODO send email
    // TODO write token in user
    var user = User.rebuildUser(email, eventStore);
    var event = user.storeEmailConfirmationToken(emailConfirmationToken);
    eventStore.appendToStream(User.stream(email), GsonUtil.toEventData(event)).get();
    log.info("Sent confirmation email");
  }

  private void sendEmail(String email, String emailConfirmationToken) {
    log.info("SMTP: Sending confirmation email to {} with token {} ...", email, emailConfirmationToken);
    if (Math.random() < 0.1) {
      // DEBATE: how to implement retries
      // DEBATE: how to implement persistent retries, if server remains down for days
      throw new RuntimeException("Email server down");
    }
  }
}
