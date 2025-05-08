package victor.training.sourcing;

import lombok.*;

@Data
public abstract class AbstractEvent {
  protected transient boolean replay = false; // only set to true when replaying
}
