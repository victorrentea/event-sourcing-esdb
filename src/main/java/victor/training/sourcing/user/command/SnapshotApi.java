package victor.training.sourcing.user.command;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SnapshotApi {
  private final EventStoreDBClient eventStore;

  @PostMapping("/users/{email}/snapshot")
  public void createSnapshot(@PathVariable String email) throws Exception {
    var user = User.rebuildUser(email, eventStore);
    EventData snapshotEvent = GsonUtil.toEventData(user);
    eventStore.appendToStream(User.stream(email) + "-snapshot", snapshotEvent).get();
  }

}
