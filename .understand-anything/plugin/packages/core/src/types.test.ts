import { describe, it, expect } from "vitest";
import type { KnowledgeGraph, GraphNode, GraphEdge } from "./types.js";

describe("KnowledgeGraph types", () => {
  it("should create a valid empty KnowledgeGraph", () => {
    const graph: KnowledgeGraph = {
      version: "1.0.0",
      project: {
        name: "test-project",
        languages: [],
        frameworks: [],
        description: "A test project",
        analyzedAt: new Date().toISOString(),
        gitCommitHash: "abc123",
      },
      nodes: [],
      edges: [],
      layers: [],
      tour: [],
    };

    expect(graph.version).toBe("1.0.0");
    expect(graph.project.name).toBe("test-project");
    expect(graph.nodes).toHaveLength(0);
    expect(graph.edges).toHaveLength(0);
    expect(graph.layers).toHaveLength(0);
    expect(graph.tour).toHaveLength(0);
  });

  it("should create valid GraphNodes with all fields", () => {
    const fileNode: GraphNode = {
      id: "node-1",
      type: "file",
      name: "index.ts",
      filePath: "src/index.ts",
      lineRange: [1, 50],
      summary: "Entry point of the application",
      tags: ["entry", "typescript"],
      complexity: "simple",
      languageNotes: "Uses ES module syntax",
    };

    expect(fileNode.id).toBe("node-1");
    expect(fileNode.type).toBe("file");
    expect(fileNode.filePath).toBe("src/index.ts");
    expect(fileNode.lineRange).toEqual([1, 50]);
    expect(fileNode.tags).toContain("entry");
    expect(fileNode.complexity).toBe("simple");

    const functionNode: GraphNode = {
      id: "node-2",
      type: "function",
      name: "processData",
      filePath: "src/utils.ts",
      lineRange: [10, 25],
      summary: "Processes raw data into structured format",
      tags: ["utility", "data"],
      complexity: "moderate",
    };

    expect(functionNode.type).toBe("function");
    expect(functionNode.complexity).toBe("moderate");
    expect(functionNode.languageNotes).toBeUndefined();

    const classNode: GraphNode = {
      id: "node-3",
      type: "class",
      name: "DataStore",
      summary: "Manages application state",
      tags: ["state", "core"],
      complexity: "complex",
    };

    expect(classNode.type).toBe("class");
    expect(classNode.filePath).toBeUndefined();
    expect(classNode.lineRange).toBeUndefined();
  });

  it("should create valid GraphEdges with weight bounds", () => {
    const edge: GraphEdge = {
      source: "node-1",
      target: "node-2",
      type: "imports",
      direction: "forward",
      description: "index.ts imports processData from utils.ts",
      weight: 0.8,
    };

    expect(edge.source).toBe("node-1");
    expect(edge.target).toBe("node-2");
    expect(edge.type).toBe("imports");
    expect(edge.direction).toBe("forward");
    expect(edge.weight).toBeGreaterThanOrEqual(0);
    expect(edge.weight).toBeLessThanOrEqual(1);

    const minWeightEdge: GraphEdge = {
      source: "node-1",
      target: "node-3",
      type: "related",
      direction: "bidirectional",
      weight: 0,
    };

    expect(minWeightEdge.weight).toBe(0);
    expect(minWeightEdge.description).toBeUndefined();

    const maxWeightEdge: GraphEdge = {
      source: "node-2",
      target: "node-3",
      type: "calls",
      direction: "forward",
      weight: 1,
    };

    expect(maxWeightEdge.weight).toBe(1);
  });
});
