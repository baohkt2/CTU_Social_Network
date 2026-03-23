import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { mkdtempSync, rmSync, existsSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { saveGraph, loadGraph, saveMeta, loadMeta } from "./index.js";
import type { KnowledgeGraph, AnalysisMeta } from "../types.js";

describe("persistence", () => {
  let tempDir: string;

  beforeEach(() => {
    tempDir = mkdtempSync(join(tmpdir(), "ua-test-"));
  });

  afterEach(() => {
    rmSync(tempDir, { recursive: true, force: true });
  });

  const sampleGraph: KnowledgeGraph = {
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

  const sampleMeta: AnalysisMeta = {
    lastAnalyzedAt: "2026-03-14T00:00:00.000Z",
    gitCommitHash: "abc123",
    version: "1.0.0",
    analyzedFiles: 42,
  };

  describe("saveGraph / loadGraph", () => {
    it("should write knowledge-graph.json to .understand-anything/", () => {
      saveGraph(tempDir, sampleGraph);

      const filePath = join(tempDir, ".understand-anything", "knowledge-graph.json");
      expect(existsSync(filePath)).toBe(true);
    });

    it("should read back the saved graph correctly", () => {
      saveGraph(tempDir, sampleGraph);
      const loaded = loadGraph(tempDir);

      expect(loaded).not.toBeNull();
      expect(loaded).toEqual(sampleGraph);
    });

    it("should return null when no graph exists", () => {
      const loaded = loadGraph(tempDir);
      expect(loaded).toBeNull();
    });
  });

  describe("saveMeta / loadMeta", () => {
    it("should write meta.json to .understand-anything/", () => {
      saveMeta(tempDir, sampleMeta);

      const filePath = join(tempDir, ".understand-anything", "meta.json");
      expect(existsSync(filePath)).toBe(true);
    });

    it("should read back the saved meta correctly", () => {
      saveMeta(tempDir, sampleMeta);
      const loaded = loadMeta(tempDir);

      expect(loaded).not.toBeNull();
      expect(loaded).toEqual(sampleMeta);
    });

    it("should return null when no meta exists", () => {
      const loaded = loadMeta(tempDir);
      expect(loaded).toBeNull();
    });
  });
});
