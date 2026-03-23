import { Handle, Position } from "@xyflow/react";
import type { NodeProps, Node } from "@xyflow/react";

const typeColors: Record<string, string> = {
  file: "var(--color-node-file)",
  function: "var(--color-node-function)",
  class: "var(--color-node-class)",
  module: "var(--color-node-module)",
  concept: "var(--color-node-concept)",
};

const typeTextColors: Record<string, string> = {
  file: "text-node-file",
  function: "text-node-function",
  class: "text-node-class",
  module: "text-node-module",
  concept: "text-node-concept",
};

const complexityColors: Record<string, string> = {
  simple: "text-node-function",
  moderate: "text-gold-dim",
  complex: "text-[#c97070]",
};

export interface CustomNodeData extends Record<string, unknown> {
  label: string;
  nodeType: string;
  summary: string;
  complexity: string;
  isHighlighted: boolean;
  searchScore?: number;
  isSelected: boolean;
  isTourHighlighted: boolean;
  isDiffChanged: boolean;
  isDiffAffected: boolean;
  isDiffFaded: boolean;
  onNodeClick?: (nodeId: string) => void;
}

export type CustomFlowNode = Node<CustomNodeData, "custom">;

export default function CustomNode({
  id,
  data,
}: NodeProps<CustomFlowNode>) {
  const barColor = typeColors[data.nodeType] ?? typeColors.file;
  const textColor = typeTextColors[data.nodeType] ?? typeTextColors.file;
  const complexityColor = complexityColors[data.complexity] ?? complexityColors.simple;

  let extraClass = "";
  if (data.isSelected) {
    extraClass = "ring-2 ring-gold node-glow";
  } else if (data.isTourHighlighted) {
    extraClass = "ring-2 ring-gold-dim animate-gold-pulse";
  } else if (data.isHighlighted) {
    const score = data.searchScore ?? 1;
    if (score <= 0.1) {
      extraClass = "ring-2 ring-gold-bright";
    } else if (score <= 0.3) {
      extraClass = "ring-2 ring-gold";
    } else {
      extraClass = "ring-1 ring-gold-dim/60";
    }
  }

  // Diff overlay styling (composes with above)
  if (data.isDiffChanged) {
    extraClass += " ring-2 ring-[var(--color-diff-changed)] diff-changed-glow";
  } else if (data.isDiffAffected) {
    extraClass += " ring-1 ring-[var(--color-diff-affected)] diff-affected-glow";
  } else if (data.isDiffFaded) {
    extraClass += " diff-faded";
  }

  const name = data.label ?? "unnamed";
  const truncatedName =
    name.length > 24 ? name.slice(0, 22) + "..." : name;

  return (
    <div
      className={`relative rounded-lg bg-elevated border border-border-subtle ${extraClass} min-w-[180px] max-w-[220px] overflow-hidden transition-all duration-200 cursor-pointer`}
      style={{ boxShadow: '0 2px 8px rgba(0,0,0,0.3)' }}
      onClick={() => data.onNodeClick?.(id)}
    >
      {/* Left color bar */}
      <div
        className="absolute left-0 top-0 bottom-0 w-1 rounded-l-lg"
        style={{ backgroundColor: barColor }}
      />

      <Handle
        type="target"
        position={Position.Top}
        className="!bg-text-muted !w-2 !h-2"
      />

      <div className="pl-4 pr-3 py-2">
        <div className="flex items-center justify-between mb-1">
          <span className={`text-[10px] font-semibold uppercase tracking-wider ${textColor}`}>
            {data.nodeType}
          </span>
          <span className={`text-[9px] font-mono ${complexityColor}`}>
            {data.complexity}
          </span>
        </div>

        <div className="text-sm font-serif text-text-primary truncate" title={data.label}>
          {truncatedName}
        </div>

        <div className="text-[11px] text-text-secondary mt-1 line-clamp-2 leading-tight">
          {data.summary}
        </div>
      </div>

      <Handle
        type="source"
        position={Position.Bottom}
        className="!bg-text-muted !w-2 !h-2"
      />
    </div>
  );
}
