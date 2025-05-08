package victor.training.sourcing.user.domain;

import lombok.Data;
import victor.training.sourcing.AbstractEvent;

import java.time.LocalDateTime;

public sealed abstract class UserEvent extends AbstractEvent
    permits
    UserEvent.UserCreated,
    UserEvent.ConfirmationEmailSent,
    UserEvent.UserActivated,
    UserEvent.UserEmailConfirmed,
    UserEvent.UserEmailUpdated,
    UserEvent.UserLoggedIn,
    UserEvent.UserUpdated {

  @Data
  public static final class UserCreated extends UserEvent {
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String departmentId;
  }

  @Data
  public static final class ConfirmationEmailSent extends UserEvent {
    private String emailConfirmationToken;
  }

  @Data
  public static final class UserActivated extends UserEvent {
    private String email;
  }

  @Data
  public static final class UserEmailUpdated extends UserEvent {
    private String email;
  }

  @Data
  public static final class UserEmailConfirmed extends UserEvent {
    // via click on link in email
    // TODO race: change emails twice (first: incorrect, second: correct)
  }


  @Data
  public static final class UserLoggedIn extends UserEvent {
    private String application;
    private LocalDateTime loginTime;
  }

  @Data
  public static final class UserUpdated extends UserEvent {
    private String name;
    private String departmentId;
  }
}
