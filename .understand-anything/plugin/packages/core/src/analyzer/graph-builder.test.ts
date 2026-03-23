import { describe, it, expect } from "vitest";
import { GraphBuilder } from "./graph-builder.js";
import type { StructuralAnalysis } from "../types.js";

describe("GraphBuilder", () => {
  it("should create file nodes from file list", () => {
    const builder = new GraphBuilder("test-project", "abc123");

    builder.addFile("src/index.ts", {
      summary: "Entry point",
      tags: ["entry"],
      complexity: "simple",
    });
    builder.addFile("src/utils.ts", {
      summary: "Utility functions",
      tags: ["utility"],
      complexity: "moderate",
    });

    const graph = builder.build();

    expect(graph.nodes).toHaveLength(2);
    expect(graph.nodes[0]).toMatchObject({
      id: "file:src/index.ts",
      type: "file",
      name: "index.ts",
      filePath: "src/index.ts",
      summary: "Entry point",
      tags: ["entry"],
      complexity: "simple",
    });
    expect(graph.nodes[1]).toMatchObject({
      id: "file:src/utils.ts",
      type: "file",
      name: "utils.ts",
      filePath: "src/utils.ts",
      summary: "Utility functions",
    });
  });

  it("should create function and class nodes from structural analysis", () => {
    const builder = new GraphBuilder("test-project", "abc123");
    const analysis: StructuralAnalysis = {
      functions: [
        { name: "processData", lineRange: [10, 25], params: ["input"], returnType: "string" },
        { name: "validate", lineRange: [30, 40], params: ["data"] },
      ],
      classes: [
        { name: "DataStore", lineRange: [50, 100], methods: ["get", "set"], properties: ["data"] },
      ],
      imports: [],
      exports: [],
    };

    builder.addFileWithAnalysis("src/service.ts", analysis, {
      summary: "Service module",
      tags: ["service"],
      complexity: "complex",
      fileSummary: "Handles data processing",
      summaries: {
        processData: "Processes raw input data",
        validate: "Validates data format",
        DataStore: "Manages stored data",
      },
    });

    const graph = builder.build();

    // 1 file + 2 functions + 1 class = 4 nodes
    expect(graph.nodes).toHaveLength(4);

    const fileNode = graph.nodes.find((n) => n.id === "file:src/service.ts");
    expect(fileNode).toBeDefined();
    expect(fileNode!.type).toBe("file");
    expect(fileNode!.summary).toBe("Handles data processing");

    const funcNode = graph.nodes.find((n) => n.id === "func:src/service.ts:processData");
    expect(funcNode).toBeDefined();
    expect(funcNode!.type).toBe("function");
    expect(funcNode!.name).toBe("processData");
    expect(funcNode!.lineRange).toEqual([10, 25]);
    expect(funcNode!.summary).toBe("Processes raw input data");

    const validateNode = graph.nodes.find((n) => n.id === "func:src/service.ts:validate");
    expect(validateNode).toBeDefined();
    expect(validateNode!.summary).toBe("Validates data format");

    const classNode = graph.nodes.find((n) => n.id === "class:src/service.ts:DataStore");
    expect(classNode).toBeDefined();
    expect(classNode!.type).toBe("class");
    expect(classNode!.name).toBe("DataStore");
    expect(classNode!.summary).toBe("Manages stored data");
  });

  it("should create contains edges between files and their functions/classes", () => {
    const builder = new GraphBuilder("test-project", "abc123");
    const analysis: StructuralAnalysis = {
      functions: [
        { name: "helper", lineRange: [5, 15], params: [] },
      ],
      classes: [
        { name: "Widget", lineRange: [20, 50], methods: [], properties: [] },
      ],
      imports: [],
      exports: [],
    };

    builder.addFileWithAnalysis("src/widget.ts", analysis, {
      summary: "Widget module",
      tags: [],
      complexity: "moderate",
      fileSummary: "Widget component",
      summaries: { helper: "Helper function", Widget: "Widget class" },
    });

    const graph = builder.build();

    const containsEdges = graph.edges.filter((e) => e.type === "contains");
    expect(containsEdges).toHaveLength(2);

    expect(containsEdges[0]).toMatchObject({
      source: "file:src/widget.ts",
      target: "func:src/widget.ts:helper",
      type: "contains",
      direction: "forward",
      weight: 1,
    });
    expect(containsEdges[1]).toMatchObject({
      source: "file:src/widget.ts",
      target: "class:src/widget.ts:Widget",
      type: "contains",
      direction: "forward",
      weight: 1,
    });
  });

  it("should create import edges between files", () => {
    const builder = new GraphBuilder("test-project", "abc123");

    builder.addFile("src/index.ts", {
      summary: "Entry",
      tags: [],
      complexity: "simple",
    });
    builder.addFile("src/utils.ts", {
      summary: "Utils",
      tags: [],
      complexity: "simple",
    });

    builder.addImportEdge("src/index.ts", "src/utils.ts");

    const graph = builder.build();
    const importEdges = graph.edges.filter((e) => e.type === "imports");
    expect(importEdges).toHaveLength(1);
    expect(importEdges[0]).toMatchObject({
      source: "file:src/index.ts",
      target: "file:src/utils.ts",
      type: "imports",
      direction: "forward",
    });
  });

  it("should create call edges between functions", () => {
    const builder = new GraphBuilder("test-project", "abc123");

    builder.addCallEdge("src/index.ts", "main", "src/utils.ts", "helper");

    const graph = builder.build();
    const callEdges = graph.edges.filter((e) => e.type === "calls");
    expect(callEdges).toHaveLength(1);
    expect(callEdges[0]).toMatchObject({
      source: "func:src/index.ts:main",
      target: "func:src/utils.ts:helper",
      type: "calls",
      direction: "forward",
    });
  });

  it("should set project metadata correctly", () => {
    const builder = new GraphBuilder("my-awesome-project", "deadbeef");

    builder.addFile("src/app.ts", {
      summary: "App",
      tags: [],
      complexity: "simple",
    });
    builder.addFile("src/script.py", {
      summary: "Script",
      tags: [],
      complexity: "simple",
    });

    const graph = builder.build();

    expect(graph.version).toBe("1.0.0");
    expect(graph.project.name).toBe("my-awesome-project");
    expect(graph.project.gitCommitHash).toBe("deadbeef");
    expect(graph.project.languages).toEqual(["python", "typescript"]);
    expect(graph.project.analyzedAt).toBeTruthy();
    expect(graph.layers).toEqual([]);
    expect(graph.tour).toEqual([]);
  });

  it("should detect languages from file extensions", () => {
    const builder = new GraphBuilder("polyglot", "hash123");

    builder.addFile("main.go", { summary: "", tags: [], complexity: "simple" });
    builder.addFile("lib.rs", { summary: "", tags: [], complexity: "simple" });
    builder.addFile("app.js", { summary: "", tags: [], complexity: "simple" });

    const graph = builder.build();
    expect(graph.project.languages).toEqual(["go", "javascript", "rust"]);
  });
});
