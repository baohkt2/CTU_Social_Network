import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { join } from "node:path";
import type { KnowledgeGraph, AnalysisMeta } from "../types.js";
import { validateGraph } from "../schema.js";

const UA_DIR = ".understand-anything";
const GRAPH_FILE = "knowledge-graph.json";
const META_FILE = "meta.json";

function ensureDir(projectRoot: string): string {
  const dir = join(projectRoot, UA_DIR);
  if (!existsSync(dir)) {
    mkdirSync(dir, { recursive: true });
  }
  return dir;
}

export function saveGraph(projectRoot: string, graph: KnowledgeGraph): void {
  const dir = ensureDir(projectRoot);
  writeFileSync(join(dir, GRAPH_FILE), JSON.stringify(graph, null, 2), "utf-8");
}

export function loadGraph(
  projectRoot: string,
  options?: { validate?: boolean },
): KnowledgeGraph | null {
  const filePath = join(projectRoot, UA_DIR, GRAPH_FILE);
  if (!existsSync(filePath)) return null;

  const data = JSON.parse(readFileSync(filePath, "utf-8"));

  if (options?.validate !== false) {
    const result = validateGraph(data);
    if (!result.success) {
      throw new Error(
        `Invalid knowledge graph: ${result.errors!.join("; ")}`,
      );
    }
    return result.data as KnowledgeGraph;
  }

  return data as KnowledgeGraph;
}

export function saveMeta(projectRoot: string, meta: AnalysisMeta): void {
  const dir = ensureDir(projectRoot);
  writeFileSync(join(dir, META_FILE), JSON.stringify(meta, null, 2), "utf-8");
}

export function loadMeta(projectRoot: string): AnalysisMeta | null {
  const filePath = join(projectRoot, UA_DIR, META_FILE);
  if (!existsSync(filePath)) return null;
  return JSON.parse(readFileSync(filePath, "utf-8")) as AnalysisMeta;
}
