package victor.training.sourcing.visit;

import com.eventstore.dbclient.*;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
public class VisitorsRestApi {
  private static final String VISITORS_STREAM = "visitors-stream";
  public static final Gson gson = new Gson();
  private final EventStoreDBClient eventStore;

  @GetMapping("/hello-world")
  public String sayHello(@RequestParam(name = "visitor", required = false, defaultValue = "Visitor") String visitor)
      throws ExecutionException, InterruptedException {
    VisitorGreetedEvent visitorGreetedEvent = new VisitorGreetedEvent(visitor);
    var eventJsonString = gson.toJson(visitorGreetedEvent);
    EventData event = EventData.builderAsJson("VisitorGreeted", eventJsonString.getBytes())
//        .metadataAsBytes(gson.toJson(new EventMetadata().authorUsername("jdoe")).getBytes())
        .build();

    WriteResult writeResult = eventStore.appendToStream(VISITORS_STREAM, event).get();

    eventStore.appendToStream(VISITORS_STREAM+"-link",
        EventData.builderAsJson("$>", "%s@%s".formatted(writeResult.getLogPosition().getCommitUnsigned(), VISITORS_STREAM).getBytes()).build()).get();




    ReadResult eventStream = eventStore.readStream(VISITORS_STREAM,
            ReadStreamOptions.get().fromStart())
        .get();

    List<String> visitorsGreeted = new ArrayList<>();
    for (ResolvedEvent re : eventStream.getEvents()) {
      var recordedEvent = re.getOriginalEvent();
      String json = new String(recordedEvent.getEventData());
      VisitorGreetedEvent greetedEvent = gson.fromJson(json, VisitorGreetedEvent.class);
      visitorsGreeted.add(greetedEvent.visitor());
    }

    return String.format(
        "%d visitors have been greeted, they are: [%s]",
        visitorsGreeted.size(),
        visitorGreetedEvent);

  }

}
