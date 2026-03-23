import type {
  KnowledgeGraph,
  GraphNode,
  GraphEdge,
  StructuralAnalysis,
} from "../types.js";

interface FileMeta {
  summary: string;
  tags: string[];
  complexity: "simple" | "moderate" | "complex";
}

interface FileAnalysisMeta extends FileMeta {
  summaries: Record<string, string>; // function/class name -> summary
  fileSummary: string;
}

const EXTENSION_LANGUAGE: Record<string, string> = {
  ".ts": "typescript",
  ".tsx": "typescript",
  ".js": "javascript",
  ".jsx": "javascript",
  ".mjs": "javascript",
  ".cjs": "javascript",
  ".py": "python",
  ".rb": "ruby",
  ".go": "go",
  ".rs": "rust",
  ".java": "java",
  ".kt": "kotlin",
  ".swift": "swift",
  ".c": "c",
  ".cpp": "cpp",
  ".h": "c",
  ".hpp": "cpp",
  ".cs": "csharp",
  ".php": "php",
  ".lua": "lua",
  ".sh": "shell",
  ".bash": "shell",
  ".zsh": "shell",
  ".json": "json",
  ".yaml": "yaml",
  ".yml": "yaml",
  ".toml": "toml",
  ".xml": "xml",
  ".html": "html",
  ".css": "css",
  ".scss": "scss",
  ".less": "less",
  ".md": "markdown",
  ".sql": "sql",
};

function detectLanguage(filePath: string): string {
  const lastDot = filePath.lastIndexOf(".");
  if (lastDot === -1) return "unknown";
  const ext = filePath.slice(lastDot).toLowerCase();
  return EXTENSION_LANGUAGE[ext] ?? "unknown";
}

export class GraphBuilder {
  private nodes: GraphNode[] = [];
  private edges: GraphEdge[] = [];
  private languages = new Set<string>();
  private projectName: string;
  private gitHash: string;

  constructor(projectName: string, gitHash: string) {
    this.projectName = projectName;
    this.gitHash = gitHash;
  }

  addFile(filePath: string, meta: FileMeta): void {
    const lang = detectLanguage(filePath);
    if (lang !== "unknown") {
      this.languages.add(lang);
    }

    const name = filePath.split("/").pop() ?? filePath;

    this.nodes.push({
      id: `file:${filePath}`,
      type: "file",
      name,
      filePath,
      summary: meta.summary,
      tags: meta.tags,
      complexity: meta.complexity,
    });
  }

  addFileWithAnalysis(
    filePath: string,
    analysis: StructuralAnalysis,
    meta: FileAnalysisMeta,
  ): void {
    const lang = detectLanguage(filePath);
    if (lang !== "unknown") {
      this.languages.add(lang);
    }

    const fileName = filePath.split("/").pop() ?? filePath;
    const fileId = `file:${filePath}`;

    // Create the file node
    this.nodes.push({
      id: fileId,
      type: "file",
      name: fileName,
      filePath,
      summary: meta.fileSummary,
      tags: meta.tags,
      complexity: meta.complexity,
    });

    // Create function nodes with "contains" edges
    for (const fn of analysis.functions) {
      const funcId = `func:${filePath}:${fn.name}`;
      this.nodes.push({
        id: funcId,
        type: "function",
        name: fn.name,
        filePath,
        lineRange: fn.lineRange,
        summary: meta.summaries[fn.name] ?? "",
        tags: [],
        complexity: meta.complexity,
      });

      this.edges.push({
        source: fileId,
        target: funcId,
        type: "contains",
        direction: "forward",
        weight: 1,
      });
    }

    // Create class nodes with "contains" edges
    for (const cls of analysis.classes) {
      const classId = `class:${filePath}:${cls.name}`;
      this.nodes.push({
        id: classId,
        type: "class",
        name: cls.name,
        filePath,
        lineRange: cls.lineRange,
        summary: meta.summaries[cls.name] ?? "",
        tags: [],
        complexity: meta.complexity,
      });

      this.edges.push({
        source: fileId,
        target: classId,
        type: "contains",
        direction: "forward",
        weight: 1,
      });
    }
  }

  addImportEdge(fromFile: string, toFile: string): void {
    this.edges.push({
      source: `file:${fromFile}`,
      target: `file:${toFile}`,
      type: "imports",
      direction: "forward",
      weight: 0.7,
    });
  }

  addCallEdge(
    callerFile: string,
    callerFunc: string,
    calleeFile: string,
    calleeFunc: string,
  ): void {
    this.edges.push({
      source: `func:${callerFile}:${callerFunc}`,
      target: `func:${calleeFile}:${calleeFunc}`,
      type: "calls",
      direction: "forward",
      weight: 0.8,
    });
  }

  build(): KnowledgeGraph {
    return {
      version: "1.0.0",
      project: {
        name: this.projectName,
        languages: [...this.languages].sort(),
        frameworks: [],
        description: "",
        analyzedAt: new Date().toISOString(),
        gitCommitHash: this.gitHash,
      },
      nodes: this.nodes,
      edges: this.edges,
      layers: [],
      tour: [],
    };
  }
}
