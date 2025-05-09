package victor.training.sourcing.user.projection;

import com.eventstore.dbclient.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent.UserLoggedIn;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByValue;

@Slf4j
public class LastLoginTimeProjection extends SubscriptionListener {
  // {app: {userId: lastLoginTime}}
  private final Map<String, Map<String, LocalDateTime>> lastLoginPerApplication = new HashMap<>();

  public LastLoginTimeProjection(EventStoreDBClient eventStore) {
    eventStore.subscribeToAll(this, SubscribeToAllOptions.get().fromStart());
  }

  @Override
  public void onEvent(Subscription subscription, ResolvedEvent resolvedEvent) {
    var eventOpt = GsonUtil.tryParseEvent(resolvedEvent.getEvent(), UserLoggedIn.class);
    if (eventOpt.isEmpty()) {
      return;
    }
    var event = eventOpt.get();
    Map<String, LocalDateTime> logins = lastLoginPerApplication.computeIfAbsent(event.application(), k -> new HashMap<>());
    var streamId = resolvedEvent.getEvent().getStreamId();
    var email = User.emailFromStreamName(streamId);
    logins.put(email, event.loginTime());
  }

  private List<LastLoginTimeProjectionView.LastLoginTimeResponse> getLastLoginPerApp(String application) {
    Map<String, LocalDateTime> logins = this.lastLoginPerApplication.getOrDefault(application, Map.of());
    return logins.entrySet().stream()
        .sorted(comparingByValue(reverseOrder()))
        .map(e -> new LastLoginTimeProjectionView.LastLoginTimeResponse(e.getKey(), e.getValue()))
        .toList();
  }

  @RestController
  public static class LastLoginTimeProjectionView {
    private final LastLoginTimeProjection projection;

    public LastLoginTimeProjectionView(EventStoreDBClient eventStore) {
      projection = new LastLoginTimeProjection(eventStore);
    }

    public record LastLoginTimeResponse(String userId, LocalDateTime loginTime) {}

    @GetMapping("logins/{application}")
    public List<LastLoginTimeResponse> getLastLogins(@PathVariable String application) {
      return projection.getLastLoginPerApp(application);
    }

  }


}
