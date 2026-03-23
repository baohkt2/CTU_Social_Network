import { useCallback, useEffect, useMemo, useRef } from "react";
import {
  ReactFlow,
  ReactFlowProvider,
  useNodesState,
  useEdgesState,
  useReactFlow,
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
} from "@xyflow/react";
import type { Edge, Node } from "@xyflow/react";
import "@xyflow/react/dist/style.css";

import CustomNode from "./CustomNode";
import type { CustomFlowNode } from "./CustomNode";
import { useDashboardStore } from "../store";
import { applyDagreLayout, NODE_WIDTH, NODE_HEIGHT } from "../utils/layout";
// Layer colors are hardcoded to gold-tinted values in the group node styles

const LAYER_PADDING = 40;

const nodeTypes = { custom: CustomNode };

/**
 * Inner component that pans/zooms to tour-highlighted nodes.
 * Must be rendered inside <ReactFlow> so useReactFlow() works.
 */
function TourFitView() {
  const tourHighlightedNodeIds = useDashboardStore((s) => s.tourHighlightedNodeIds);
  const { fitView } = useReactFlow();
  const prevRef = useRef<string[]>([]);

  useEffect(() => {
    const prev = prevRef.current;
    const changed =
      tourHighlightedNodeIds.length > 0 &&
      (tourHighlightedNodeIds.length !== prev.length ||
        tourHighlightedNodeIds.some((id, i) => id !== prev[i]));
    prevRef.current = tourHighlightedNodeIds;

    if (changed) {
      // Small delay to ensure nodes are rendered before fitting
      requestAnimationFrame(() => {
        fitView({
          nodes: tourHighlightedNodeIds.map((id) => ({ id })),
          duration: 500,
          padding: 0.3,
          maxZoom: 1.2,
          minZoom: 0.01,
        });
      });
    }
  }, [tourHighlightedNodeIds, fitView]);

  return null;
}

/**
 * Centers the graph on the selected node (e.g. from search).
 * Must be rendered inside <ReactFlow> so useReactFlow() works.
 */
function SelectedNodeFitView() {
  const selectedNodeId = useDashboardStore((s) => s.selectedNodeId);
  const { fitView } = useReactFlow();
  const prevRef = useRef<string | null>(null);

  useEffect(() => {
    if (selectedNodeId && selectedNodeId !== prevRef.current) {
      requestAnimationFrame(() => {
        fitView({
          nodes: [{ id: selectedNodeId }],
          duration: 500,
          padding: 0.3,
          maxZoom: 1.2,
          minZoom: 0.01,
        });
      });
    }
    prevRef.current = selectedNodeId;
  }, [selectedNodeId, fitView]);

  return null;
}

function GraphViewInner() {
  const graph = useDashboardStore((s) => s.graph);
  const selectedNodeId = useDashboardStore((s) => s.selectedNodeId);
  const searchResults = useDashboardStore((s) => s.searchResults);
  const selectNode = useDashboardStore((s) => s.selectNode);
  const openCodeViewer = useDashboardStore((s) => s.openCodeViewer);
  const showLayers = useDashboardStore((s) => s.showLayers);
  const tourHighlightedNodeIds = useDashboardStore((s) => s.tourHighlightedNodeIds);
  const persona = useDashboardStore((s) => s.persona);
  const diffMode = useDashboardStore((s) => s.diffMode);
  const changedNodeIds = useDashboardStore((s) => s.changedNodeIds);
  const affectedNodeIds = useDashboardStore((s) => s.affectedNodeIds);

  const handleNodeSelect = useCallback(
    (nodeId: string) => {
      selectNode(nodeId);
      openCodeViewer(nodeId);
    },
    [selectNode, openCodeViewer],
  );

  const { initialNodes, initialEdges } = useMemo(() => {
    if (!graph)
      return {
        initialNodes: [] as (CustomFlowNode | Node)[],
        initialEdges: [] as Edge[],
      };

    // Filter nodes and edges based on persona
    const filteredGraphNodes =
      persona === "non-technical"
        ? graph.nodes.filter(
            (n) =>
              n.type === "concept" || n.type === "module" || n.type === "file",
          )
        : graph.nodes;

    const filteredNodeIds = new Set(filteredGraphNodes.map((n) => n.id));
    const filteredGraphEdges =
      persona === "non-technical"
        ? graph.edges.filter(
            (e) => filteredNodeIds.has(e.source) && filteredNodeIds.has(e.target),
          )
        : graph.edges;

    const flowNodes: CustomFlowNode[] = filteredGraphNodes.map((node) => {
      const matchResult = searchResults.find((r) => r.nodeId === node.id);
      return {
        id: node.id,
        type: "custom" as const,
        position: { x: 0, y: 0 },
        data: {
          label: node.name ?? node.filePath?.split("/").pop() ?? node.id,
          nodeType: node.type,
          summary: node.summary,
          complexity: node.complexity,
          isHighlighted: !!matchResult,
          searchScore: matchResult?.score,
          isSelected: selectedNodeId === node.id,
          isTourHighlighted: tourHighlightedNodeIds.includes(node.id),
          isDiffChanged: diffMode && changedNodeIds.has(node.id),
          isDiffAffected: diffMode && affectedNodeIds.has(node.id),
          isDiffFaded: diffMode && !changedNodeIds.has(node.id) && !affectedNodeIds.has(node.id),
          onNodeClick: handleNodeSelect,
        },
      };
    });

    const diffNodeIds = diffMode ? new Set([...changedNodeIds, ...affectedNodeIds]) : new Set<string>();
    const flowEdges: Edge[] = filteredGraphEdges.map((edge, i) => {
      const sourceInDiff = diffMode && diffNodeIds.has(edge.source);
      const targetInDiff = diffMode && diffNodeIds.has(edge.target);
      const isImpacted = diffMode && (sourceInDiff || targetInDiff);

      return {
        id: `e-${i}`,
        source: edge.source,
        target: edge.target,
        label: edge.type,
        animated: edge.type === "calls" || isImpacted,
        style: isImpacted
          ? {
              stroke: sourceInDiff && targetInDiff
                ? "rgba(224, 82, 82, 0.7)"
                : "rgba(212, 160, 48, 0.5)",
              strokeWidth: 2.5,
            }
          : diffMode
            ? { stroke: "rgba(212,165,116,0.08)", strokeWidth: 1 }
            : { stroke: "rgba(212,165,116,0.3)", strokeWidth: 1.5 },
        labelStyle: diffMode && !isImpacted
          ? { fill: "rgba(163,151,135,0.3)", fontSize: 10 }
          : { fill: "#a39787", fontSize: 10 },
      };
    });

    // Run dagre layout on all nodes (without groups)
    const laid = applyDagreLayout(flowNodes, flowEdges);
    const laidNodes = laid.nodes as CustomFlowNode[];

    const layers = graph.layers ?? [];
    if (!showLayers || layers.length === 0) {
      return { initialNodes: laidNodes, initialEdges: laid.edges };
    }

    // Build a map of nodeId -> layer for quick lookup
    const nodeToLayer = new Map<string, string>();
    for (const layer of layers) {
      for (const nodeId of layer.nodeIds) {
        nodeToLayer.set(nodeId, layer.id);
      }
    }

    // Create group nodes and adjust member positions
    const groupNodes: Node[] = [];
    const adjustedNodes: (CustomFlowNode | Node)[] = [];

    for (let layerIdx = 0; layerIdx < layers.length; layerIdx++) {
      const layer = layers[layerIdx];
      const memberNodes = laidNodes.filter((n) =>
        layer.nodeIds.includes(n.id),
      );

      if (memberNodes.length === 0) continue;

      // Compute bounding box around member nodes
      let minX = Infinity;
      let minY = Infinity;
      let maxX = -Infinity;
      let maxY = -Infinity;

      for (const node of memberNodes) {
        const x = node.position.x;
        const y = node.position.y;
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x + NODE_WIDTH);
        maxY = Math.max(maxY, y + NODE_HEIGHT);
      }

      // Group node position = top-left with padding
      const groupX = minX - LAYER_PADDING;
      const groupY = minY - LAYER_PADDING - 24; // extra space for label
      const groupWidth = maxX - minX + LAYER_PADDING * 2;
      const groupHeight = maxY - minY + LAYER_PADDING * 2 + 24;

      // Create the group node
      groupNodes.push({
        id: layer.id,
        type: "group",
        position: { x: groupX, y: groupY },
        data: { label: layer.name },
        style: {
          width: groupWidth,
          height: groupHeight,
          backgroundColor: "rgba(212,165,116,0.05)",
          borderRadius: 12,
          border: `2px dashed rgba(212,165,116,0.25)`,
          padding: 8,
          fontSize: 13,
          fontWeight: 600,
          color: "#d4a574",
        },
      });

      // Adjust member node positions to be relative to the group
      for (const node of memberNodes) {
        adjustedNodes.push({
          ...node,
          parentId: layer.id,
          extent: "parent" as const,
          position: {
            x: node.position.x - groupX,
            y: node.position.y - groupY,
          },
        });
      }
    }

    // Add nodes that are not in any layer (keep original positions)
    for (const node of laidNodes) {
      if (!nodeToLayer.has(node.id)) {
        adjustedNodes.push(node);
      }
    }

    // Group nodes must come before their children in the array
    const allNodes: (CustomFlowNode | Node)[] = [
      ...groupNodes,
      ...adjustedNodes,
    ];

    return { initialNodes: allNodes, initialEdges: laid.edges };
  }, [graph, searchResults, selectedNodeId, showLayers, tourHighlightedNodeIds, persona, handleNodeSelect, diffMode, changedNodeIds, affectedNodeIds]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  useEffect(() => {
    setNodes(initialNodes);
  }, [initialNodes, setNodes]);

  useEffect(() => {
    setEdges(initialEdges);
  }, [initialEdges, setEdges]);

  const onNodeClick = useCallback(
    (_: React.MouseEvent, node: { id: string }) => {
      // Ignore clicks on group nodes
      const isGroupNode = graph?.layers?.some((l) => l.id === node.id);
      if (isGroupNode) return;
      selectNode(node.id);
      openCodeViewer(node.id);
    },
    [selectNode, openCodeViewer, graph],
  );

  const onPaneClick = useCallback(() => {
    selectNode(null);
  }, [selectNode]);

  if (!graph) {
    return (
      <div className="h-full w-full flex items-center justify-center bg-root rounded-lg">
        <p className="text-text-muted text-sm">No knowledge graph loaded</p>
      </div>
    );
  }

  return (
    <div className="h-full w-full">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={onNodeClick}
        onPaneClick={onPaneClick}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ minZoom: 0.01, padding: 0.1 }}
        minZoom={0.01}
        maxZoom={2}
        colorMode="dark"
      >
        <Background variant={BackgroundVariant.Dots} color="rgba(212,165,116,0.15)" gap={20} size={1} />
        <Controls />
        <MiniMap
          nodeColor="#1a1a1a"
          maskColor="rgba(10,10,10,0.7)"
          className="!bg-surface !border !border-border-subtle"
        />
        <TourFitView />
        <SelectedNodeFitView />
      </ReactFlow>
    </div>
  );
}

export default function GraphView() {
  return (
    <ReactFlowProvider>
      <GraphViewInner />
    </ReactFlowProvider>
  );
}
