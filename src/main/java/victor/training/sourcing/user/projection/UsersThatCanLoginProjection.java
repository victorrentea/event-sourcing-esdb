package victor.training.sourcing.user.projection;

import com.eventstore.dbclient.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class UsersThatCanLoginProjection extends SubscriptionListener {
  // a user can login if it's active and its email was confirmed

  public UsersThatCanLoginProjection(EventStoreDBClient eventStore) {
    eventStore.subscribeToAll(this, SubscribeToAllOptions.get().fromStart());
  }

  public UsersThatCanLoginProjection(EventStoreDBClient eventStore, Long asOfPosition) throws ExecutionException, InterruptedException {
    var done = new CompletableFuture<>();

    eventStore.subscribeToAll(new SubscriptionListener() {
      @Override
      public void onEvent(Subscription subscription, ResolvedEvent event) {
        System.out.println(event.getEvent().getPosition().getCommitUnsigned());
        if (event.getEvent().getPosition().getCommitUnsigned() > asOfPosition)
          return; // ignore later
        UsersThatCanLoginProjection.this.onEvent(subscription,event);
      }
      @Override
      public void onCaughtUp(Subscription subscription) {
        done.complete("Done");
      }
    }, SubscribeToAllOptions.get().fromStart()).get();
    done.get();
  }

  public UsersThatCanLoginProjection(EventStoreDBClient eventStore, Instant asOfInstant) throws ExecutionException, InterruptedException {
    var done = new CompletableFuture<>();

    eventStore.subscribeToAll(new SubscriptionListener() {
      @Override
      public void onEvent(Subscription subscription, ResolvedEvent event) {
        if (event.getEvent().getCreated().isAfter(asOfInstant))
          return; // ignore later
        UsersThatCanLoginProjection.this.onEvent(subscription,event);
      }
      @Override
      public void onCaughtUp(Subscription subscription) {
        done.complete("Done");
      }
    }, SubscribeToAllOptions.get().fromStart()).get();
    done.get();
  }

  @Override
  public void onEvent(Subscription subscription, ResolvedEvent resolvedEvent) {
    var streamId = resolvedEvent.getEvent().getStreamId();
    if (!streamId.startsWith("user-")) {
      return;
    }
    String email = User.getEmailFromStreamId(streamId);
    var userEvent = GsonUtil.fromEventDataSealed(resolvedEvent.getEvent(), UserEvent.class);
  }

  private Set<String> getUsersThatCanLogin() {
    return null;//.TODO;
  }

  @RestController
  public static class ProjectionView {
    private final UsersThatCanLoginProjection projection;
    private final EventStoreDBClient eventStore;

    public ProjectionView(EventStoreDBClient eventStore) {
      this.eventStore = eventStore;
      projection = new UsersThatCanLoginProjection(this.eventStore);
    }

    // http://localhost:8080/users-to-login
    @GetMapping("/users-to-login")
    public Set<String> getUsersToLogin(
        @RequestParam(required = false) Long position,
        @RequestParam(required = false) String time
      ) throws ExecutionException, InterruptedException {
      if (position != null) {
        var projection = new UsersThatCanLoginProjection(eventStore, position);
        return projection.getUsersThatCanLogin();
      }
      if (time != null) {
        var instant = Instant.parse(time);
        return new UsersThatCanLoginProjection(eventStore, instant).getUsersThatCanLogin();
      }
      return projection.getUsersThatCanLogin();
    }

  }


}
