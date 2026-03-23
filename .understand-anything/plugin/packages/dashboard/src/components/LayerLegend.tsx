import { useDashboardStore } from "../store";

const LAYER_COLORS = [
  "rgba(59, 130, 246, 0.08)", // blue
  "rgba(16, 185, 129, 0.08)", // green
  "rgba(245, 158, 11, 0.08)", // amber
  "rgba(139, 92, 246, 0.08)", // violet
  "rgba(236, 72, 153, 0.08)", // pink
  "rgba(6, 182, 212, 0.08)", // cyan
  "rgba(249, 115, 22, 0.08)", // orange
  "rgba(168, 162, 158, 0.08)", // stone
];

export const LAYER_BORDER_COLORS = [
  "rgba(59, 130, 246, 0.5)", // blue
  "rgba(16, 185, 129, 0.5)", // green
  "rgba(245, 158, 11, 0.5)", // amber
  "rgba(139, 92, 246, 0.5)", // violet
  "rgba(236, 72, 153, 0.5)", // pink
  "rgba(6, 182, 212, 0.5)", // cyan
  "rgba(249, 115, 22, 0.5)", // orange
  "rgba(168, 162, 158, 0.5)", // stone
];

export { LAYER_COLORS };

export function getLayerColor(index: number): string {
  return LAYER_COLORS[index % LAYER_COLORS.length];
}

export function getLayerBorderColor(index: number): string {
  return LAYER_BORDER_COLORS[index % LAYER_BORDER_COLORS.length];
}

export default function LayerLegend() {
  const graph = useDashboardStore((s) => s.graph);
  const showLayers = useDashboardStore((s) => s.showLayers);
  const toggleLayers = useDashboardStore((s) => s.toggleLayers);

  const layers = graph?.layers ?? [];
  const hasLayers = layers.length > 0;

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={toggleLayers}
        disabled={!hasLayers}
        className={`px-2 py-0.5 rounded text-[11px] font-medium transition-colors ${
          showLayers && hasLayers
            ? "bg-gold/20 text-gold"
            : hasLayers
              ? "bg-elevated text-text-secondary hover:bg-surface"
              : "bg-elevated text-text-muted cursor-not-allowed"
        }`}
        title={
          hasLayers
            ? showLayers
              ? "Hide layer grouping"
              : "Show layer grouping"
            : "No layers in graph"
        }
      >
        Layers {showLayers && hasLayers ? "ON" : "OFF"}
      </button>

      {showLayers && hasLayers && (
        <div className="flex items-center gap-3">
          {layers.map((layer, i) => (
            <div key={layer.id} className="flex items-center gap-1">
              <span
                className="inline-block w-2 h-2 rounded-full"
                style={{ backgroundColor: getLayerBorderColor(i) }}
              />
              <span className="text-text-secondary text-[11px]">
                {layer.name}
                <span className="text-text-muted ml-0.5">
                  ({layer.nodeIds.length})
                </span>
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
