package victor.training.sourcing.user.reactor;

import com.eventstore.dbclient.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent.ConfirmationEmailSent;
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

  @Override
  public void onEvent(Subscription subscription, ResolvedEvent resolvedEvent) {
    var eventOpt = GsonUtil.tryParseEvent(resolvedEvent.getEvent(), UserCreated.class);
    if (eventOpt.isEmpty()) {
      return;
    }
    String emailConfirmationToken = UUID.randomUUID().toString();
    var email = eventOpt.get().email();
    sendEmail(email, emailConfirmationToken);

    eventStore.appendToStream(User.getStreamName(email), GsonUtil.toEventData(
        new ConfirmationEmailSent()
            .emailConfirmationToken(emailConfirmationToken)));
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
