package victor.training.sourcing.user.command;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SnapshotApi {
  private final EventStoreDBClient eventStore;
  public record SnapshotMetadata(long eventRevision) {}
  @PostMapping("/users/{email}/snapshot")
  public void createSnapshot(@PathVariable String email) throws Exception {
    //    User user = new User();

//    var snapshotReadResult = eventStore.readStream("snapshot-" + User.stream(email),
//        ReadStreamOptions.get().fromEnd().backwards()).get();
//    var latestSnapshotEvent = snapshotReadResult.getEvents().get(0);
//    var user = GsonUtil.parseEventData(latestSnapshotEvent.getEvent(), User.class);
//
//    var readResult = eventStore.readStream("user-" + email,
//        ReadStreamOptions.get().fromRevision(snapshotReadResult.getLastStreamPosition())).get();
//    for (var resolvedEvent : readResult.getEvents()) {
//      user.apply(GsonUtil.fromEventDataSealed(resolvedEvent.getEvent(), UserEvent.class));
//    }
//    var eventType = ((Object) user).getClass().getSimpleName();
//    var jsonString = GsonUtil.gson.toJson(user);
//    EventData snapshotEvent = EventData.builderAsJson(eventType, jsonString.getBytes())
//        .metadataAsBytes(new SnapshotMetadata())
//        .build();
//    eventStore.appendToStream("snapshot-" + User.stream(email), snapshotEvent).get();


  }

}
