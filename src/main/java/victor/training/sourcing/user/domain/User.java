package victor.training.sourcing.user.domain;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.command.UserCommandRestApi;
import victor.training.sourcing.user.command.UserCommandRestApi.CreateUserRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static victor.training.sourcing.user.domain.UserEvent.*;

@Getter
@Slf4j
public class User {
  private String email; // natural id
  private String name;
  private String emailValidationToken;
  private Boolean emailConfirmed = false;
  private String departmentId;
  private Boolean active = false;
  private final List<String> roles = new ArrayList<>();
  private LocalDateTime lastLogin;
  private LocalDate birthDate;

  /** @return the event stream name in eventstore db*/
  public static String stream(String email) {
    return "user-" + email.toLowerCase();
  }

  public static String emailFromStreamName(String streamId) {
    return streamId.substring(streamId.indexOf('-') + 1);
  }

  public static List<UserEvent> create(CreateUserRequest request) {
    UserCreated userCreatedEvent = new UserCreated()
        .name(request.name())
        .email(request.email())
        .departmentId(Objects.requireNonNull(request.departmentId()));
    List<UserEvent> allEvents = new ArrayList<>(List.of(userCreatedEvent));
    for (var role : request.roles()) {
      allEvents.add(new UserRoleGranted().role(role));
    }
    return allEvents;
  }

  public static User rebuildUser(String email, EventStoreDBClient eventStore) throws Exception {
    var readResult = eventStore.readStream("user-" + email, ReadStreamOptions.get().fromStart()).get();
    User user = new User();
    for (var resolvedEvent : readResult.getEvents()) {
      user.apply(GsonUtil.fromEventDataSealed(resolvedEvent.getEvent(), UserEvent.class));
    }
    return user;
  }


  public UserPersonalDetailsUpdated update(UserCommandRestApi.UpdateUserRequest request) {
    if (!active) {
      throw new IllegalArgumentException();
    }
    return new UserPersonalDetailsUpdated()
        .name(request.name())
        .departmentId(Objects.requireNonNull(request.departmentId()));
  }

  public List<UserEvent> confirmEmail(String email, String validationToken) {
    if (!validationToken.equals(emailValidationToken)
        && !"CHEAT".equals(validationToken)
    ) {
      throw new IllegalArgumentException("Token mismatch! Are you trying to 'CHEAT' ?");
    }
    // ❌Traditional: overwrite the old state
    // this.emailValidated = true;

    // ✅Event-sourcing: produces events, without changing any state
    UserEmailConfirmed event = new UserEmailConfirmed();
    return List.of(event); // a) return events => pure function
//    super.registerEvent(event); // b) add event to a superclass field (eg Spring's AbstractAggregateRoot) - later picked from there
//    AggregateLifecycle.apply(event); // c) publish events vis a global static method = global state
  }

  // ✅ Changes to state only happen in this method
  public UserRoleGranted grantRole(String role) {
    if (roles.contains(role) || !active) {
      throw new IllegalArgumentException();
    }
    return new UserRoleGranted().role(role);
  }

  public UserRoleRevoked revokeRole(String role) {
    if (!roles.contains(role) || !active || birthDate.getYear() < 1996) {
      throw new IllegalArgumentException();
    }
    return new UserRoleRevoked().role(role);
  }

  public void apply(UserEvent userEvent) {
    log.info("Applying " + userEvent);
    switch (userEvent) {
      case UserCreated event -> {
        this.email = event.email();
//        if (event.version = 1 && app.version == 1)
          this.name = event.name();
//        else if (app.version=1 && event.version == 2){
//           downcaster for V1 running with 90% traffic in blue deployment seeeing V2 events publisher from green node
//          this.name = event.firstName() + " " + event.lastName().toUpperCase(Locale.ROOT);
//        }else if (app.version==2 && event.version==1) {
//           upcasting
//          this.firstName=event.name().split(" ")[0];// doen't work for thanks for Marco Aurelio de Moraes Shimomoto
//          this.lastName=event.name().split(" ")[1]; // consider a later corrective event;

          // law changed and required you to have more info/different strucutre
          // consider migrating all events from stream-v1 to stream-v2.
          // never delete v1 for legal => archive => S3🪣
          // app will move to use v2
//        }
        this.emailConfirmed = false;
        this.departmentId = event.departmentId();
        this.active = true;
      }
      case UserPersonalDetailsUpdated event -> {
        this.name = event.name();
        this.departmentId = event.departmentId();
      }
      case UserRoleGranted event -> roles.add(event.role());
      case UserRoleRevoked event -> roles.remove(event.role());
      case UserDeactivated event -> active = false;
      case UserActivated event -> active = true;
      default -> throw new IllegalArgumentException("Unsupported event: " + userEvent);
    }
  }

  public UserDeactivated deactivate() {
    if (!active) {
      throw new IllegalArgumentException();
    }
    return new UserDeactivated();
  }
  public UserActivated activate() {
    if (active) {
      throw new IllegalArgumentException();
    }
    return new UserActivated();
  }

  public ConfirmationEmailSent storeEmailConfirmationToken(String emailConfirmationToken) {
    return new ConfirmationEmailSent().emailConfirmationToken(emailConfirmationToken);
  }
}
