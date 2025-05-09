package victor.training.sourcing.user.projection;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.ResolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GetUserByIdProjection {
  private final EventStoreDBClient eventStore;

  public record GetUserResponse(
      String email,
      String name,
      String departmentId,
      List<String> roles,
      Boolean emailValidated,
      Boolean active
  ) {
    public static GetUserResponse fromUser(User user) {
      return new GetUserResponse(user.email(), user.name(), user.departmentId(), user.roles(), user.emailConfirmed(), user.active());
    }
  }


  @GetMapping("users/{email}")
  public GetUserResponse getUser(@PathVariable String email) throws ExecutionException, InterruptedException {
    var readResult = eventStore.readStream("user-" + email, ReadStreamOptions.get().fromStart()).get();

    User user = new User();
    for (var resolvedEvent : readResult.getEvents()) {
      user.apply(GsonUtil.fromEventDataSealed(resolvedEvent.getEvent(), UserEvent.class));
    }
    return GetUserResponse.fromUser(user);
  }
}
