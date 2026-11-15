package kanban.fixtures.entityinrequest.usecases.commands.dothing;

import java.util.List;
import kanban.fixtures.entityinrequest.entities.Thing;

/** 違規樣本（CA-5）：Request Model 夾帶領域物件（藏在 List 裡也算）。 */
public record DoThingRequest(String boardId, List<Thing> things) {
}
