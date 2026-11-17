package kanban.fixtures.contextmapbypass.downstream.usecases;

import kanban.fixtures.contextmapbypass.upstream.usecases.queries.published.PublishedFact;

/** 違規樣本：下游不經過防腐層，直接讀上游公布的格式。 */
public class KnowsUpstream {

    PublishedFact fact;
}
