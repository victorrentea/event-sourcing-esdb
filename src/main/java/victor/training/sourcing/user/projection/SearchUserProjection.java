package victor.training.sourcing.user.projection;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import victor.training.sourcing.GsonUtil;
import victor.training.sourcing.user.domain.User;
import victor.training.sourcing.user.domain.UserEvent;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchUserProjection {
  private final EventStoreDBClient eventStore;

  public record UserSearchCriteria(String namePart, String emailPart){}
  public record UserSearchResult(String email, String name, String departmentId) {}

  @GetMapping("users/search")
  public void search(@RequestParam UserSearchCriteria criteria) {
    // TODO
  }
}
