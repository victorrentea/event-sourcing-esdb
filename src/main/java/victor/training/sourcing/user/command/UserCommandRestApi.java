package victor.training.sourcing.user.command;

import com.eventstore.dbclient.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent;

import java.util.List;

import static com.eventstore.dbclient.ExpectedRevision.noStream;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCommandRestApi {
  private final EventStoreDBClient eventStore;

  @With
  public record CreateUserRequest(
      @Email String email,
      @NotBlank String name,
      @NotNull String departmentId,
      List<String> roles
  ) {
  }

  @PostMapping
  public void createUser(@RequestBody @Validated CreateUserRequest request) throws Exception {
    List<UserEvent> events = User.create(request);
    eventStore.appendToStream(User.stream(request.email()),
            AppendToStreamOptions.get().expectedRevision(noStream()), // Unique-Key in Event-Sourcing
            events.stream().map(GsonUtil::toEventData).iterator())
        .get();
    log.info("Created user");
    // TODO discuss: UserCreated💖{..,roles}, +UserRolesGranted{roles}, +UserRoleGranted{role}
  }

  @PutMapping("/{email}/confirm-email")
  public void confirmEmail(@PathVariable String email, @RequestParam String token) throws Exception {
    // === § PART 1 : read the aggregate ===
    // ❌Traditional: read current state from a DB:
    // User user = userRepo.findById(email);

    // ✅Event-Sourced: replay the events about this aggregate
    // 1) get events
    var readResult = eventStore.readStream(User.stream(email), ReadStreamOptions.get().fromStart()).get();

    // 2a) rebuild user from events : imperative-style
    User user = new User();
    for (ResolvedEvent re : readResult.getEvents()) {
      UserEvent event = GsonUtil.fromEventDataSealed(re.getEvent(), UserEvent.class);
      user.apply(event);
    }

    // 2b) rebuild user from events : functional-style
//    var user = userEvents.reduce(new User(),
//            (u, event) -> u.apply(event), // returns new new user
//            (u1, u2) -> {throw new IllegalArgumentException();}); // N/A as never parallelStream

    // === § PART 2 : apply the command on the aggregate ===
    // ❌Traditional: replace the old data
    // user.setEmailConfirmed(true); // you loose WHO, WHEN, IN WHAT ORDER
    // userRepo.save(user)

    // ❓audit columns
    // user.setLastModifiedBy(currentUser);

    // ❓explicit audit table
    // auditRepo.save(new Audit("user-confirmed-email",userId, currentUser,now(),"{......}"))

    // ❓automatic audit table using Hibernate Envers

    // ❓time-traveling queries: SELECT .. AS OF TIMESTAMP ...

    // ✅Event-Sourced: all changes happen via events
    // 1) command produces events
    List<UserEvent> events = user.confirmEmail(email, token);
    // 2) persist events = source of truth

    for (var event : events) {
      var eventData = GsonUtil.toEventData(event);
      eventStore.appendToStream(User.stream(user.email()),
//          AppendToStreamOptions.get().expectedRevision(ExpectedRevision.streamExists()),
          AppendToStreamOptions.get().expectedRevision(readResult.getLastStreamPosition()), // concurrency protection ~ optimistic locking
          eventData).get();
    }
  }

  public record UpdateUserRequest(
      @NotBlank String name,
      @NotNull String departmentId
  ) {
  }

  @PutMapping("/{email}/details")
  public void update(@PathVariable String email, @RequestBody @Validated UpdateUserRequest request) throws Exception {
    var user = User.rebuildUser(email, eventStore);
    var event = user.update(request);
    eventStore.appendToStream(User.stream(email),
        AppendToStreamOptions.get().expectedRevision(ExpectedRevision.streamExists()),// this guarantees the stream exists
        GsonUtil.toEventData(event)).get();
  }

  @PutMapping("/{email}/roles/{role}")
  public void grantRole(@PathVariable String email, @PathVariable String role) throws Exception {
    var user = User.rebuildUser(email, eventStore);
    var event = user.grantRole(role);
    eventStore.appendToStream(User.stream(email), GsonUtil.toEventData(event)).get();
  }

  @DeleteMapping("/{email}/roles/{role}")
  public void revokeRole(@PathVariable String email, @PathVariable String role) throws Exception {
    var user = User.rebuildUser(email, eventStore);
    var event = user.revokeRole(role);
    eventStore.appendToStream(User.stream(email), GsonUtil.toEventData(event)).get();
  }

  @PutMapping("/{email}/deactivate")
  public void deactivate(@PathVariable String email) throws Exception {
    var user = User.rebuildUser(email, eventStore);
    var event = user.deactivate();
    eventStore.appendToStream(User.stream(email), GsonUtil.toEventData(event)).get();
  }

  @PutMapping("/{email}/activate")
  public void activate(@PathVariable String email) throws Exception {
    var user = User.rebuildUser(email, eventStore);
    var event = user.activate();
    eventStore.appendToStream(User.stream(email), GsonUtil.toEventData(event)).get();
  }

}
