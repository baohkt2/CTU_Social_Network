import { useState } from "react";
import { useDashboardStore } from "../store";

const typeBadgeColors: Record<string, string> = {
  file: "text-node-file border border-node-file/30 bg-node-file/10",
  function: "text-node-function border border-node-function/30 bg-node-function/10",
  class: "text-node-class border border-node-class/30 bg-node-class/10",
  module: "text-node-module border border-node-module/30 bg-node-module/10",
  concept: "text-node-concept border border-node-concept/30 bg-node-concept/10",
};

const complexityBadgeColors: Record<string, string> = {
  simple: "text-node-function border border-node-function/30 bg-node-function/10",
  moderate: "text-gold-dim border border-gold-dim/30 bg-gold-dim/10",
  complex: "text-[#c97070] border border-[#c97070]/30 bg-[#c97070]/10",
};

export default function NodeInfo() {
  const graph = useDashboardStore((s) => s.graph);
  const selectedNodeId = useDashboardStore((s) => s.selectedNodeId);
  const [languageExpanded, setLanguageExpanded] = useState(true);

  const node = graph?.nodes.find((n) => n.id === selectedNodeId) ?? null;

  if (!node) {
    return (
      <div className="h-full w-full flex items-center justify-center bg-surface">
        <p className="text-text-muted text-sm">Select a node to see details</p>
      </div>
    );
  }

  const connections = (graph?.edges ?? []).filter(
    (e) => e.source === node.id || e.target === node.id,
  );

  const typeBadge = typeBadgeColors[node.type] ?? typeBadgeColors.file;
  const complexityBadge =
    complexityBadgeColors[node.complexity] ?? complexityBadgeColors.simple;

  return (
    <div className="h-full w-full overflow-auto p-5 animate-fade-slide-in">
      <div className="flex items-center gap-2 mb-3">
        <span
          className={`text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded ${typeBadge}`}
        >
          {node.type}
        </span>
        <span
          className={`text-[10px] font-semibold px-2 py-0.5 rounded ${complexityBadge}`}
        >
          {node.complexity}
        </span>
      </div>

      <h2 className="text-lg font-serif text-text-primary mb-2">{node.name}</h2>

      <p className="text-sm text-text-secondary mb-4 leading-relaxed">
        {node.summary}
      </p>

      {node.filePath && (
        <div className="text-xs text-text-secondary mb-2">
          <span className="font-medium text-text-muted">File:</span>{" "}
          {node.filePath}
          {node.lineRange && (
            <span className="ml-2">
              (L{node.lineRange[0]}-{node.lineRange[1]})
            </span>
          )}
        </div>
      )}

      {node.languageNotes && (
        <div className="mb-4">
          <button
            onClick={() => setLanguageExpanded(!languageExpanded)}
            className="flex items-center gap-1.5 text-xs font-semibold text-gold uppercase tracking-wider mb-2 hover:text-gold-bright transition-colors"
          >
            <svg
              className={`w-3 h-3 transition-transform ${languageExpanded ? "rotate-90" : ""}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
            Language Concepts
          </button>
          {languageExpanded && (
            <div className="bg-gold/5 border border-gold/20 rounded-lg p-3">
              <p className="text-sm text-text-secondary leading-relaxed">
                {node.languageNotes}
              </p>
            </div>
          )}
        </div>
      )}

      {node.tags.length > 0 && (
        <div className="mb-4">
          <h3 className="text-[11px] font-semibold text-gold uppercase tracking-wider mb-2">
            Tags
          </h3>
          <div className="flex flex-wrap gap-1.5">
            {node.tags.map((tag) => (
              <span
                key={tag}
                className="text-[11px] glass text-text-secondary px-2.5 py-1 rounded-full"
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
      )}

      {connections.length > 0 && (
        <div>
          <h3 className="text-[11px] font-semibold text-gold uppercase tracking-wider mb-2">
            Connections ({connections.length})
          </h3>
          <div className="space-y-1.5">
            {connections.map((edge, i) => {
              const isSource = edge.source === node.id;
              const otherId = isSource ? edge.target : edge.source;
              const otherNode = graph?.nodes.find((n) => n.id === otherId);
              const arrow = isSource ? "\u2192" : "\u2190";

              return (
                <div
                  key={i}
                  className="text-xs bg-elevated rounded-lg px-3 py-2 border border-border-subtle flex items-center gap-2"
                >
                  <span className="text-gold font-mono">{arrow}</span>
                  <span className="text-text-muted">{edge.type}</span>
                  <span className="text-text-primary truncate">
                    {otherNode?.name ?? otherId}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
