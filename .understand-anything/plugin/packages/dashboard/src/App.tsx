import { useEffect, useState } from "react";
import { validateGraph } from "@understand-anything/core/schema";
import { useDashboardStore } from "./store";
import GraphView from "./components/GraphView";
import CodeViewer from "./components/CodeViewer";
import SearchBar from "./components/SearchBar";
import NodeInfo from "./components/NodeInfo";
import LayerLegend from "./components/LayerLegend";
import DiffToggle from "./components/DiffToggle";
import LearnPanel from "./components/LearnPanel";
import PersonaSelector from "./components/PersonaSelector";
import ProjectOverview from "./components/ProjectOverview";

function App() {
  const graph = useDashboardStore((s) => s.graph);
  const setGraph = useDashboardStore((s) => s.setGraph);
  const selectedNodeId = useDashboardStore((s) => s.selectedNodeId);
  const tourActive = useDashboardStore((s) => s.tourActive);
  const persona = useDashboardStore((s) => s.persona);
  const codeViewerOpen = useDashboardStore((s) => s.codeViewerOpen);
  const closeCodeViewer = useDashboardStore((s) => s.closeCodeViewer);
  const setDiffOverlay = useDashboardStore((s) => s.setDiffOverlay);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    fetch("/knowledge-graph.json")
      .then((res) => res.json())
      .then((data: unknown) => {
        const result = validateGraph(data);
        if (result.success && result.data) {
          setGraph(result.data);
        } else {
          const errorMsg = result.errors?.join("; ") ?? "Unknown validation error";
          console.error("Knowledge graph validation failed:", errorMsg);
          setLoadError(`Invalid knowledge graph: ${errorMsg}`);
        }
      })
      .catch((err) => {
        console.error("Failed to load knowledge graph:", err);
        setLoadError(`Failed to load knowledge graph: ${err instanceof Error ? err.message : String(err)}`);
      });
  }, [setGraph]);

  useEffect(() => {
    fetch("/diff-overlay.json")
      .then((res) => {
        if (!res.ok) return null;
        return res.json();
      })
      .then((data: unknown) => {
        if (
          data &&
          typeof data === "object" &&
          "changedNodeIds" in data &&
          "affectedNodeIds" in data &&
          Array.isArray((data as Record<string, unknown>).changedNodeIds) &&
          Array.isArray((data as Record<string, unknown>).affectedNodeIds)
        ) {
          const d = data as { changedNodeIds: string[]; affectedNodeIds: string[] };
          if (d.changedNodeIds.length > 0) {
            setDiffOverlay(d.changedNodeIds, d.affectedNodeIds);
          }
        }
      })
      .catch(() => {
        // Silently ignore - diff overlay is optional
      });
  }, [setDiffOverlay]);

  // Determine sidebar content
  // Learn persona always shows LearnPanel; tour active overrides everything
  const sidebarContent = tourActive || persona === "junior" ? (
    <LearnPanel />
  ) : selectedNodeId ? (
    <NodeInfo />
  ) : (
    <ProjectOverview />
  );

  return (
    <div className="h-screen w-screen flex flex-col bg-root text-text-primary noise-overlay">
      {/* Header */}
      <header className="flex items-center justify-between px-5 py-3 bg-surface border-b border-border-subtle shrink-0">
        <div className="flex items-center gap-5">
          <h1 className="font-serif text-lg text-text-primary tracking-wide">
            {graph?.project.name ?? "Understand Anything"}
          </h1>
          <div className="w-px h-5 bg-border-subtle" />
          <PersonaSelector />
        </div>
        <div className="flex items-center gap-4">
          <DiffToggle />
          <LayerLegend />
        </div>
      </header>

      {/* Search */}
      <SearchBar />

      {/* Error banner */}
      {loadError && (
        <div className="px-5 py-3 bg-red-900/30 border-b border-red-700 text-red-200 text-sm">
          {loadError}
        </div>
      )}

      {/* Main content: Graph + Sidebar */}
      <div className="flex-1 flex min-h-0 relative">
        {/* Graph area */}
        <div className="flex-1 min-w-0 min-h-0">
          <GraphView />
        </div>

        {/* Right sidebar */}
        <aside className="w-[360px] shrink-0 bg-surface border-l border-border-subtle overflow-hidden">
          {sidebarContent}
        </aside>

        {/* Code viewer overlay */}
        {codeViewerOpen && (
          <div className="absolute bottom-0 left-0 right-0 h-[40vh] bg-surface border-t border-border-subtle animate-slide-up z-20">
            <div className="h-full flex flex-col">
              <div className="flex items-center justify-end px-3 py-1 shrink-0">
                <button
                  onClick={closeCodeViewer}
                  className="text-text-muted hover:text-text-primary text-xs transition-colors"
                >
                  Close
                </button>
              </div>
              <div className="flex-1 min-h-0">
                <CodeViewer />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
