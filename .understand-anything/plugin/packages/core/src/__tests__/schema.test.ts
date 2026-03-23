import { describe, it, expect } from "vitest";
import { validateGraph } from "../schema.js";
import type { KnowledgeGraph } from "../types.js";

const validGraph: KnowledgeGraph = {
  version: "1.0.0",
  project: {
    name: "test-project",
    languages: ["typescript"],
    frameworks: ["vitest"],
    description: "A test project",
    analyzedAt: "2026-03-14T00:00:00.000Z",
    gitCommitHash: "abc123",
  },
  nodes: [
    {
      id: "node-1",
      type: "file",
      name: "index.ts",
      filePath: "src/index.ts",
      lineRange: [1, 50],
      summary: "Entry point",
      tags: ["entry"],
      complexity: "simple",
    },
  ],
  edges: [
    {
      source: "node-1",
      target: "node-2",
      type: "imports",
      direction: "forward",
      weight: 0.8,
    },
  ],
  layers: [
    {
      id: "layer-1",
      name: "Core",
      description: "Core layer",
      nodeIds: ["node-1"],
    },
  ],
  tour: [
    {
      order: 1,
      title: "Start here",
      description: "Begin with the entry point",
      nodeIds: ["node-1"],
    },
  ],
};

describe("schema validation", () => {
  it("validates a correct knowledge graph", () => {
    const result = validateGraph(validGraph);
    expect(result.success).toBe(true);
    expect(result.data).toBeDefined();
    expect(result.data!.version).toBe("1.0.0");
    expect(result.errors).toBeUndefined();
  });

  it("rejects graph with missing required fields", () => {
    const incomplete = {
      version: "1.0.0",
      // missing project, nodes, edges, layers, tour
    };

    const result = validateGraph(incomplete);
    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    expect(result.errors!.length).toBeGreaterThan(0);
  });

  it("rejects node with invalid type", () => {
    const graph = structuredClone(validGraph);
    (graph.nodes[0] as any).type = "invalid_type";

    const result = validateGraph(graph);
    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    expect(result.errors!.some((e) => e.includes("type"))).toBe(true);
  });

  it("rejects edge with invalid EdgeType", () => {
    const graph = structuredClone(validGraph);
    (graph.edges[0] as any).type = "not_a_real_edge_type";

    const result = validateGraph(graph);
    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    expect(result.errors!.some((e) => e.includes("type"))).toBe(true);
  });

  it("rejects weight out of range (>1)", () => {
    const graph = structuredClone(validGraph);
    graph.edges[0].weight = 1.5;

    const result = validateGraph(graph);
    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
  });

  it("rejects weight out of range (<0)", () => {
    const graph = structuredClone(validGraph);
    graph.edges[0].weight = -0.1;

    const result = validateGraph(graph);
    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
  });
});
