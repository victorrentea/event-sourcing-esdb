package victor.training.sourcing;

import com.eventstore.dbclient.EventStoreDBClient;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import victor.training.sourcing.user.command.SnapshotApi;
import victor.training.sourcing.user.command.UserCommandRestApi;
import victor.training.sourcing.user.command.UserCommandRestApi.CreateUserRequest;
import victor.training.sourcing.user.projection.GetUserByIdProjection;
import victor.training.sourcing.user.projection.UsersThatCanLoginProjection;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
@TestMethodOrder(MethodName.class)
@AutoConfigureMockMvc
public class FullTest {
  public static final String EMAIL = "test-%s@integration.com".formatted(new Random().nextInt(1000_0000));
  @Autowired
  UserCommandRestApi commandApi;
  @Autowired
  SnapshotApi snapshotApi;
  @Autowired
  GetUserByIdProjection getUserByIdProjection;
  @Autowired
  UsersThatCanLoginProjection loginUsers;
  @Autowired
  EventStoreDBClient eventStore;
  @Autowired
  MockMvc mockMvc;

  @Test
  void _01_create() throws Exception {
    CreateUserRequest createRequest = new CreateUserRequest(
        EMAIL,
        "John",
        "dep1",
        List.of("app1:ADMIN"));

    commandApi.createUser(createRequest);
//    extracted(post("/users"), createRequest);
  }

//  private void extracted(MockHttpServletRequestBuilder request, Object body) throws Exception {
//    mockMvc.perform(request
//            .content(new Gson().toJson(body))
//            .contentType(MediaType.APPLICATION_JSON))
//        .andExpect(status().isOk());
//  }

  @Test
  void _01_create_snapshot() throws Exception {
//    extracted(post("/users/"+EMAIL+"/snapshot"), null);
//    commandApi.
    snapshotApi.createSnapshot(EMAIL);

  }

  @Test
  void _02_get() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.email()).isEqualTo(EMAIL);
    assertThat(user.name()).isEqualTo("John");
    assertThat(user.departmentId()).isEqualTo("dep1");
    assertThat(user.roles()).containsExactly("app1:ADMIN");
    assertThat(user.emailValidated()).isFalse();
    assertThat(user.active()).isTrue();
  }

  @Test
  void _03_user_cannot_login_yet() throws ExecutionException, InterruptedException {
    assertThat(loginUsers.getUsersToLogin(null, null))
        .doesNotContain(EMAIL);
  }

  @Test
  void _10_confirmEmail() throws Exception {
    commandApi.confirmEmail(EMAIL, "CHEAT");
    commandApi.confirmEmail(EMAIL, "CHEAT");
    commandApi.confirmEmail(EMAIL, "CHEAT");
  }

  @Test
  void _11_get_emailWasConfirmed() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.emailValidated()).isTrue();
    assertThat(user.active()).isTrue();
  }

  @Test
//  @Disabled
  void _12_user_can_now_login() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.emailValidated()).isTrue();
//    assertThat(user.com).isTrue();

    assertThat(loginUsers.getUsersToLogin(null, null)).contains(EMAIL);
  }

  @Test
  void _20_create_duplicated_email_fails() {
    assertThatThrownBy(() ->
        commandApi.createUser(new CreateUserRequest(
            EMAIL,
            "John",
            "dep1",
            List.of("ADMIN")))); // fails!
  }

  @Test
  void _22_update_details() throws Exception {
    commandApi.update(EMAIL, new UserCommandRestApi.UpdateUserRequest(
        "Jane",
        "dep2"));
  }
  @Test
  void _23_get() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.name()).isEqualTo("Jane");
    assertThat(user.departmentId()).isEqualTo("dep2");
  }

  @Test
  void _30_grant_role() throws Exception {
    commandApi.grantRole(EMAIL, "app2:ADMIN");
  }

  @Test
  void _31_new_role_visible() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.roles()).containsExactly(
        "app1:ADMIN",
        "app2:ADMIN");
  }
  @Test
  void _32_revoke_role() throws Exception {
    commandApi.revokeRole(EMAIL, "app1:ADMIN");
  }
  @Test
  void _33_new_role_visible() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.roles()).containsExactly("app2:ADMIN");
  }

  @Test
  void _40_deactivate_user() throws Exception {
    commandApi.deactivate(EMAIL);
  }

  @Test
  void _41_get_shows_inactive() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.active()).isFalse();
  }
  @Test
  void _42_user_cannot_login() throws ExecutionException, InterruptedException {
    assertThat(loginUsers.getUsersToLogin(null, null))
        .doesNotContain(EMAIL);
  }
  @Test
  void _50_reactivate_user() throws Exception {
    commandApi.activate(EMAIL);
  }
  @Test
  void _51_get_shows_active() throws ExecutionException, InterruptedException {
    var user = getUserByIdProjection.getUser(EMAIL);
    assertThat(user.active()).isTrue();
  }
  @Test
  void _52_user_can_login_again() throws ExecutionException, InterruptedException {
    assertThat(loginUsers.getUsersToLogin(null, null))
        .contains(EMAIL);
  }

  @Test
  void _99_time_travel_test() throws Exception {
    assertThat(loginUsers.getUsersToLogin(null, null)).contains(EMAIL);
    commandApi.deactivate(EMAIL);

    Thread.sleep(100);
    assertThat(loginUsers.getUsersToLogin(null, null)).doesNotContain(EMAIL);
    commandApi.activate(EMAIL);

    Thread.sleep(100);
    assertThat(loginUsers.getUsersToLogin(null, null)).contains(EMAIL); // ✅

    assertThat(loginUsers.getUsersToLogin(null, now().minusMillis(150L).toString()))
        .doesNotContain(EMAIL); // ❌

    assertThat(loginUsers.getUsersToLogin(null, now().minusMillis(250L).toString()))
        .contains(EMAIL); // ✅
  }

}
