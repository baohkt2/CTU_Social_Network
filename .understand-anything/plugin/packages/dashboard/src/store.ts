import { create } from "zustand";
import { SearchEngine } from "@understand-anything/core/search";
import type { SearchResult } from "@understand-anything/core/search";
import type {
  KnowledgeGraph,
  TourStep,
} from "@understand-anything/core/types";

export type Persona = "non-technical" | "junior" | "experienced";

interface DashboardStore {
  graph: KnowledgeGraph | null;
  selectedNodeId: string | null;
  searchQuery: string;
  searchResults: SearchResult[];
  searchEngine: SearchEngine | null;
  searchMode: "fuzzy" | "semantic";
  setSearchMode: (mode: "fuzzy" | "semantic") => void;

  showLayers: boolean;

  codeViewerOpen: boolean;
  codeViewerNodeId: string | null;

  tourActive: boolean;
  currentTourStep: number;
  tourHighlightedNodeIds: string[];

  persona: Persona;

  diffMode: boolean;
  changedNodeIds: Set<string>;
  affectedNodeIds: Set<string>;

  setGraph: (graph: KnowledgeGraph) => void;
  selectNode: (nodeId: string | null) => void;
  setSearchQuery: (query: string) => void;
  toggleLayers: () => void;
  setPersona: (persona: Persona) => void;
  openCodeViewer: (nodeId: string) => void;
  closeCodeViewer: () => void;

  setDiffOverlay: (changed: string[], affected: string[]) => void;
  toggleDiffMode: () => void;
  clearDiffOverlay: () => void;

  startTour: () => void;
  stopTour: () => void;
  setTourStep: (step: number) => void;
  nextTourStep: () => void;
  prevTourStep: () => void;
}

function getSortedTour(graph: KnowledgeGraph): TourStep[] {
  const tour = graph.tour ?? [];
  return [...tour].sort((a, b) => a.order - b.order);
}

export const useDashboardStore = create<DashboardStore>()((set, get) => ({
  graph: null,
  selectedNodeId: null,
  searchQuery: "",
  searchResults: [],
  searchEngine: null,
  searchMode: "fuzzy",

  showLayers: false,

  codeViewerOpen: false,
  codeViewerNodeId: null,

  tourActive: false,
  currentTourStep: 0,
  tourHighlightedNodeIds: [],

  persona: "junior",

  diffMode: false,
  changedNodeIds: new Set<string>(),
  affectedNodeIds: new Set<string>(),

  setGraph: (graph) => {
    const searchEngine = new SearchEngine(graph.nodes);
    const query = get().searchQuery;
    const searchResults = query.trim() ? searchEngine.search(query) : [];
    set({ graph, searchEngine, searchResults });
  },
  selectNode: (nodeId) => set({ selectedNodeId: nodeId }),
  setSearchMode: (mode) => set({ searchMode: mode }),
  setSearchQuery: (query) => {
    const engine = get().searchEngine;
    const mode = get().searchMode;
    if (!engine || !query.trim()) {
      set({ searchQuery: query, searchResults: [] });
      return;
    }
    // Currently both modes use the same fuzzy engine
    // When embeddings are available, "semantic" mode will use SemanticSearchEngine
    void mode;
    const searchResults = engine.search(query);
    set({ searchQuery: query, searchResults });
  },

  toggleLayers: () => set((state) => ({ showLayers: !state.showLayers })),

  setPersona: (persona) => set({ persona }),

  openCodeViewer: (nodeId) => set({ codeViewerOpen: true, codeViewerNodeId: nodeId }),
  closeCodeViewer: () => set({ codeViewerOpen: false, codeViewerNodeId: null }),

  setDiffOverlay: (changed, affected) =>
    set({
      diffMode: true,
      changedNodeIds: new Set(changed),
      affectedNodeIds: new Set(affected),
    }),

  toggleDiffMode: () => set((state) => ({ diffMode: !state.diffMode })),

  clearDiffOverlay: () =>
    set({
      diffMode: false,
      changedNodeIds: new Set<string>(),
      affectedNodeIds: new Set<string>(),
    }),

  startTour: () => {
    const { graph } = get();
    if (!graph || !graph.tour || graph.tour.length === 0) return;
    const sorted = getSortedTour(graph);
    set({
      tourActive: true,
      currentTourStep: 0,
      tourHighlightedNodeIds: sorted[0].nodeIds,
      selectedNodeId: null,
    });
  },

  stopTour: () =>
    set({
      tourActive: false,
      currentTourStep: 0,
      tourHighlightedNodeIds: [],
    }),

  setTourStep: (step) => {
    const { graph } = get();
    if (!graph || !graph.tour || graph.tour.length === 0) return;
    const sorted = getSortedTour(graph);
    if (step < 0 || step >= sorted.length) return;
    set({
      currentTourStep: step,
      tourHighlightedNodeIds: sorted[step].nodeIds,
    });
  },

  nextTourStep: () => {
    const { graph, currentTourStep } = get();
    if (!graph || !graph.tour || graph.tour.length === 0) return;
    const sorted = getSortedTour(graph);
    if (currentTourStep < sorted.length - 1) {
      const next = currentTourStep + 1;
      set({
        currentTourStep: next,
        tourHighlightedNodeIds: sorted[next].nodeIds,
      });
    }
  },

  prevTourStep: () => {
    const { graph, currentTourStep } = get();
    if (!graph || !graph.tour || graph.tour.length === 0) return;
    if (currentTourStep > 0) {
      const sorted = getSortedTour(graph);
      const prev = currentTourStep - 1;
      set({
        currentTourStep: prev,
        tourHighlightedNodeIds: sorted[prev].nodeIds,
      });
    }
  },
}));
