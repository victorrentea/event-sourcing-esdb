package victor.training.sourcing.user.domain;

import lombok.Getter;
import victor.training.sourcing.user.command.UserCommandRestApi.CreateUserRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static victor.training.sourcing.user.domain.UserEvent.*;

@Getter
public class User {
  private String email; // natural id
  private String name;
  private String emailValidationToken;
  private Boolean emailConfirmed = false;
  private String departmentId;
  private Boolean active = false;
  private final List<String> roles = new ArrayList<>();
  private LocalDateTime lastLogin;

  public static String getStreamName(String email) {
    return "user-"+email.toLowerCase();
  }

  public static String getEmailFromStreamId(String streamId) {
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
  public void apply(UserEvent userEvent) {
    switch (userEvent) {
      case UserCreated event -> {
        this.email = event.email();
        this.name = event.name();
        this.emailConfirmed = false;
        this.departmentId = event.departmentId();
        this.active = true;
      }
      case UserRoleGranted event -> {
        roles.add(event.role());
      }

      case UserPersonalDetailsUpdated event -> { //TODO name too broad.
        this.name = event.name();
        this.departmentId = event.departmentId();
      }
      default -> throw new IllegalArgumentException();
    }
  }

}
