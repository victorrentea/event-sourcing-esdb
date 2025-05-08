package victor.training.sourcing.user.domain;

import lombok.Getter;
import victor.training.sourcing.user.command.UserCommandRestApi.CreateUserRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    List<UserRoleGranted> rolesGrantedEvent = request.roles().stream()
        .map(role -> new UserRoleGranted().role(role))
        .toList();

    return Stream.concat(
        Stream.of(userCreatedEvent),
        rolesGrantedEvent.stream()
    ).toList();
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
      case UserUpdated event -> {
        this.name = event.name();
        this.departmentId = event.departmentId();
      }
      case UserEmailUpdated event -> {
        this.email = event.email();
        this.emailConfirmed = false;
      }
      case UserEmailConfirmed event -> {
//        if (!event.email().equals(email)) { // TODO discuss: when to validate?
//          throw new IllegalArgumentException("Email mismatch: " + event.email() + " vs " + email);
//        }
        this.emailConfirmed = true;
      }
      case UserDeactivated ignored -> active = false;
      case UserActivated ignored -> active = true;
      case UserRoleGranted event -> roles.add(event.role());
      case UserRoleRevoked event -> roles.remove(event.role());
      case UserLoggedIn event -> this.lastLogin = event.loginTime();
      case ConfirmationEmailSent event -> this.emailValidationToken = event.emailConfirmationToken();
    }
  }

}
