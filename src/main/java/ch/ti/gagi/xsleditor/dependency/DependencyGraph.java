package ch.ti.gagi.xsleditor.dependency;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DependencyGraph {

    private final Map<Path, List<Path>> edges;

    public DependencyGraph(Map<Path, List<Path>> edges) {
        this.edges = edges.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> List.copyOf(e.getValue())
                ));
    }

    public Map<Path, List<Path>> edges() {
        return edges;
    }

    public List<Path> dependenciesOf(Path file) {
        return edges.getOrDefault(file, List.of());
    }
}
