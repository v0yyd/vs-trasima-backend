package dhbw.trasima.trasima_aufgabe_10.server;

import dhbw.trasima.trasima_aufgabe_10.model.V2State;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class InMemoryV2Store {

    private final ConcurrentMap<Integer, V2State> store = new ConcurrentHashMap<>();

    List<V2State> list() {
        return new ArrayList<>(store.values());
    }

    V2State get(int id) {
        return store.get(id);
    }

    boolean create(V2State state) {
        return store.putIfAbsent(state.id, state) == null;
    }

    boolean update(V2State state) {
        return store.replace(state.id, state) != null;
    }

    boolean delete(int id) {
        return store.remove(id) != null;
    }
}

