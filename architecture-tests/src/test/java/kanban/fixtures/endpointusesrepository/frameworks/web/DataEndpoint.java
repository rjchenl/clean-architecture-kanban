package kanban.fixtures.endpointusesrepository.frameworks.web;

import kanban.fixtures.endpointusesrepository.adapters.eventstore.SomeRepository;

/** 違規樣本（CA-7）：HTTP 端點直接碰儲存。 */
public final class DataEndpoint {
    SomeRepository repository;
}
