package victor.training.sourcing;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.RecordedEvent;
import com.google.gson.*;
import victor.training.sourcing.user.domain.UserEvent;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class GsonUtil {
  private static final Gson gson = new GsonBuilder()
      .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
          return new JsonPrimitive(src.toString());
        }
      })
      .create();

  public static EventData toEventData(Object event) {
    var eventType = event.getClass().getSimpleName();
    var jsonString = gson.toJson(event);
    return EventData.builderAsJson(eventType, jsonString.getBytes())
//        .metadataAsBytes(new EventMetadata(SecurityContextHolder.....))
    .build();
  }

  @SuppressWarnings("unchecked")
  public static <T> T fromEventDataSealed(RecordedEvent eventData, Class<UserEvent> eventSealedSuperclass) {
    var jsonString = new String(eventData.getEventData());
    var eventClasses = Arrays.stream(eventSealedSuperclass.getPermittedSubclasses())
        .collect(toMap(Class::getSimpleName, Function.identity()));
    Class<?> eventClass = Objects.requireNonNull(eventClasses.get(eventData.getEventType()),
        "Unknown Type: " + eventData.getEventType());
    return (T) gson.fromJson(jsonString, eventClass);
  }
  @SuppressWarnings("unchecked")
  public static <T> T parseEventData(RecordedEvent eventData, Class<UserEvent> eventClass) {
    var jsonString = new String(eventData.getEventData());
    return (T) gson.fromJson(jsonString, eventClass);
  }
  public static <T> Optional<T> tryParseEvent(RecordedEvent eventData, Class<T> eventClass) {
    if (eventData.getEventType().equals(eventClass.getSimpleName())) {
      var jsonString = new String(eventData.getEventData());
      return Optional.ofNullable(gson.fromJson(jsonString, eventClass));
    } else {
      return Optional.empty();
    }
  }
}
