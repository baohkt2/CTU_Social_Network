import { useDashboardStore } from "../store";

export default function ProjectOverview() {
  const graph = useDashboardStore((s) => s.graph);
  const startTour = useDashboardStore((s) => s.startTour);

  if (!graph) {
    return (
      <div className="h-full w-full flex items-center justify-center">
        <p className="text-text-muted text-sm">Loading project...</p>
      </div>
    );
  }

  const { project, nodes, edges, layers } = graph;
  const hasTour = graph.tour.length > 0;

  // Count node types
  const typeCounts: Record<string, number> = {};
  for (const node of nodes) {
    typeCounts[node.type] = (typeCounts[node.type] ?? 0) + 1;
  }

  return (
    <div className="h-full w-full overflow-auto p-5 animate-fade-slide-in">
      {/* Project name */}
      <h2 className="font-serif text-2xl text-text-primary mb-1">{project.name}</h2>
      <p className="text-sm text-text-secondary leading-relaxed mb-6">{project.description}</p>

      {/* Stats grid */}
      <div className="grid grid-cols-2 gap-3 mb-6">
        <div className="bg-elevated rounded-lg p-3 border border-border-subtle">
          <div className="text-2xl font-mono font-medium text-gold">{nodes.length}</div>
          <div className="text-[11px] text-text-muted uppercase tracking-wider mt-1">Nodes</div>
        </div>
        <div className="bg-elevated rounded-lg p-3 border border-border-subtle">
          <div className="text-2xl font-mono font-medium text-gold">{edges.length}</div>
          <div className="text-[11px] text-text-muted uppercase tracking-wider mt-1">Edges</div>
        </div>
        <div className="bg-elevated rounded-lg p-3 border border-border-subtle">
          <div className="text-2xl font-mono font-medium text-gold">{layers.length}</div>
          <div className="text-[11px] text-text-muted uppercase tracking-wider mt-1">Layers</div>
        </div>
        <div className="bg-elevated rounded-lg p-3 border border-border-subtle">
          <div className="text-2xl font-mono font-medium text-gold">{Object.keys(typeCounts).length}</div>
          <div className="text-[11px] text-text-muted uppercase tracking-wider mt-1">Types</div>
        </div>
      </div>

      {/* Languages */}
      {project.languages.length > 0 && (
        <div className="mb-5">
          <h3 className="text-[11px] font-semibold text-gold uppercase tracking-wider mb-2">Languages</h3>
          <div className="flex flex-wrap gap-1.5">
            {project.languages.map((lang) => (
              <span key={lang} className="text-[11px] glass text-text-secondary px-2.5 py-1 rounded-full">
                {lang}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Frameworks */}
      {project.frameworks.length > 0 && (
        <div className="mb-5">
          <h3 className="text-[11px] font-semibold text-gold uppercase tracking-wider mb-2">Frameworks</h3>
          <div className="flex flex-wrap gap-1.5">
            {project.frameworks.map((fw) => (
              <span key={fw} className="text-[11px] glass text-text-secondary px-2.5 py-1 rounded-full">
                {fw}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Analyzed at */}
      <div className="text-[11px] text-text-muted mb-6">
        Analyzed: {new Date(project.analyzedAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}
      </div>

      {/* Start Tour button */}
      {hasTour && (
        <button
          onClick={startTour}
          className="w-full bg-gold/10 border border-gold/30 text-gold text-sm font-medium py-2.5 px-4 rounded-lg hover:bg-gold/20 transition-all duration-200"
        >
          Start Guided Tour
        </button>
      )}
    </div>
  );
}
