import type { AnalyzerPlugin, StructuralAnalysis, ImportResolution } from "../types.js";

const EXTENSION_TO_LANGUAGE: Record<string, string> = {
  ts: "typescript",
  tsx: "typescript",
  js: "javascript",
  jsx: "javascript",
  py: "python",
  go: "go",
  rs: "rust",
  rb: "ruby",
  java: "java",
  kt: "kotlin",
  cs: "csharp",
  cpp: "cpp",
  c: "c",
  swift: "swift",
  php: "php",
};

/**
 * Registry for analyzer plugins. Maps languages to plugins and provides
 * a unified interface for analyzing files across languages.
 */
export class PluginRegistry {
  private plugins: AnalyzerPlugin[] = [];
  private languageMap = new Map<string, AnalyzerPlugin>();

  register(plugin: AnalyzerPlugin): void {
    this.plugins.push(plugin);
    for (const lang of plugin.languages) {
      this.languageMap.set(lang, plugin);
    }
  }

  unregister(name: string): void {
    const plugin = this.plugins.find((p) => p.name === name);
    if (!plugin) return;
    this.plugins = this.plugins.filter((p) => p.name !== name);
    this.languageMap.clear();
    for (const p of this.plugins) {
      for (const lang of p.languages) {
        this.languageMap.set(lang, p);
      }
    }
  }

  getPluginForLanguage(language: string): AnalyzerPlugin | null {
    return this.languageMap.get(language) ?? null;
  }

  getPluginForFile(filePath: string): AnalyzerPlugin | null {
    const ext = filePath.split(".").pop()?.toLowerCase();
    if (!ext) return null;
    const language = EXTENSION_TO_LANGUAGE[ext];
    if (!language) return null;
    return this.getPluginForLanguage(language);
  }

  analyzeFile(filePath: string, content: string): StructuralAnalysis | null {
    const plugin = this.getPluginForFile(filePath);
    if (!plugin) return null;
    return plugin.analyzeFile(filePath, content);
  }

  resolveImports(filePath: string, content: string): ImportResolution[] | null {
    const plugin = this.getPluginForFile(filePath);
    if (!plugin) return null;
    return plugin.resolveImports(filePath, content);
  }

  getPlugins(): AnalyzerPlugin[] {
    return [...this.plugins];
  }

  getSupportedLanguages(): string[] {
    return [...this.languageMap.keys()];
  }
}
