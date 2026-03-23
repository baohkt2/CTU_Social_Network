import { useDashboardStore } from "../store";

export default function CodeViewer() {
  const graph = useDashboardStore((s) => s.graph);
  const codeViewerNodeId = useDashboardStore((s) => s.codeViewerNodeId);
  const closeCodeViewer = useDashboardStore((s) => s.closeCodeViewer);

  const node = graph?.nodes.find((n) => n.id === codeViewerNodeId) ?? null;

  if (!node) {
    return (
      <div className="h-full w-full flex items-center justify-center bg-surface">
        <p className="text-text-muted text-sm">No file selected</p>
      </div>
    );
  }

  const lineInfo = node.lineRange
    ? `Lines ${node.lineRange[0]}\u2013${node.lineRange[1]}`
    : "Full file";

  return (
    <div className="h-full w-full flex flex-col bg-surface overflow-hidden">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-2.5 bg-elevated border-b border-border-subtle shrink-0">
        <span
          className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded border"
          style={{
            color: "var(--color-node-file)",
            borderColor: "rgba(74,124,155,0.3)",
            backgroundColor: "rgba(74,124,155,0.1)",
          }}
        >
          {node.type}
        </span>
        <span className="text-sm font-serif text-text-primary truncate">
          {node.name}
        </span>
        {node.filePath && (
          <span className="text-xs font-mono text-text-muted truncate ml-auto">
            {node.filePath}
          </span>
        )}
        <span className="text-[10px] text-text-muted">{lineInfo}</span>
        <button
          onClick={closeCodeViewer}
          className="text-text-muted hover:text-text-primary ml-2 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-auto p-5">
        {/* Summary */}
        <div className="mb-4">
          <h4 className="text-[11px] font-semibold text-gold uppercase tracking-wider mb-2">Summary</h4>
          <p className="text-sm text-text-secondary leading-relaxed">{node.summary}</p>
        </div>

        {/* Language notes callout */}
        {node.languageNotes && (
          <div className="mb-4 bg-gold/5 border border-gold/20 rounded-lg p-3">
            <h4 className="text-[11px] font-semibold text-gold uppercase tracking-wider mb-1.5">Language Notes</h4>
            <p className="text-sm text-text-secondary leading-relaxed">{node.languageNotes}</p>
          </div>
        )}

        {/* Tags */}
        {node.tags.length > 0 && (
          <div className="mb-4">
            <h4 className="text-[11px] font-semibold text-gold uppercase tracking-wider mb-2">Tags</h4>
            <div className="flex flex-wrap gap-1.5">
              {node.tags.map((tag) => (
                <span key={tag} className="text-[11px] glass text-text-secondary px-2.5 py-1 rounded-full">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Source note */}
        <div className="text-[11px] text-text-muted italic">
          Source code available locally at {node.filePath ?? "the project directory"}
        </div>
      </div>
    </div>
  );
}
