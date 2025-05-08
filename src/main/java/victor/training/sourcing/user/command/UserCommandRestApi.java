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
import java.util.concurrent.ExecutionException;

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
  public void createUser(@RequestBody @Validated CreateUserRequest request) {
    List<UserEvent> events = User.create(request);
    var streamId = User.getStreamName(request.email());
    eventStore.appendToStream("user-"+ request.email(),
        AppendToStreamOptions.get().expectedRevision(ExpectedRevision.noStream()), // Unique-Key in Event-Sourcing
        events.stream().map(GsonUtil::toEventData).iterator()); // atomic write iff the streamId is not present
    // TODO discuss:
    //  a) UserCreated{roles} = coarse, mapped to domain action (publisher convenience) 💖
    //  b) UserRolesGranted{roles} = coarse-grained aggregated event
    //  c) UserRoleGranted{role} = fine-grained events (listener convenience)
  }

  @PutMapping("/{email}/confirm-email")
  public void confirmEmail(@PathVariable String email, @RequestParam String token) throws ExecutionException, InterruptedException {
    // === § PART 1 : read the aggregate ===
    // ❌Traditional: read current state from a DB:
    // User user = userRepo.findById(email);

    // ✅Event-Sourced: replay the events about this aggregate
    // 1) get events
    var readResult = eventStore.readStream(User.getStreamName(email), ReadStreamOptions.get().fromStart()).get();

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

//    eventStore.appendToStream(User.getStreamName(user.email()), events.stream().map(GsonUtil::toEventData).iterator());
    for (var event : events) {
      var eventData = GsonUtil.toEventData(event);
      eventStore.appendToStream(User.getStreamName(user.email()),
//          AppendToStreamOptions.get().expectedRevision(ExpectedRevision.any()), // concurrency protection via optimistic locking
          eventData).get();
    }
  }

  public record UpdateUserRequest(
      @Email String email,
      @NotBlank String name,
      @NotNull String departmentId
  ) {
  }

  @PutMapping("/{userId}")
  public void update(@PathVariable String userId, @RequestBody UpdateUserRequest request) {
    //TODO
  }

  @PutMapping("/{userId}/roles/{role}")
  public void assignRole(@PathVariable String userId, @PathVariable String role) {
    // TODO
  }

  @DeleteMapping("/{userId}/roles/{role}")
  public void revokeRole(@PathVariable String userId, @PathVariable String role) {
    // TODO
  }

  @PutMapping("/{userId}/activate")
  public void activate(@PathVariable String userId) {
    // TODO
  }

  @PutMapping("/{userId}/deactivate")
  public void deactivate(@PathVariable String userId) {
    // TODO
  }

}
