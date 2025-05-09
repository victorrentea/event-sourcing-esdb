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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toSet;


@RestController
public class UsersThatCanLoginProjection {
  private final Projector projection;
  private final EventStoreDBClient eventStore;

  public UsersThatCanLoginProjection(EventStoreDBClient eventStore) {
    this.eventStore = eventStore;
    projection = new Projector(this.eventStore);
  }

  @Slf4j
  public static class Projector extends SubscriptionListener {
    // TODO: Business Rule: a user can login if it's active and its email was confirmed
    // - a user that did not YET confirmed its email address cannot login
    // - a user that was banned (active=false) cannot login

    // for performance you can memoize this data in a cache =>
    // make the read projection persistent eg in Redis/Mongo/SQL
    private Set<String> activeUsers = new HashSet<>();
    private Set<String> confirmedUsers = new HashSet<>();

    public Projector(EventStoreDBClient eventStore) {
      eventStore.subscribeToAll(this, SubscribeToAllOptions.get()
          .filter(SubscriptionFilter.newBuilder().addStreamNamePrefix("user-").build())
          .fromStart());
    }

    public Projector(EventStoreDBClient eventStore, Long asOfPosition) throws ExecutionException, InterruptedException {
      var done = new CompletableFuture<>();

      eventStore.subscribeToAll(new SubscriptionListener() {
        @Override
        public void onEvent(Subscription subscription, ResolvedEvent event) {
          System.out.println(event.getEvent().getPosition().getCommitUnsigned());
          if (event.getEvent().getPosition().getCommitUnsigned() > asOfPosition)
            return; // ignore later
          Projector.this.onEvent(subscription,event);
        }
        @Override
        public void onCaughtUp(Subscription subscription) {
          done.complete("Done");
        }
      }, SubscribeToAllOptions.get().fromStart());
      done.get();
    }

    public Projector(EventStoreDBClient eventStore, Instant asOfInstant) throws ExecutionException, InterruptedException {
      var done = new CompletableFuture<>();

      eventStore.subscribeToAll(new SubscriptionListener() {
        @Override
        public void onEvent(Subscription subscription, ResolvedEvent event) {
          System.out.println("Saw " + event);
          if (!event.getEvent().getCreated().isAfter(asOfInstant)) {
            Projector.this.onEvent(subscription, event);
          }
        }

        @Override
        public void onCaughtUp(Subscription subscription) {
          done.complete("Done");
        }
      }, SubscribeToAllOptions.get().fromStart());
      done.get();
    }

    @Override
    public void onEvent(Subscription subscription, ResolvedEvent resolvedEvent) {
      var streamId = resolvedEvent.getEvent().getStreamId();
      String email = User.emailFromStreamName(streamId);
      UserEvent event = GsonUtil.fromEventDataSealed(resolvedEvent.getEvent(), UserEvent.class);
//      log.info("Processing {} > {}",email, event);
      switch(event) {
        case UserEvent.UserActivated ignored -> activeUsers.add(email);
        case UserEvent.UserDeactivated ignored -> activeUsers.remove(email);
        case UserEvent.UserEmailConfirmed ignored -> confirmedUsers.add(email);
        default -> {/*ignored*/}
      }
    }

    private Set<String> getUsersThatCanLogin() {
      return confirmedUsers.stream()
          .filter(activeUsers::contains)
          .collect(toSet());
    }
  }

  //    @GetMapping("/users/{email}/can-login") >> to easy: hydrate the user > done
  // TODO a better: a searhc: what users with name j* can login
  @GetMapping("/users-to-login")
  public Set<String> getUsersToLogin(
      @RequestParam(required = false) Long asOfPosition,
      @RequestParam(required = false) String asOfTime
  ) throws ExecutionException, InterruptedException {
    if (asOfPosition != null) {
      var projection = new Projector(eventStore, asOfPosition);
      return projection.getUsersThatCanLogin();
    }
    if (asOfTime != null) {
      var instant = Instant.parse(asOfTime);
      return new Projector(eventStore, instant).getUsersThatCanLogin();
    }
    return projection.getUsersThatCanLogin();
  }



}



